package com.ZyroTrade.ledger;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Ledger administration")
public class LedgerReconciliationController {
    private final LedgerReconciliationService reconciliationService;

    public LedgerReconciliationController(LedgerReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/wallet-reconciliation")
    public List<WalletReconciliationResult> reconcileWallets() {
        return reconciliationService.reconcileAllWallets();
    }
}
