package com.ZyroTrade.Portfolios;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Stock stock;

    private int quantity;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPrice = BigDecimal.ZERO.setScale(4);

    @Column(precision = 29, scale = 4)
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(4);

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice == null ? null : avgPrice.setScale(4, RoundingMode.HALF_EVEN);
    }
}
