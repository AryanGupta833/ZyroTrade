package com.ZyroTrade.Orders;

public class CostBasisUnavailableException extends RuntimeException {
    public CostBasisUnavailableException() {
        super("This holding requires a reviewed cost-basis migration before it can be traded");
    }
}
