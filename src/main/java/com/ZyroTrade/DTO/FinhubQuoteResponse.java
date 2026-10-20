package com.ZyroTrade.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinhubQuoteResponse {
    @JsonProperty("c")
    private BigDecimal currentPrice;
    @JsonProperty("t")
    private Long quoteEpochSeconds;
}
