package com.ZyroTrade.Orders;

import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.WalletTransaction.WalletTransactionRepository;
import com.ZyroTrade.user.Role;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import com.ZyroTrade.ledger.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ZyroTrade.Portfolios.PortfolioPnlService;
import com.ZyroTrade.outbox.*;
import com.ZyroTrade.Notification.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceIntegrationTests {
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private PortfolioPnlService portfolioPnlService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LedgerService ledgerService;
    @Autowired private LedgerAccountRepository ledgerAccountRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private LedgerLineRepository ledgerLineRepository;
    @Autowired private LedgerReconciliationService reconciliationService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxInboxRepository outboxInboxRepository;
    @Autowired private OutboxService outboxService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;

    private User user;
    private Stock stock;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM outbox_inbox_receipts");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM ledger_lines");
        jdbcTemplate.update("DELETE FROM journal_entries");
        jdbcTemplate.update("DELETE FROM ledger_accounts WHERE wallet_id IS NOT NULL");
        orderRepository.deleteAll();
        portfolioRepository.deleteAll();
        walletTransactionRepository.deleteAll();
        walletRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setUsername("order-test-user");
        user.setPassword("not-used");
        user.setRole(Role.ROLE_USER);
        user = userRepository.saveAndFlush(user);

        stock = new Stock();
        stock.setSymbol("TST");
        stock.setName("Test Stock");
        stock.setPrice(new BigDecimal("10.12345"));
        stock.setTradable(true);
        stock = stockRepository.saveAndFlush(stock);

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.0000"));
        wallet = walletRepository.saveAndFlush(wallet);
        ledgerService.postOpeningWalletBalance(wallet, wallet.getBalance(), "WALLET_OPENING:" + wallet.getId());
        authenticate();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchingIdempotencyRetryReturnsOriginalWithoutSecondDebitOrTrade() {
        Order first = orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "retry-1");
        Order second = orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "retry-1");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .hasSize(1);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("79.7532");
        assertThat(first.getPrice()).isEqualByComparingTo("10.1234");
        assertThat(portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId())
                .orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        var reconciled = reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow());
        assertThat(reconciled.orderCount()).isEqualTo(1);
        assertThat(reconciled.orderPostingCount()).isEqualTo(1);
        assertThat(reconciled.missingOrderIds()).isEmpty();
        assertThat(reconciled.reconciled()).isTrue();
    }

    @Test
    void openingWalletPostingIsRepeatableOnlyForTheSameReviewedAmount() {
        JournalEntry opening = journalEntryRepository.findByBusinessReference("WALLET_OPENING:" + wallet.getId()).orElseThrow();
        JournalEntry repeated = ledgerService.postOpeningWalletBalance(wallet, new BigDecimal("100.00"),
                "WALLET_OPENING:" + wallet.getId());
        assertThat(repeated.getId()).isEqualTo(opening.getId());
        assertThat(journalEntryRepository.count()).isEqualTo(1);

        assertThatThrownBy(() -> ledgerService.postOpeningWalletBalance(wallet, new BigDecimal("99.0000"),
                "WALLET_OPENING:" + wallet.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different financial details");
        assertThat(journalEntryRepository.count()).isEqualTo(1);
    }

    @Test
    void fractionalBuysKeepWeightedAverageAndPnlInDecimalArithmetic() {
        orderService.placeOrder(stock.getId(), OrderType.BUY, 1);
        stock.setPrice(new BigDecimal("10.98765"));
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.saveAndFlush(stock);
        orderService.placeOrder(stock.getId(), OrderType.BUY, 2);

        Portfolio holding = portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId()).orElseThrow();
        assertThat(holding.getAvgPrice()).isEqualByComparingTo("10.6995");
        assertThat(portfolioPnlService.calculatePnlByUsername(user.getUsername()).holdings().get(0).pnl())
                .isEqualByComparingTo("0.8642");
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.0000");
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("67.9014");
    }

    @Test
    void sellRealizesPnlAndRoundTripsBigDecimalJsonAndPersistence() throws Exception {
        orderService.placeOrder(stock.getId(), OrderType.BUY, 2);
        stock.setPrice(new BigDecimal("12.00005"));
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.saveAndFlush(stock);
        Order sell = orderService.placeOrder(stock.getId(), OrderType.SELL, 1);

        assertThat(sell.getRealizedPnl()).isEqualByComparingTo("1.8766");
        assertThat(orderRepository.findById(sell.getId()).orElseThrow().getPrice()).isEqualByComparingTo("12.0000");
        assertThat(portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId()).orElseThrow().getAvgPrice())
                .isEqualByComparingTo("10.1234");
        String json = objectMapper.writeValueAsString(sell);
        assertThat(json).contains("\"price\":12.0000");
    }

    @Test
    void buySellJournalsBalanceAndWalletReconciles() {
        Order buy = orderService.placeOrder(stock.getId(), OrderType.BUY, 2);
        Order sell = orderService.placeOrder(stock.getId(), OrderType.SELL, 1);

        assertThat(journalEntryRepository.count()).isEqualTo(3); // opening plus both orders
        for (JournalEntry entry : journalEntryRepository.findAll()) {
            List<LedgerLine> lines = ledgerLineRepository.findByJournalEntryIdOrderByLineNumber(entry.getId());
            BigDecimal debits = lines.stream().filter(l -> l.getSide() == LedgerSide.DEBIT)
                    .map(LedgerLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = lines.stream().filter(l -> l.getSide() == LedgerSide.CREDIT)
                    .map(LedgerLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(debits).isEqualByComparingTo(credits);
        }
        assertThat(ledgerEntryReference(buy)).isEqualTo("ORDER:" + buy.getId());
        assertThat(ledgerEntryReference(sell)).isEqualTo("ORDER:" + sell.getId());
        var result = reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow());
        assertThat(result.reconciled()).isTrue();
        assertThat(result.ledgerBalance()).isEqualByComparingTo("89.8766");
    }

    @Test
    void journalRejectsUnbalancedAndDuplicateBusinessReferences() {
        LedgerAccount cash = ledgerAccountRepository.findByCode("CASH_ASSET_INR").orElseThrow();
        LedgerAccount payable = ledgerService.walletAccount(wallet);
        List<LedgerPosting> unbalanced = List.of(
                new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, new BigDecimal("2.0000")),
                new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, new BigDecimal("1.0000")));
        assertThatThrownBy(() -> ledgerService.post("TEST:UNBALANCED", "bad", "INR", unbalanced))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("balance");
        assertThat(journalEntryRepository.findByBusinessReference("TEST:UNBALANCED")).isEmpty();

        ledgerService.post("TEST:DUPLICATE", "valid", "INR", List.of(
                new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, new BigDecimal("1.0000")),
                new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, new BigDecimal("1.0000"))));
        assertThatThrownBy(() -> ledgerService.post("TEST:DUPLICATE", "duplicate", "INR", List.of(
                new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, new BigDecimal("1.0000")),
                new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, new BigDecimal("1.0000")))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exists");

        JournalEntry original = journalEntryRepository.findByBusinessReference("TEST:DUPLICATE").orElseThrow();
        assertThatThrownBy(() -> {
            original.setDescription("mutated");
            journalEntryRepository.saveAndFlush(original);
        }).isInstanceOf(RuntimeException.class);
        assertThat(journalEntryRepository.findByBusinessReference("TEST:DUPLICATE").orElseThrow().getDescription())
                .isEqualTo("valid");

        JournalEntry reversal = ledgerService.postCompensatingEntry(
                "TEST:DUPLICATE", "TEST:REVERSAL", "correcting test posting");
        assertThat(reversal.getReversalOf().getId()).isEqualTo(original.getId());
        assertThat(ledgerLineRepository.findByJournalEntryIdOrderByLineNumber(reversal.getId()))
                .extracting(LedgerLine::getSide).containsExactly(LedgerSide.CREDIT, LedgerSide.DEBIT);
        assertThatThrownBy(() -> ledgerService.postCompensatingEntry(
                "TEST:DUPLICATE", "TEST:REVERSAL-2", "second reversal"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already has");
    }

    @Test
    void journalPostingRollsBackWhenOrderTransactionFails() {
        Portfolio fullHolding = new Portfolio();
        fullHolding.setUser(user);
        fullHolding.setStock(stock);
        fullHolding.setQuantity(Integer.MAX_VALUE);
        fullHolding.setAvgPrice(new BigDecimal("10.1235"));
        fullHolding.setCostBasis(new BigDecimal("20247000000.0000"));
        portfolioRepository.saveAndFlush(fullHolding);

        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(ArithmeticException.class);
        assertThat(journalEntryRepository.count()).isEqualTo(1); // opening journal only
        assertThat(outboxEventRepository.count()).isZero();
        assertThat(ledgerLineRepository.count()).isEqualTo(2);
        assertThat(reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow()).reconciled())
                .isTrue();
    }

    @Test
    void tradeOutboxIsTransactionalAndConsumerDeliveryIsIdempotent() {
        Order order = orderService.placeOrder(stock.getId(), OrderType.BUY, 1);
        OutboxEvent event = outboxEventRepository.findTop100ByOrderByCreatedAtDesc().get(0);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxService.dispatchPending()).isEqualTo(1);
        assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(notificationRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername())).hasSize(1);

        eventPublisher.publishEvent(new TradeCompletedEvent(event.getEventUuid(), order.getId(), user.getUsername(),
                stock.getSymbol(), OrderType.BUY.name(), 1, order.getPrice()));
        assertThat(notificationRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername())).hasSize(1);
        assertThat(outboxInboxRepository.count()).isEqualTo(1);
    }

    @Test
    void outboxDeadLettersAfterBoundedFailuresAndCanBeReplayed() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("unsupported.event.v1");
        event.setAggregateId(999L);
        event.setPayload("{}");
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(7);
        event.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        event = outboxEventRepository.saveAndFlush(event);

        assertThat(outboxService.dispatchPending()).isZero();
        OutboxEvent failed = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(8);
        assertThat(failed.getLastError()).isEqualTo("IllegalStateException");

        outboxService.replay(event.getId());
        OutboxEvent replayed = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(replayed.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(replayed.getAttempts()).isZero();
    }

    @Test
    void reconciliationReportsDifferencesWithoutChangingBalances() {
        wallet.setBalance(new BigDecimal("99.0000"));
        walletRepository.saveAndFlush(wallet);

        var result = reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow());
        assertThat(result.reconciled()).isFalse();
        assertThat(result.ledgerBalance()).isEqualByComparingTo("100.0000");
        assertThat(result.discrepancy()).isEqualByComparingTo("-1.0000");
        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("99.0000");
    }

    private void resetOpeningBalance(BigDecimal balance) {
        wallet.setBalance(balance);
        wallet = walletRepository.saveAndFlush(wallet);
        jdbcTemplate.update("DELETE FROM ledger_lines");
        jdbcTemplate.update("DELETE FROM journal_entries");
        ledgerService.postOpeningWalletBalance(wallet, balance, "WALLET_OPENING:" + wallet.getId());
    }

    private String ledgerEntryReference(Order order) {
        return journalEntryRepository.findByBusinessReference("ORDER:" + order.getId()).orElseThrow()
                .getBusinessReference();
    }

    @Test
    void conflictingPayloadForExistingKeyIsRejectedWithoutFinancialEffects() {
        orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "conflict-1");

        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 3, "conflict-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different request");
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("79.7532");
        assertThat(journalEntryRepository.count()).isEqualTo(2);
    }

    @Test
    void databaseUniqueConstraintRejectsDuplicateKeys() {
        orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "unique-index-key");
        Order duplicate = new Order();
        duplicate.setUser(user);
        duplicate.setStock(stock);
        duplicate.setType(OrderType.BUY);
        duplicate.setQuantity(1);
        duplicate.setPrice(new BigDecimal("10.0000"));
        duplicate.setRealizedPnl(BigDecimal.ZERO.setScale(4));
        duplicate.setIdempotencyKey("unique-index-key");

        assertThatThrownBy(() -> orderRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void simultaneousMatchingKeySubmissionsCreateOneTrade() throws Exception {
        int callers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                futures.add(pool.submit(() -> {
                    authenticate();
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException("start barrier timeout");
                    return orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "concurrent-key").getId();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Long> returnedIds = new ArrayList<>();
            for (Future<Long> future : futures) returnedIds.add(future.get(30, TimeUnit.SECONDS));
            assertThat(returnedIds).containsOnly(returnedIds.get(0));
        } finally {
            pool.shutdownNow();
        }
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .hasSize(1);
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("79.7532");
    }

    @Test
    void concurrentDifferentBuysCannotSpendTheSameFunds() throws Exception {
        resetOpeningBalance(new BigDecimal("15.0000"));
        List<Boolean> results = runTwoConcurrent(
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "funds-a"),
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "funds-b"));

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("4.8766");
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .hasSize(1);
        assertThat(journalEntryRepository.count()).isEqualTo(2);
    }

    @Test
    void concurrentSellsCannotSellTheSameHoldingTwice() throws Exception {
        Portfolio holding = new Portfolio();
        holding.setUser(user);
        holding.setStock(stock);
        holding.setQuantity(1);
        holding.setAvgPrice(new BigDecimal("5.0000"));
        holding.setCostBasis(new BigDecimal("5.0000"));
        portfolioRepository.saveAndFlush(holding);

        List<Boolean> results = runTwoConcurrent(
                () -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1, "sell-a"),
                () -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1, "sell-b"));

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId())).isEmpty();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("110.1234");
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .hasSize(1);
    }

    @Test
    void insufficientFundsLeavesWalletAndTradeUnchanged() {
        resetOpeningBalance(new BigDecimal("5.0000"));

        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient balance");
        assertThat(orderRepository.count()).isZero();
        assertThat(portfolioRepository.findByUserUsername(user.getUsername())).isEmpty();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("5.0000");
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .isEmpty();
        assertThat(journalEntryRepository.count()).isEqualTo(1);
    }

    @Test
    void insufficientHoldingsDoesNotCreditWalletOrCreateTrade() {
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1))
                .isInstanceOf(InsufficientHoldingsException.class)
                .hasMessageContaining("Insufficient holdings");
        assertThat(orderRepository.count()).isZero();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .isEmpty();
        assertThat(journalEntryRepository.count()).isEqualTo(1);
    }

    @Test
    void legacyHoldingWithoutReviewedCostBasisIsNotEstimatedOrTraded() {
        Portfolio holding = new Portfolio();
        holding.setUser(user);
        holding.setStock(stock);
        holding.setQuantity(2);
        holding.setAvgPrice(new BigDecimal("5.0000"));
        holding.setCostBasis(null);
        portfolioRepository.saveAndFlush(holding);

        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1))
                .isInstanceOf(CostBasisUnavailableException.class);
        assertThat(portfolioPnlService.calculatePnlByUsername(user.getUsername()).holdings().get(0).pnl()).isNull();
        assertThat(portfolioPnlService.calculatePnlByUsername(user.getUsername()).holdings().get(0).costBasisStatus())
                .isEqualTo("REVIEW_REQUIRED");
        assertThat(orderRepository.count()).isZero();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
    }

    @Test
    void exceptionAfterDebitRollsBackWalletTransactionAndHolding() {
        Portfolio fullHolding = new Portfolio();
        fullHolding.setUser(user);
        fullHolding.setStock(stock);
        fullHolding.setQuantity(Integer.MAX_VALUE);
        fullHolding.setAvgPrice(new BigDecimal("10.1235"));
        portfolioRepository.saveAndFlush(fullHolding);

        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(ArithmeticException.class);
        assertThat(orderRepository.count()).isZero();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .isEmpty();
        assertThat(portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId())
                .orElseThrow().getQuantity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void absentIdempotencyKeyPreservesIndependentOrderBehavior() {
        orderService.placeOrder(stock.getId(), OrderType.BUY, 1);
        orderService.placeOrder(stock.getId(), OrderType.BUY, 1);
        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("79.7532");
    }

    @Test
    void invalidOrFutureQuoteCannotExecute() {
        stock.setPrice(BigDecimal.ZERO);
        stockRepository.saveAndFlush(stock);
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid");

        stock.setPrice(new BigDecimal("10.0000"));
        stock.setLastUpdated(LocalDateTime.now().plusSeconds(1));
        stockRepository.saveAndFlush(stock);
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timestamp");
        stock.setLastUpdated(LocalDateTime.now().minusSeconds(121));
        stockRepository.saveAndFlush(stock);
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale");

        stock.setLastUpdated(null);
        stockRepository.saveAndFlush(stock);
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timestamp");
        assertThat(orderRepository.count()).isZero();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "test"));
    }

    private List<Boolean> runTwoConcurrent(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> callFirst = wrapResult(first, ready, start);
        Callable<Boolean> callSecond = wrapResult(second, ready, start);
        try {
            Future<Boolean> a = pool.submit(callFirst);
            Future<Boolean> b = pool.submit(callSecond);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Boolean> wrapResult(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            authenticate();
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException("start barrier timeout");
            try {
                action.call();
                return true;
            } catch (RuntimeException expectedRejection) {
                return false;
            }
        };
    }
}
