package com.ZyroTrade.dailyPnlSnapshot;

import com.ZyroTrade.DTO.PnlHistoryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pnl")
public class PnlHistoryController {
    @Autowired
    private DailyPnlSnapshotService dailyPnlSnapshotService;

    @GetMapping("/history")
    public List<PnlHistoryDto> getLastNDays(@RequestParam(defaultValue = "30")int days){
        return dailyPnlSnapshotService.getLastNDaysHistory(days);
    }

    @GetMapping("/history/range")
    public List<PnlHistoryDto> getByDataRange(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
return dailyPnlSnapshotService.getHistoryBetween(from, to);
    }
}
