package com.ZyroTrade.ledger;

public class LedgerCutoverRequiredException extends IllegalStateException {
    public LedgerCutoverRequiredException() {
        super("Wallet ledger opening balance has not been reviewed");
    }
}
