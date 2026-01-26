package com.ZyroTrade.dailyPnlSnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class DailyPnlSnapshotController {

    @Autowired
    private DailyPnlSnapshotRepository snapshotRepository;

    @GetMapping("/history")
    public List<DailyPnlSnapshot> myHistory() {

        String username =
                SecurityContextHolder.getContext()
                        .getAuthentication().getName();

        return snapshotRepository
                .findByUsernameOrderBySnapshotDateDesc(username);
    }
}
