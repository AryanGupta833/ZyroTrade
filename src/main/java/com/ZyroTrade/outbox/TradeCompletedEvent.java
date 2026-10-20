package com.ZyroTrade.outbox;

import java.math.BigDecimal;

public record TradeCompletedEvent(String eventId, Long orderId, String username, String symbol,
                                  String type, int quantity, BigDecimal executionPrice) {
}
