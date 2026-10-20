package com.ZyroTrade.Wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ZyroTrade.DTO.WalletResponse;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/wallet")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Wallet", description = "Authenticated user's wallet balance")
public class WalletController {
    @Autowired
    private WalletService walletService;
    @Value("${ledger.currency:INR}")
    private String currency;

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Get the authenticated user's wallet")
    public WalletResponse myWallet(){
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        return WalletResponse.from(walletService.getMyWallet(username), currency);

    }
}
