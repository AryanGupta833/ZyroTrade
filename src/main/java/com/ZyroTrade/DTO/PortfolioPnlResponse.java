package com.ZyroTrade.DTO;

import java.util.List;
import java.math.BigDecimal;

public record PortfolioPnlResponse (
    List<HoldingPnlResponse> holdings,
    BigDecimal totalPnl
){}

