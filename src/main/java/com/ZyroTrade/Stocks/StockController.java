package com.ZyroTrade.Stocks;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ZyroTrade.DTO.PageResponse;
import com.ZyroTrade.DTO.StockResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@Validated
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Stocks", description = "Supported instruments and quote freshness")
public class StockController {

    @Autowired
    private StockService stockServicest;
    @Value("${trading.max-price-age-seconds:120}")
    private long maxPriceAgeSeconds;

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "List supported stocks with quote freshness metadata")
    public List<StockResponse> getAllStocks(){
        return stockServicest.getAllStocks().stream().map(stock -> StockResponse.from(stock, maxPriceAgeSeconds)).toList();
    }

    @GetMapping("/search")
    @io.swagger.v3.oas.annotations.Operation(summary = "Search stocks", description = "Case-insensitive search by symbol or company name. Zero-based pages, maximum size 100.")
    public PageResponse<StockResponse> search(@RequestParam("q") String query,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) int size) {
        int boundedSize = Math.min(size, 100);
        return PageResponse.from(stockServicest.search(query, PageRequest.of(page, boundedSize,
                Sort.by(Sort.Direction.ASC, "symbol")))
                .map(stock -> StockResponse.from(stock, maxPriceAgeSeconds)));
    }
  }
