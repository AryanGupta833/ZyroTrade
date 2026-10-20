package com.ZyroTrade.Orders;

public class TradingHaltedException extends IllegalStateException {
    public TradingHaltedException() {
        super("Trading is halted for this stock");
    }
}
