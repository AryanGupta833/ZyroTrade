package com.ZyroTrade.outbox;

import java.math.BigDecimal;

public record TradeCompletedPayload(Long orderId, String username, String symbol, String type,
                                    int quantity, BigDecimal executionPrice) {
}
