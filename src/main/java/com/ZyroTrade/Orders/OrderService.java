package com.ZyroTrade.Orders;

import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Wallet.WalletService;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import com.ZyroTrade.ledger.LedgerService;
import com.ZyroTrade.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;
    private final long maxPriceAgeSeconds;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meters;

    public OrderService(OrderRepository orderRepository,
                        StockRepository stockRepository,
                        UserRepository userRepository,
                        PortfolioRepository portfolioRepository,
                        WalletService walletService,
                        LedgerService ledgerService,
                        OutboxService outboxService,
                        PlatformTransactionManager transactionManager,
                        MeterRegistry meters,
                        @Value("\u0024{trading.max-price-age-seconds:120}") long maxPriceAgeSeconds) {
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.walletService = walletService;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
        this.maxPriceAgeSeconds = maxPriceAgeSeconds;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.meters = meters;
    }

    public Order placeOrder(Long stockId, OrderType type, int quantity) {
        return placeOrder(stockId, type, quantity, null);
    }

    public Order placeOrder(Long stockId, OrderType type, int quantity, String idempotencyKey) {
        if (stockId == null || type == null || quantity <= 0) {
            throw new IllegalArgumentException("Stock, order type, and positive quantity are required");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String key = normalizeKey(idempotencyKey);
        Timer.Sample sample = Timer.start(meters);
        String outcome = "rejected";
        String orderType = type.name().toLowerCase(java.util.Locale.ROOT);
        try {
            Order order = transactionTemplate.execute(status -> executeOrder(username, stockId, type, quantity, key));
            outcome = "accepted";
            meters.counter("orders.submission", "outcome", outcome, "type", orderType).increment();
            return order;
        } catch (RuntimeException failure) {
            if (key != null) {
                Optional<Order> committed = orderRepository.findByIdempotencyKey(key);
                if (committed.isPresent()) {
                    Order retry = returnIfMatching(committed.get(), username, stockId, type, quantity);
                    outcome = "idempotent_replay";
                    meters.counter("orders.submission", "outcome", outcome, "type", orderType).increment();
                    return retry;
                }
            }
            meters.counter("orders.submission", "outcome", outcome, "type", orderType).increment();
            throw failure;
        } finally {
            sample.stop(meters.timer("orders.submission.duration", "outcome", outcome, "type", orderType));
        }
    }

    private Order returnIfMatching(Order prior, String username, Long stockId, OrderType type, int quantity) {
        if (!prior.getUser().getUsername().equals(username)
                || !prior.getStock().getId().equals(stockId)
                || prior.getType() != type
                || prior.getQuantity() != quantity) {
            throw new IdempotencyConflictException();
        }
        return prior;
    }

    private Order executeOrder(String username, Long stockId, OrderType type, int quantity, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return returnIfMatching(existing.get(), username, stockId, type, quantity);
            }
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
        validateQuote(stock);

        // All user orders lock wallet first; this serializes cash and holding changes and
        // keeps lock ordering consistent. REQUIRED propagation retains the lock until commit.
        walletService.lockAccount(username);
        // A request may have waited behind another transaction long enough for its
        // previously acceptable quote to expire.
        validateQuote(stock);
        if (idempotencyKey != null) {
            Optional<Order> committedRetry = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (committedRetry.isPresent()) {
                return returnIfMatching(committedRetry.get(), username, stockId, type, quantity);
            }
        }

        Portfolio portfolio = portfolioRepository.findByUserUsernameAndStockId(username, stockId).orElse(null);
        BigDecimal unitPrice = stock.getPrice().setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal tradeValue = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_EVEN);

        if (type == OrderType.BUY) {
            walletService.debit(username, tradeValue);
            if (portfolio == null) {
                portfolio = new Portfolio();
                portfolio.setUser(user);
                portfolio.setStock(stock);
                portfolio.setQuantity(0);
                portfolio.setAvgPrice(BigDecimal.ZERO.setScale(4));
                portfolio.setCostBasis(BigDecimal.ZERO.setScale(4));
            }
            int oldQuantity = portfolio.getQuantity();
            int newQuantity = Math.addExact(oldQuantity, quantity);
            BigDecimal oldCost = portfolio.getCostBasis();
            if (oldQuantity > 0 && (oldCost == null || oldCost.signum() <= 0)) throw new CostBasisUnavailableException();
            if (oldQuantity == 0 && oldCost == null) oldCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal newCostBasis = oldCost.add(tradeValue).setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal newAverage = newCostBasis
                    .divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_EVEN);
            portfolio.setQuantity(newQuantity);
            portfolio.setAvgPrice(newAverage);
            portfolio.setCostBasis(newCostBasis);
            portfolioRepository.save(portfolio);
            return saveOrder(user, stock, type, quantity, unitPrice, BigDecimal.ZERO.setScale(4), idempotencyKey);
        }

        if (type == OrderType.SELL) {
            if (portfolio == null || portfolio.getQuantity() < quantity) {
                throw new InsufficientHoldingsException();
            }
            int oldQuantity = portfolio.getQuantity();
            BigDecimal costBasis = portfolio.getCostBasis();
            if (costBasis == null || costBasis.signum() <= 0) throw new CostBasisUnavailableException();
            BigDecimal soldCostBasis = quantity == oldQuantity ? costBasis : costBasis
                    .multiply(BigDecimal.valueOf(quantity))
                    .divide(BigDecimal.valueOf(oldQuantity), 4, RoundingMode.HALF_EVEN);
            int remaining = oldQuantity - quantity;
            if (remaining == 0) portfolioRepository.delete(portfolio);
            else {
                portfolio.setQuantity(remaining);
                BigDecimal remainingCostBasis = costBasis.subtract(soldCostBasis).setScale(4, RoundingMode.HALF_EVEN);
                portfolio.setCostBasis(remainingCostBasis);
                portfolio.setAvgPrice(remainingCostBasis.divide(BigDecimal.valueOf(remaining), 4, RoundingMode.HALF_EVEN));
                portfolioRepository.save(portfolio);
            }
            walletService.credit(username, tradeValue);
            BigDecimal realized = tradeValue.subtract(soldCostBasis).setScale(4, RoundingMode.HALF_EVEN);
            return saveOrder(user, stock, type, quantity, unitPrice, realized, idempotencyKey);
        }
        throw new IllegalArgumentException("Unsupported order type");
    }

    private void validateQuote(Stock stock) {
        if (!stock.isTradable()) throw new TradingHaltedException();
        LocalDateTime updated = stock.getLastUpdated();
        LocalDateTime now = LocalDateTime.now();
        if (stock.getPrice() == null || stock.getPrice().signum() <= 0) {
            throw new QuoteUnavailableException("Market price is invalid");
        }
        if (maxPriceAgeSeconds < 0 || updated == null || updated.isAfter(now)
                || updated.isBefore(now.minus(Duration.ofSeconds(maxPriceAgeSeconds)))) {
            throw new QuoteUnavailableException("Market price is stale or timestamp is invalid");
        }
    }

    private Order saveOrder(User user, Stock stock, OrderType type, int quantity,
                            BigDecimal price, BigDecimal realizedPnl, String key) {
        Order order = new Order();
        order.setUser(user);
        order.setStock(stock);
        order.setType(type);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setRealizedPnl(realizedPnl);
        order.setIdempotencyKey(key);
        orderRepository.saveAndFlush(order);
        ledgerService.postOrder(order, walletService.getMyWallet(user.getUsername()));
        outboxService.enqueueTradeCompleted(order);
        return order;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.trim();
        if (normalized.length() > 128) throw new IllegalArgumentException("Idempotency key is too long");
        return normalized;
    }

    public List<Order> myOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return orderRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }

    public Page<Order> myOrders(String username, Pageable pageable) {
        return orderRepository.findByUserUsernameOrderByCreatedAtDesc(username, pageable);
    }

    public Optional<Order> myOrder(Long id, String username) {
        return orderRepository.findByIdAndUserUsername(id, username);
    }
}
