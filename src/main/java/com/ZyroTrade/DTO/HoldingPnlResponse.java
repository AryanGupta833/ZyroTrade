package com.ZyroTrade.DTO;

public record HoldingPnlResponse(
        String symbol,
        int quantity,
        double avgBuyPrice,
        double currentPrice,
        double pnl
) {}
