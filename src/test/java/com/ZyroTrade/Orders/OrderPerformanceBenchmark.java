package com.ZyroTrade.Orders;

import com.ZyroTrade.Portfolios.PortfolioPnlService;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.ledger.LedgerService;
import com.ZyroTrade.user.Role;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@ActiveProfiles("mysql")
@Testcontainers(disabledWithoutDocker = true)
class OrderPerformanceBenchmark {
    private static final int CONCURRENCY = 8;
    private static final int WARMUP_PER_WORKER = 10;
    private static final int MEASUREMENT_SECONDS = 10;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("zyrotrade_benchmark")
            .withUsername("benchmark_user")
            .withPassword("benchmark_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private OrderService orders;
    @Autowired private PortfolioPnlService portfolios;
    @Autowired private UserRepository users;
    @Autowired private WalletRepository wallets;
    @Autowired private StockRepository stocks;
    @Autowired private LedgerService ledger;

    @Test
    @EnabledIfSystemProperty(named = "run.benchmarks", matches = "true")
    void benchmarkMySqlOrderSubmissionAndPortfolioQueries() throws Exception {
        Stock stock = new Stock();
        stock.setSymbol("B" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase());
        stock.setName("Benchmark stock");
        stock.setPrice(new BigDecimal("25.1234"));
        stock.setLastUpdated(LocalDateTime.now());
        stock.setTradable(true);
        stock = stocks.saveAndFlush(stock);

        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            User user = new User();
            String username = "bench-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            user.setUsername(username);
            user.setPassword("benchmark-placeholder");
            user.setRole(Role.ROLE_USER);
            user = users.saveAndFlush(user);
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(new BigDecimal("10000000.0000"));
            wallet = wallets.saveAndFlush(wallet);
            ledger.postOpeningWalletBalance(wallet, wallet.getBalance(), "WALLET_OPENING:" + wallet.getId());
            usernames.add(username);
        }

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch warmed = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Long> orderLatencies = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> portfolioLatencies = new ConcurrentLinkedQueue<>();
        AtomicLong failures = new AtomicLong();
        long benchmarkStart;
        long benchmarkEnd;
        try {
            List<Future<?>> workers = new ArrayList<>();
            for (int worker = 0; worker < CONCURRENCY; worker++) {
                String username = usernames.get(worker);
                Long stockId = stock.getId();
                workers.add(pool.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(username, "benchmark"));
                    try {
                        for (int i = 0; i < WARMUP_PER_WORKER; i++) runOne(username, stockId, failures, null, null);
                        warmed.countDown();
                        if (!start.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("benchmark start timed out");
                        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MEASUREMENT_SECONDS);
                        while (System.nanoTime() < deadline) {
                            runOne(username, stockId, failures, orderLatencies, portfolioLatencies);
                        }
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }
            if (!warmed.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("benchmark warm-up timed out");
            start.countDown();
            benchmarkStart = System.nanoTime();
            for (Future<?> worker : workers) worker.get(90, TimeUnit.SECONDS);
            benchmarkEnd = System.nanoTime();
        } finally {
            start.countDown();
            pool.shutdownNow();
        }

        double seconds = (benchmarkEnd - benchmarkStart) / 1_000_000_000.0;
        long orderCount = orderLatencies.size();
        long portfolioCount = portfolioLatencies.size();
        double errorRate = failures.get() / (double) Math.max(1, orderCount + portfolioCount + failures.get());
        System.out.printf("MYSQL_BENCHMARK mysql=%s concurrency=%d warmup_orders=%d duration_seconds=%.3f " +
                        "order_count=%d order_throughput_per_sec=%.2f order_p50_ms=%.3f order_p95_ms=%.3f order_p99_ms=%.3f " +
                        "portfolio_count=%d portfolio_throughput_per_sec=%.2f portfolio_p50_ms=%.3f portfolio_p95_ms=%.3f portfolio_p99_ms=%.3f " +
                        "failures=%d error_rate=%.6f%n",
                MYSQL.getDockerImageName(), CONCURRENCY, CONCURRENCY * WARMUP_PER_WORKER, seconds,
                orderCount, orderCount / seconds, percentile(orderLatencies, 0.50), percentile(orderLatencies, 0.95), percentile(orderLatencies, 0.99),
                portfolioCount, portfolioCount / seconds, percentile(portfolioLatencies, 0.50), percentile(portfolioLatencies, 0.95), percentile(portfolioLatencies, 0.99),
                failures.get(), errorRate);
    }

    private void runOne(String username, Long stockId, AtomicLong failures,
                        ConcurrentLinkedQueue<Long> orderLatencies,
                        ConcurrentLinkedQueue<Long> portfolioLatencies) {
        long started = System.nanoTime();
        try {
            orders.placeOrder(stockId, OrderType.BUY, 1);
            if (orderLatencies != null) orderLatencies.add(System.nanoTime() - started);
        } catch (RuntimeException failure) {
            failures.incrementAndGet();
            return;
        }
        if (portfolioLatencies == null) return;
        long queryStarted = System.nanoTime();
        try {
            portfolios.calculatePnlByUsername(username);
            portfolioLatencies.add(System.nanoTime() - queryStarted);
        } catch (RuntimeException failure) {
            failures.incrementAndGet();
        }
    }

    private double percentile(ConcurrentLinkedQueue<Long> samples, double percentile) {
        if (samples.isEmpty()) return Double.NaN;
        List<Long> sorted = samples.stream().sorted(Comparator.naturalOrder()).toList();
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index) / 1_000_000.0;
    }
}
