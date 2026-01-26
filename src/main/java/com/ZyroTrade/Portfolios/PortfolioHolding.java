package com.ZyroTrade.Portfolios;

import io.lettuce.core.dynamic.annotation.CommandNaming;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "portfolio_holdings",uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","symbol"}))
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id",nullable = false)
    private Long userid;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private double avgBuyPrice;

    @Column(nullable = false)
    private int quantity;

    protected PortfolioHolding(){}
    public PortfolioHolding(Long userId,String symbol){
        this.userid=userId;
        this.symbol=symbol;
        this.quantity=0;
        this.avgBuyPrice=0;
    }

    public void applyBuy(int qty,double price){
        double totalCost=(this.avgBuyPrice*this.quantity)+(price*qty);
        this.quantity+=qty;
        this.avgBuyPrice=totalCost/this.quantity;
    }

    public void applySell(int qty){
        this.quantity-=qty;
    }
}
