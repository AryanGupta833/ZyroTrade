package com.ZyroTrade.dailyPnlSnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.ZyroTrade.DTO.PnlHistoryDto;

@RestController
@RequestMapping("/api/performance")
public class DailyPnlSnapshotController {

    @Autowired
    private DailyPnlSnapshotRepository snapshotRepository;

    @GetMapping("/history")
    public List<PnlHistoryDto> myHistory() {

        String username =
                SecurityContextHolder.getContext()
                        .getAuthentication().getName();

        return snapshotRepository
                .findByUsernameOrderBySnapshotDateDesc(username).stream()
                .map(s -> new PnlHistoryDto(s.getSnapshotDate(), s.getRealizedPnl(), s.getUnrealizedPnl(),
                        s.getNetPnl(), s.getPortfolioValue(), s.getWalletBalance(), s.isAllQuotesFresh(),
                        s.isAllCostBasisReviewed())).toList();
    }
}
