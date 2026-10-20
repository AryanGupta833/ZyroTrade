package com.ZyroTrade.Orders;

import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.WalletTransaction.WalletTransactionRepository;
import com.ZyroTrade.ledger.*;
import com.ZyroTrade.outbox.*;
import com.ZyroTrade.Notification.NotificationRepository;
import com.ZyroTrade.user.Role;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("mysql")
@Testcontainers(disabledWithoutDocker = true)
class MySqlOrderServiceIntegrationTests {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("zyrotrade")
            .withUsername("zyrotrade_test")
            .withPassword("zyrotrade_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private LedgerService ledgerService;
    @Autowired private LedgerAccountRepository ledgerAccountRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private LedgerLineRepository ledgerLineRepository;
    @Autowired private LedgerReconciliationService reconciliationService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
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
        user.setUsername("mysql-test-user");
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
    void mysqlUsesRepeatableReadAndPessimisticWalletLockSerializesCompetingBuys() throws Exception {
        String isolation = new TransactionTemplate(transactionManager)
                .execute(status -> jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class));
        assertThat(isolation)
                .isEqualToIgnoringCase("REPEATABLE-READ");
        wallet.setBalance(new BigDecimal("15.0000"));
        walletRepository.saveAndFlush(wallet);
        // Align the test opening ledger with the edited fixture balance.
        jdbcTemplate.update("DELETE FROM ledger_lines");
        jdbcTemplate.update("DELETE FROM journal_entries");
        ledgerService.postOpeningWalletBalance(wallet, wallet.getBalance(), "WALLET_OPENING:" + wallet.getId());

        List<Boolean> results = runTwoConcurrent(
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-funds-a"),
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-funds-b"));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("4.8766");
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        assertThat(reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow()).reconciled())
                .isTrue();
    }

    @Test
    void mysqlOutboxIsAtomicUniqueAndConsumerIsIdempotent() {
        Order order = orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-outbox-key");
        OutboxEvent event = outboxEventRepository.findTop100ByOrderByCreatedAtDesc().get(0);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThatThrownBy(() -> outboxService.enqueueTradeCompleted(order))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(outboxService.dispatchPending()).isEqualTo(1);
        assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);

