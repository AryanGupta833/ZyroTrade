package com.ZyroTrade.Stocks;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private StockService stockServicest;

    @GetMapping
    public List<Stock> getAllStocks(){
        return stockServicest.getAllStocks();
    }
  }
