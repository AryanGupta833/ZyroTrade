package com.ZyroTrade.ledger;

import java.math.BigDecimal;

public record LedgerPosting(String accountCode, LedgerSide side, BigDecimal amount) {
}
