package com.ZyroTrade.Price;

public class MarketDataUnavailableException extends RuntimeException {
    public MarketDataUnavailableException() {
        super("Market price is temporarily unavailable");
    }
}
