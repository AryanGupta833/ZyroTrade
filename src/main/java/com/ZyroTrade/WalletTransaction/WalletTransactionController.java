package com.ZyroTrade.WalletTransaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletTransactionController {

@Autowired
    private WalletTransactionRepository walletTransactionRepository;

@GetMapping("/transactions")
    public List<WalletTransaction> history(){
    String username= SecurityContextHolder.getContext().getAuthentication().getName();
    return walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(username);

}

}
