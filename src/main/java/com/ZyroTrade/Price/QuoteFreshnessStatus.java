package com.ZyroTrade.Price;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public enum QuoteFreshnessStatus {
    FRESH, STALE, UNAVAILABLE;

    public static QuoteFreshnessStatus evaluate(BigDecimal price, LocalDateTime updatedAt, long maxAgeSeconds) {
        if (price == null || price.signum() <= 0 || updatedAt == null || maxAgeSeconds < 0) return UNAVAILABLE;
        LocalDateTime now = LocalDateTime.now();
        if (updatedAt.isAfter(now)) return UNAVAILABLE;
        return updatedAt.isBefore(now.minusSeconds(maxAgeSeconds)) ? STALE : FRESH;
    }
}
