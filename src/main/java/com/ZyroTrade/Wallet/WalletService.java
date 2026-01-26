package com.ZyroTrade.Wallet;

import com.ZyroTrade.WalletTransaction.TransactionType;
import com.ZyroTrade.WalletTransaction.WalletTransactionRepository;
import com.ZyroTrade.WalletTransaction.WalletTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    public Wallet getMyWallet(String username){return
            walletRepository.findByUserUsername(username).orElseThrow(()->new RuntimeException("Wallet not found"));
    }

    public void debit(String username,double amount){
        Wallet wallet=getMyWallet(username);
        if(wallet.getBalance()<amount){
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance()-amount);
        walletRepository.save(wallet);
        saveTransaction(wallet, TransactionType.DEBIT,amount);
    }

    public void credit(String username,double amount){
        Wallet wallet=getMyWallet(username);
        wallet.setBalance(wallet.getBalance()+amount);
        walletRepository.save(wallet);
        saveTransaction(wallet,TransactionType.CREDIT,amount);
    }

    private void saveTransaction(Wallet wallet, TransactionType type, double amount){
        WalletTransaction tx=new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        walletTransactionRepository.save(tx);


    }
}
