package com.ZyroTrade.DTO;

import java.util.List;

public record PortfolioPnlResponse (
    List<HoldingPnlResponse> holdings,
    double totalPnl
){}

