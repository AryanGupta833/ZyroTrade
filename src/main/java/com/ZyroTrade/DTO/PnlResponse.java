package com.ZyroTrade.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PnlResponse {
    private String symbol;
    private int quantity;
    private double avgBuyPrice;
    private double currentPrice;
    private double unrealizedPnl;

}
