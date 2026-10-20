package com.ZyroTrade.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminStockRequest(@NotBlank @Size(max = 16) String symbol,
                                @NotBlank @Size(max = 120) String name) {
}
