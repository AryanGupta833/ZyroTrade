package com.ZyroTrade.DTO;

import java.time.LocalDate;
import java.math.BigDecimal;

public record PnlHistoryDto(
        LocalDate date,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        BigDecimal netPnl,
        BigDecimal portfolioValue,
        BigDecimal walletBalance,
        boolean allQuotesFresh,
        boolean allCostBasisReviewed
) {
}
