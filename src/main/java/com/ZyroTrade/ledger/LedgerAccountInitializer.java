package com.ZyroTrade.ledger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LedgerAccountInitializer implements CommandLineRunner {
    private final LedgerService ledgerService;

    public LedgerAccountInitializer(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public void run(String... args) {
        ledgerService.ensureCashAssetAccount();
    }
}
