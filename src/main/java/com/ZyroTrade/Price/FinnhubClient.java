package com.ZyroTrade.Price;

import com.ZyroTrade.DTO.FinhubQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FinnhubClient {
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final int retryAttempts;
    private final int failureThreshold;
    private final long circuitOpenMillis;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong circuitOpenedUntil = new AtomicLong();

    @Autowired
    public FinnhubClient(@Value("${finhub.api.key:}") String apiKey, RestTemplate restTemplate,
                         @Value("${finhub.retry-attempts:2}") int retryAttempts,
                         @Value("${finhub.circuit-failure-threshold:5}") int failureThreshold,
                         @Value("${finhub.circuit-open-seconds:30}") long circuitOpenSeconds) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.retryAttempts = Math.max(1, Math.min(retryAttempts, 4));
        this.failureThreshold = Math.max(1, failureThreshold);
        this.circuitOpenMillis = Math.max(1, circuitOpenSeconds) * 1000;
    }

    public FinnhubClient(String apiKey, RestTemplate restTemplate, int retryAttempts) {
        this(apiKey, restTemplate, retryAttempts, 5, 30);
    }

    public FinhubQuoteResponse fetchQuote(String symbol) {
        if (apiKey == null || apiKey.isBlank()) throw new MarketDataUnavailableException();
        if (System.currentTimeMillis() < circuitOpenedUntil.get()) throw new MarketDataUnavailableException();
        URI uri = UriComponentsBuilder.fromUriString("https://finnhub.io/api/v1/quote")
                .queryParam("symbol", symbol).queryParam("token", apiKey).build().encode().toUri();
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                FinhubQuoteResponse response = restTemplate.getForObject(uri, FinhubQuoteResponse.class);
                if (response == null || response.getCurrentPrice() == null || response.getQuoteEpochSeconds() == null
                        || response.getCurrentPrice().signum() <= 0 || response.getQuoteEpochSeconds() <= 0) {
                    recordFailure();
                    throw new MarketDataUnavailableException();
                }
                consecutiveFailures.set(0);
                circuitOpenedUntil.set(0);
                return response;
            } catch (MarketDataUnavailableException invalid) {
                throw invalid;
            } catch (HttpStatusCodeException providerFailure) {
                if (attempt == retryAttempts) {
                    recordFailure();
                    throw new MarketDataUnavailableException();
                }
                pauseBeforeRetry(attempt, providerFailure.getResponseHeaders() == null ? null
                        : providerFailure.getResponseHeaders().getFirst("Retry-After"));
            } catch (RuntimeException providerFailure) {
                if (attempt == retryAttempts) {
                    recordFailure();
                    throw new MarketDataUnavailableException();
                }
                pauseBeforeRetry(attempt, null);
            }
        }
        throw new MarketDataUnavailableException();
    }

    private void pauseBeforeRetry(int attempt, String retryAfter) {
        long delay = 100L << Math.min(attempt - 1, 4);
        if (retryAfter != null) {
            try {
                delay = Math.min(2000, Math.max(0, Long.parseLong(retryAfter)) * 1000);
            } catch (NumberFormatException ignored) {
                // HTTP-date Retry-After values fall back to bounded exponential backoff.
            }
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            recordFailure();
            throw new MarketDataUnavailableException();
        }
    }

    private void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            circuitOpenedUntil.set(System.currentTimeMillis() + circuitOpenMillis);
        }
    }
}
