package com.ZyroTrade.Wallet;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Insufficient balance");
    }
}