        eventPublisher.publishEvent(new TradeCompletedEvent(event.getEventUuid(), order.getId(), user.getUsername(),
                stock.getSymbol(), OrderType.BUY.name(), 1, order.getPrice()));
        assertThat(notificationRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername())).hasSize(1);
        assertThat(outboxInboxRepository.count()).isEqualTo(1);
    }

    @Test
    void mysqlWalletLockPreventsTwoConcurrentSellsFromOverselling() throws Exception {
        Portfolio holding = new Portfolio();
        holding.setUser(user);
        holding.setStock(stock);
        holding.setQuantity(1);
        holding.setAvgPrice(new BigDecimal("5.0000"));
        holding.setCostBasis(new BigDecimal("5.0000"));
        portfolioRepository.saveAndFlush(holding);

        List<Boolean> results = runTwoConcurrent(
                () -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1, "mysql-sell-a"),
                () -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1, "mysql-sell-b"));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(portfolioRepository.findByUserUsernameAndStockId(user.getUsername(), stock.getId())).isEmpty();
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("110.1234");
        assertThat(reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow()).reconciled())
                .isTrue();
    }

    @Test
    void mysqlSameKeyRetriesPostOnceAndConflictsAreRejected() throws Exception {
        List<Long> ids = runConcurrentSameKey(6);
        assertThat(ids).containsOnly(ids.get(0));
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(journalEntryRepository.count()).isEqualTo(2); // opening plus order
        assertThat(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(user.getUsername()))
                .hasSize(1);
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "mysql-same-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different request");
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(journalEntryRepository.count()).isEqualTo(2);
    }

    @Test
    void mysqlConcurrentSameKeyWithDifferentPayloadHasOneWinner() throws Exception {
        List<Boolean> results = runTwoConcurrent(
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-conflicting-key"),
                () -> orderService.placeOrder(stock.getId(), OrderType.BUY, 2, "mysql-conflicting-key"));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        assertThat(reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow()).reconciled())
                .isTrue();
    }

    @Test
    void mysqlBusinessReferenceUniqueIndexAllowsOnlyOneConcurrentJournal() throws Exception {
        LedgerAccount cash = ledgerAccountRepository.findByCode("CASH_ASSET_INR").orElseThrow();
        LedgerAccount payable = ledgerAccountRepository.findByWalletIdAndCurrency(wallet.getId(), "INR").orElseThrow();
        List<LedgerPosting> lines = List.of(
                new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, new BigDecimal("1.0000")),
                new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, new BigDecimal("1.0000")));
        List<Boolean> results = runTwoConcurrent(
                () -> ledgerService.post("MYSQL:DUPLICATE-REFERENCE", "one", "INR", lines),
                () -> ledgerService.post("MYSQL:DUPLICATE-REFERENCE", "two", "INR", lines));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(journalEntryRepository.findByBusinessReference("MYSQL:DUPLICATE-REFERENCE")).isPresent();
    }

    @Test
    void mysqlUniqueConstraintsAndRollbackPreventOrphanPostings() {
        Order original = orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-unique-key");
        Order duplicate = new Order();
        duplicate.setUser(user);
        duplicate.setStock(stock);
        duplicate.setType(OrderType.BUY);
        duplicate.setQuantity(1);
        duplicate.setPrice(new BigDecimal("10.0000"));
        duplicate.setRealizedPnl(BigDecimal.ZERO.setScale(4));
        duplicate.setIdempotencyKey("mysql-unique-key");
        assertThatThrownBy(() -> orderRepository.saveAndFlush(duplicate)).isInstanceOf(RuntimeException.class);

        portfolioRepository.deleteAll();
        Portfolio fullHolding = new Portfolio();
        fullHolding.setUser(user);
        fullHolding.setStock(stock);
        fullHolding.setQuantity(Integer.MAX_VALUE);
        fullHolding.setAvgPrice(new BigDecimal("10.0000"));
        fullHolding.setCostBasis(new BigDecimal("21474836470.0000"));
        portfolioRepository.saveAndFlush(fullHolding);
        long entriesBefore = journalEntryRepository.count();
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .isInstanceOf(ArithmeticException.class);
        assertThat(journalEntryRepository.count()).isEqualTo(entriesBefore);
        assertThat(journalEntryRepository.findByBusinessReference("ORDER:" + original.getId())).isPresent();
        assertThat(reconciliationService.reconcile(walletRepository.findById(wallet.getId()).orElseThrow()).reconciled())
                .isTrue();
    }

    @Test
    void insufficientFundsAndHoldingsLeaveLedgerAndWalletUnchanged() {
        wallet.setBalance(new BigDecimal("5.0000"));
        walletRepository.saveAndFlush(wallet);
        jdbcTemplate.update("DELETE FROM ledger_lines");
        jdbcTemplate.update("DELETE FROM journal_entries");
        ledgerService.postOpeningWalletBalance(wallet, wallet.getBalance(), "WALLET_OPENING:" + wallet.getId());
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.BUY, 1))
                .hasMessageContaining("Insufficient balance");
        assertThatThrownBy(() -> orderService.placeOrder(stock.getId(), OrderType.SELL, 1))
                .hasMessageContaining("Insufficient holdings");
        assertThat(journalEntryRepository.count()).isEqualTo(1);
        assertThat(walletRepository.findByUserUsername(user.getUsername()).orElseThrow().getBalance())
                .isEqualByComparingTo("5.0000");
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "test"));
    }

    private List<Long> runConcurrentSameKey(int callers) throws Exception {
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
                    return orderService.placeOrder(stock.getId(), OrderType.BUY, 1, "mysql-same-key").getId();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Long> ids = new ArrayList<>();
            for (Future<Long> future : futures) ids.add(future.get(45, TimeUnit.SECONDS));
            return ids;
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Boolean> runTwoConcurrent(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(wrap(first, ready, start));
            Future<Boolean> b = pool.submit(wrap(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(45, TimeUnit.SECONDS), b.get(45, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Boolean> wrap(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
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
