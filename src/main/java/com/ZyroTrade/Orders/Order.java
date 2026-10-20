package com.ZyroTrade.Orders;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", uniqueConstraints =
        @UniqueConstraint(name = "uq_orders_idempotency_key", columnNames = "idempotency_key"))
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
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    private LocalDateTime createdAt=LocalDateTime.now();

}
