package com.ZyroTrade.dailyPnlSnapshot;

import com.ZyroTrade.Orders.OrderRepository;
import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.ZyroTrade.DTO.PnlHistoryDto;
import org.springframework.beans.factory.annotation.Value;
import com.ZyroTrade.Price.QuoteFreshnessStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyPnlSnapshotService {
    private PnlHistoryDto toDto(DailyPnlSnapshot s) {
        return new PnlHistoryDto(
                s.getSnapshotDate(),
                s.getRealizedPnl(),
                s.getUnrealizedPnl(),
                s.getNetPnl(),
                s.getPortfolioValue(),
                s.getWalletBalance(),
                s.isAllQuotesFresh(),
                s.isAllCostBasisReviewed()
        );
    }


    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private DailyPnlSnapshotRepository dailyPnlSnapshotRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Value("${trading.max-price-age-seconds:120}")
    private long maxPriceAgeSeconds;

    public void takeSnapShot(String username) {

        LocalDate today = LocalDate.now();


        List<Portfolio> portfolios =
                portfolioRepository.findByUserUsername(username);

        boolean allCostBasisReviewed = portfolios.stream().allMatch(p -> p.getCostBasis() != null
                && p.getCostBasis().signum() > 0);
        BigDecimal unrealizedPnl = allCostBasisReviewed ? portfolios.stream()
                .map(p -> p.getStock().getPrice().multiply(BigDecimal.valueOf(p.getQuantity())).subtract(p.getCostBasis()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_EVEN) : null;

        BigDecimal portfolioValue = portfolios.stream()
                .map(p -> p.getStock().getPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_EVEN);
        boolean allQuotesFresh = portfolios.stream().allMatch(p -> QuoteFreshnessStatus.evaluate(
                p.getStock().getPrice(), p.getStock().getLastUpdated(), maxPriceAgeSeconds)
                == QuoteFreshnessStatus.FRESH);

        Wallet wallet = walletRepository
                .findByUserUsername(username)
                .orElseThrow(() -> new IllegalStateException("Wallet is missing for the P&L snapshot"));

        BigDecimal walletBalance = wallet.getBalance();


        BigDecimal realizedPnl =
                orderRepository.sumRealizedPnl(username);

        if (realizedPnl == null) realizedPnl = BigDecimal.ZERO.setScale(4);
        BigDecimal netPnl = unrealizedPnl == null ? null
                : realizedPnl.add(unrealizedPnl).setScale(4, RoundingMode.HALF_EVEN);


        DailyPnlSnapshot snapshot = dailyPnlSnapshotRepository
                .findByUsernameAndSnapshotDate(username, today)
                .orElseGet(() -> {
                    DailyPnlSnapshot s = new DailyPnlSnapshot();
                    s.setUsername(username);
                    s.setSnapshotDate(today);
                    return s;
                });

        snapshot.setRealizedPnl(realizedPnl);
        snapshot.setUnrealizedPnl(unrealizedPnl);
        snapshot.setNetPnl(netPnl);
        snapshot.setPortfolioValue(portfolioValue);
        snapshot.setWalletBalance(walletBalance);
        snapshot.setAllQuotesFresh(allQuotesFresh);
        snapshot.setAllCostBasisReviewed(allCostBasisReviewed);

        dailyPnlSnapshotRepository.save(snapshot);

    }
    public List<PnlHistoryDto> getLastNDaysHistory(int days) {
        if (days < 1 || days > 365) throw new IllegalArgumentException("days must be between 1 and 365");
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        LocalDate from = LocalDate.now().minusDays(days);

        return dailyPnlSnapshotRepository
                .findByUsernameAndSnapshotDateAfterOrderBySnapshotDateAsc(
                        username, from
                ).stream().map(this::toDto).collect(Collectors.toList());
    }
    public List<PnlHistoryDto> getHistoryBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusDays(365))) {
            throw new IllegalArgumentException("Date range must be ordered and no longer than 366 days");
        }
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return dailyPnlSnapshotRepository
                .findByUsernameAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        username, from, to
                ).stream().map(this::toDto).collect(Collectors.toList());
    }
}
