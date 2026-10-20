package com.ZyroTrade.Orders;

public class InsufficientHoldingsException extends RuntimeException {
    public InsufficientHoldingsException() {
        super("Insufficient holdings for this sell order");
    }
}
