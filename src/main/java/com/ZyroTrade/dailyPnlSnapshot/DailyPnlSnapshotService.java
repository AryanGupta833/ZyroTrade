package com.ZyroTrade.dailyPnlSnapshot;

import com.ZyroTrade.Orders.OrderRepository;
import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.ZyroTrade.DTO.PnlHistoryDto;
import java.util.stream.Collectors;
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
                s.getWalletBalance()
        );
    }


    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyPnlSnapshotRepository dailyPnlSnapshotRepository;

    @Autowired
    private OrderRepository orderRepository;

    public void takeSnapShot(String username) {

        LocalDate today = LocalDate.now();


        List<Portfolio> portfolios =
                portfolioRepository.findByUserUsername(username);

        double unrealizedPnl = portfolios.stream()
                .mapToDouble(p ->
                        (p.getStock().getPrice() - p.getAvgPrice())
                                * p.getQuantity()
                ).sum();

        double portfolioValue = portfolios.stream()
                .mapToDouble(p ->
                        p.getStock().getPrice() * p.getQuantity()
                ).sum();

        Wallet wallet = walletRepository
                .findByUserUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User not found: " + username));

                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(0.0);

                    walletRepository.save(w);

                    System.out.println(" Auto-created wallet for: " + username);

                    return w;
                });

        double walletBalance = wallet.getBalance();


        double realizedPnl =
                orderRepository.sumRealizedPnl(username);

        double netPnl = realizedPnl + unrealizedPnl;


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

        dailyPnlSnapshotRepository.save(snapshot);

        System.out.println("Daily PnL snapshot saved for: " + username);
    }
    public List<PnlHistoryDto> getLastNDaysHistory(int days) {
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
