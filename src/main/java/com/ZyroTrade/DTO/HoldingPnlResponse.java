package com.ZyroTrade.DTO;

import java.math.BigDecimal;

public record HoldingPnlResponse(
        String symbol,
        int quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal pnl,
        String quoteStatus,
        String costBasisStatus
) {}
