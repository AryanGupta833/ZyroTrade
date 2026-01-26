package com.ZyroTrade.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FinhubQuoteResponse {
    @JsonProperty("c")
    private Double currentPrice;
}
