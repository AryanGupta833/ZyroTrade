package com.ZyroTrade.Stocks;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price = BigDecimal.ZERO.setScale(4);

    public void setPrice(BigDecimal price) {
        this.price = price == null ? null : price.setScale(4, RoundingMode.HALF_EVEN);
    }

    @Column(nullable = false)
    boolean tradable=true;

    private LocalDateTime lastUpdated=LocalDateTime.now();

}
