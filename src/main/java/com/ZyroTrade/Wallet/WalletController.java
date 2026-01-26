package com.ZyroTrade.Wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;

    @GetMapping
    public Wallet myWallet(){
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        return walletService.getMyWallet(username);

    }
}
