package com.ZyroTrade.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PnlResponse {
    private String symbol;
    private int quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnl;
    private String quoteStatus;
    private String costBasisStatus;

}
