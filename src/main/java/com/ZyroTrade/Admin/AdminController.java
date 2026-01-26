package com.ZyroTrade.Admin;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stocks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private StockService stockService;

    @PostMapping
    public Stock addStock(@RequestBody Stock stock)
    {
        return stockService.addStock(stock);
    }
    @PutMapping("/{id}/halt")
    public Stock haltTrading(@PathVariable Long id){
        return stockService.setTradable(id,false);
    }

    @PutMapping("/{id}/resume")
    public Stock resumeTrading(@PathVariable Long id){
        return stockService.setTradable(id,true);
    }
    @DeleteMapping("/{id}")
    public void deleteStock(@PathVariable Long id){
        stockService.deleteStock(id);
    }
}
