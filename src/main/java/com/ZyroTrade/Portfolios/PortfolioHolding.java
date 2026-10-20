package com.ZyroTrade.Portfolios;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgBuyPrice = BigDecimal.ZERO.setScale(4);

    @Column(precision = 29, scale = 4)
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(4);

    @Column(nullable = false)
    private int quantity;

    protected PortfolioHolding(){}
    public PortfolioHolding(Long userId,String symbol){
        this.userid=userId;
        this.symbol=symbol;
        this.quantity=0;
        this.avgBuyPrice=BigDecimal.ZERO.setScale(4);
        this.costBasis=BigDecimal.ZERO.setScale(4);
    }

    public void applyBuy(int qty, BigDecimal price){
        if (qty <= 0 || price == null || price.signum() <= 0) throw new IllegalArgumentException("Positive quantity and price are required");
        if (quantity > 0 && (costBasis == null || costBasis.signum() <= 0)) throw new IllegalStateException("Reviewed cost basis is required before adding to this holding");
        if (costBasis == null) costBasis = BigDecimal.ZERO.setScale(4);
        int newQuantity = Math.addExact(this.quantity, qty);
        BigDecimal newCost = price.setScale(4, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(qty));
        this.costBasis = this.costBasis.add(newCost);
        this.avgBuyPrice = this.costBasis.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_EVEN);
        this.quantity = newQuantity;
    }

    public void applySell(int qty){
        if (qty <= 0 || qty > quantity) throw new IllegalArgumentException("Sale quantity exceeds holdings");
        if (costBasis == null || costBasis.signum() <= 0) throw new IllegalStateException("Reviewed cost basis is required before selling this holding");
        BigDecimal allocatedCost = qty == quantity ? costBasis : costBasis.multiply(BigDecimal.valueOf(qty))
                .divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_EVEN);
        costBasis = costBasis.subtract(allocatedCost);
        this.quantity-=qty;
        if (quantity > 0) avgBuyPrice = costBasis.divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_EVEN);
    }
}
