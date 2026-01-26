package com.ZyroTrade.Price;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class FinnhubClient {

    @Value("${finhub.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public double fetchPrice(String symbol) {

        String url = "https://finnhub.io/api/v1/quote"
                + "?symbol=" + symbol
                + "&token=" + apiKey;

        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                restTemplate.getForObject(url, Map.class);

        if (response == null || response.get("c") == null) {
            throw new RuntimeException("Finnhub price missing for " + symbol);
        }

        return Double.parseDouble(response.get("c").toString());
    }
}
