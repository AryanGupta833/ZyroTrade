package com.ZyroTrade.Orders;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private OrderType type;

    private int quantity;
    private double price;
    private Double realizedPnl;

    private LocalDateTime createdAt=LocalDateTime.now();

}
