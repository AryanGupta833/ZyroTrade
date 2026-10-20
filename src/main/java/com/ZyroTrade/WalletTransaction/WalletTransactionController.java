package com.ZyroTrade.WalletTransaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import com.ZyroTrade.DTO.WalletTransactionResponse;
import com.ZyroTrade.DTO.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/wallet")
@Validated
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Wallet transactions")
public class WalletTransactionController {

@Autowired
    private WalletTransactionRepository walletTransactionRepository;

@GetMapping("/transactions")
public List<WalletTransactionResponse> history(){
    String username= SecurityContextHolder.getContext().getAuthentication().getName();
    return walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(username)
            .stream().map(WalletTransactionResponse::from).toList();

}

@GetMapping("/transactions/page")
public PageResponse<WalletTransactionResponse> historyPage(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return PageResponse.from(walletTransactionRepository.findByWalletUserUsernameOrderByCreatedAtDesc(username,
                    PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(WalletTransactionResponse::from));
}

}
