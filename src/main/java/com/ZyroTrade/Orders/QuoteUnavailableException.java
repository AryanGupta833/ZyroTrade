package com.ZyroTrade.Orders;

public class QuoteUnavailableException extends IllegalStateException {
    public QuoteUnavailableException(String message) {
        super(message);
    }
}
