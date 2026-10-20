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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/pnl")
@Validated
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class PnlHistoryController {
    @Autowired
    private DailyPnlSnapshotService dailyPnlSnapshotService;

    @GetMapping("/history")
    public List<PnlHistoryDto> getLastNDays(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days){
        return dailyPnlSnapshotService.getLastNDaysHistory(days);
    }

    @GetMapping("/history/range")
    public List<PnlHistoryDto> getByDataRange(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
return dailyPnlSnapshotService.getHistoryBetween(from, to);
    }
}
