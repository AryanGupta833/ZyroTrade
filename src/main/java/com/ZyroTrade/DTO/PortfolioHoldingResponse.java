package com.ZyroTrade.DTO;

import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioHolding;

import java.math.BigDecimal;

public record PortfolioHoldingResponse(Long id, String symbol, int quantity,
                                       BigDecimal averageAcquisitionPrice, BigDecimal costBasis) {
    public static PortfolioHoldingResponse from(Portfolio holding) {
        return new PortfolioHoldingResponse(holding.getId(), holding.getStock().getSymbol(),
                holding.getQuantity(), holding.getAvgPrice(), holding.getCostBasis());
    }
    public static PortfolioHoldingResponse from(PortfolioHolding holding) {
        return new PortfolioHoldingResponse(holding.getId(), holding.getSymbol(), holding.getQuantity(),
                holding.getAvgBuyPrice(), holding.getCostBasis());
    }
}
