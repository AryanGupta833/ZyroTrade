package com.ZyroTrade.DTO;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Price.QuoteFreshnessStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record StockResponse(Long id, String symbol, String name, BigDecimal price, boolean tradable,
                            String quoteStatus, OffsetDateTime lastUpdated, long maxAgeSeconds) {
    public static StockResponse from(Stock stock, long maxAgeSeconds) {
        LocalDateTime updated = stock.getLastUpdated();
        String status = QuoteFreshnessStatus.evaluate(stock.getPrice(), updated, maxAgeSeconds).name();
        OffsetDateTime timestamp = updated == null ? null : updated.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return new StockResponse(stock.getId(), stock.getSymbol(), stock.getName(), stock.getPrice(),
                stock.isTradable(), status, timestamp, maxAgeSeconds);
    }
}
