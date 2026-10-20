package com.ZyroTrade.Portfolios;

import com.ZyroTrade.Stocks.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MonetaryPrecisionTests {
    @Test
    void fourPlacePricesUseHalfEvenAtTheInputBoundary() {
        Stock stock = new Stock();
        stock.setPrice(new BigDecimal("1.23445"));
        assertThat(stock.getPrice()).isEqualByComparingTo("1.2344");
        stock.setPrice(new BigDecimal("1.23455"));
        assertThat(stock.getPrice()).isEqualByComparingTo("1.2346");
    }

    @Test
    void repeatedFractionalBuysAccumulateAtFourPlacesAndAverageSafely() {
        PortfolioHolding holding = new PortfolioHolding(7L, "TST");
        holding.applyBuy(1, new BigDecimal("0.1000"));
        holding.applyBuy(2, new BigDecimal("0.2000"));
        holding.applyBuy(3, new BigDecimal("0.3000"));

        assertThat(holding.getQuantity()).isEqualTo(6);
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("0.2333");
        assertThat(holding.getCostBasis()).isEqualByComparingTo("1.4000");
    }

    @Test
    void largeValuesAndNonTerminatingWeightedAverageRemainExactWithinPolicy() {
        PortfolioHolding holding = new PortfolioHolding(7L, "BIG");
        holding.applyBuy(1, new BigDecimal("999999999999999.9999"));
        holding.applyBuy(2, new BigDecimal("0.0001"));

        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("333333333333333.3334");
    }

    @Test
    void sellRejectsOverdrawAndZeroQuantities() {
        PortfolioHolding holding = new PortfolioHolding(7L, "TST");
        holding.applyBuy(2, new BigDecimal("3.0000"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> holding.applySell(3))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> holding.applySell(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(holding.getQuantity()).isEqualTo(2);
    }
}
