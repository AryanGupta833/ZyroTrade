package com.ZyroTrade.DTO;

import com.ZyroTrade.PriceAlert.PriceAlert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PriceAlertResponse(Long id, String symbol, BigDecimal targetPrice, String type,
                                 boolean triggered, OffsetDateTime createdAt) {
    public static PriceAlertResponse from(PriceAlert alert) {
        return new PriceAlertResponse(alert.getId(), alert.getSymbol(), alert.getTargetPrice(),
                alert.getAlertType().name(), alert.isTriggered(), alert.getCreatedAt() == null ? null
                : alert.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
