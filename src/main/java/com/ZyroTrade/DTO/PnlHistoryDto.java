package com.ZyroTrade.DTO;

import java.time.LocalDate;

public record PnlHistoryDto(
        LocalDate date,
        double realizedPnl,
        double unrealizedPnl,
        double netPnl,
        double portfolioValue,
        double walletBalance
) {
}
