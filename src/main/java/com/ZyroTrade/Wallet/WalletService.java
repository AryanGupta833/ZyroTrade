package com.ZyroTrade.Wallet;

import com.ZyroTrade.WalletTransaction.TransactionType;
import com.ZyroTrade.WalletTransaction.WalletTransactionRepository;
import com.ZyroTrade.WalletTransaction.WalletTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    public Wallet getMyWallet(String username){return
            walletRepository.findByUserUsername(username).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void lockAccount(String username) {
        walletRepository.findByUserUsernameForUpdate(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void debit(String username, BigDecimal amount){
        amount = normalize(amount);
        Wallet wallet=walletRepository.findByUserUsernameForUpdate(username)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        if(amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if(wallet.getBalance().compareTo(amount)<0){
            throw new InsufficientFundsException();
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        saveTransaction(wallet, TransactionType.DEBIT,amount);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void credit(String username, BigDecimal amount){
        amount = normalize(amount);
        if(amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        Wallet wallet=walletRepository.findByUserUsernameForUpdate(username)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        saveTransaction(wallet,TransactionType.CREDIT,amount);
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        return amount.setScale(4, RoundingMode.HALF_EVEN);
    }

    private void saveTransaction(Wallet wallet, TransactionType type, BigDecimal amount){
        WalletTransaction tx=new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        walletTransactionRepository.save(tx);


    }
}
