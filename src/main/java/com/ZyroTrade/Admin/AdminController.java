package com.ZyroTrade.Admin;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockService;
import com.ZyroTrade.DTO.AdminStockRequest;
import com.ZyroTrade.DTO.StockResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/stocks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private StockService stockService;
    @Value("${trading.max-price-age-seconds:120}")
    private long maxPriceAgeSeconds;

    @PostMapping
    public StockResponse addStock(@Valid @RequestBody AdminStockRequest request)
    {
        Stock stock = new Stock();
        stock.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        stock.setName(request.name().trim());
        stock.setPrice(BigDecimal.ZERO.setScale(4));
        stock.setTradable(false);
        stock.setLastUpdated(null);
        return StockResponse.from(stockService.addStock(stock), maxPriceAgeSeconds);
    }
    @PutMapping("/{id}/halt")
    public StockResponse haltTrading(@PathVariable Long id){
        return StockResponse.from(stockService.setTradable(id,false), maxPriceAgeSeconds);
    }

    @PutMapping("/{id}/resume")
    public StockResponse resumeTrading(@PathVariable Long id){
        return StockResponse.from(stockService.setTradable(id,true), maxPriceAgeSeconds);
    }
    @DeleteMapping("/{id}")
    public void deleteStock(@PathVariable Long id){
        stockService.deleteStock(id);
    }
}
