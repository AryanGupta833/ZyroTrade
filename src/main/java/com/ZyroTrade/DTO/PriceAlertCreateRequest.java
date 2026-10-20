package com.ZyroTrade.DTO;

import com.ZyroTrade.PriceAlert.AlertType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PriceAlertCreateRequest(@NotBlank @Size(max = 16) String symbol,
                                      @NotNull @DecimalMin(value = "0.0001") BigDecimal targetPrice,
                                      @NotNull AlertType type) {
}
