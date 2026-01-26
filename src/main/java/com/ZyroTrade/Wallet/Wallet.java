package com.ZyroTrade.Wallet;

import com.ZyroTrade.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id",nullable = false,unique = true)
    private User user;

    @Column(nullable = false)
    private double balance;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreated(){
        createdAt=LocalDateTime.now();
                updatedAt=createdAt;
    }
    @PreUpdate
    void onUpdate(){
        updatedAt=LocalDateTime.now();
    }
}
