package com.ZyroTrade.PriceAlert;

import com.ZyroTrade.DTO.PriceAlertCreateRequest;
import com.ZyroTrade.DTO.PriceAlertResponse;
import com.ZyroTrade.Stocks.StockRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Price alerts")
public class PriceAlertController {
    private final PriceAlertRepository alerts;
    private final StockRepository stocks;

    public PriceAlertController(PriceAlertRepository alerts, StockRepository stocks) {
        this.alerts = alerts;
        this.stocks = stocks;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceAlertResponse createAlert(@Valid @RequestBody PriceAlertCreateRequest request) {
        String symbol = request.symbol().trim().toUpperCase(java.util.Locale.ROOT);
        if (!stocks.existsBySymbol(symbol)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found");
        PriceAlert alert = new PriceAlert();
        alert.setUsername(currentUsername());
        alert.setSymbol(symbol);
        alert.setTargetPrice(request.targetPrice());
        alert.setAlertType(request.type());
        alert.setTriggered(false);
        return PriceAlertResponse.from(alerts.save(alert));
    }

    @GetMapping
    public List<PriceAlertResponse> myAlerts() {
        return alerts.findByUsername(currentUsername()).stream().map(PriceAlertResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@PathVariable Long id) {
        PriceAlert alert = alerts.findByIdAndUsername(id, currentUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        alerts.delete(alert);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
