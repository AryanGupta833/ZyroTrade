package com.ZyroTrade.DTO;

import com.ZyroTrade.Orders.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record OrderResponse(Long id, String symbol, String type, int quantity, BigDecimal executionPrice,
                            BigDecimal realizedPnl, String status, OffsetDateTime createdAt) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStock().getSymbol(), order.getType().name(),
                order.getQuantity(), order.getPrice(), order.getRealizedPnl(), "EXECUTED",
                order.getCreatedAt() == null ? null : order.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
