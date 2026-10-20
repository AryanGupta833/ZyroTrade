package com.ZyroTrade.Orders;

public class IdempotencyConflictException extends IllegalArgumentException {
    public IdempotencyConflictException() {
        super("Idempotency key was already used for a different request");
    }
}
