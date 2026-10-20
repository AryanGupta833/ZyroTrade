package com.ZyroTrade.Price;

import com.ZyroTrade.DTO.FinhubQuoteResponse;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketDataReliabilityTests {
    @Test
    void clientRetriesBoundedlyAndReturnsProviderTimestamp() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rest);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("symbol=MSFT")))
                .andExpect(method(HttpMethod.GET)).andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("symbol=MSFT")))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(
                        "{\"c\":123.4567,\"t\":1700000000}", org.springframework.http.MediaType.APPLICATION_JSON));
        FinnhubClient client = new FinnhubClient("test-secret", rest, 2);

        FinhubQuoteResponse quote = client.fetchQuote("MSFT");

        assertThat(quote.getCurrentPrice()).isEqualByComparingTo("123.4567");
        assertThat(quote.getQuoteEpochSeconds()).isEqualTo(1700000000L);
        server.verify();
    }

    @Test
    void clientRejectsMissingTimestampAndNonpositivePrices() {
        for (String body : List.of("{\"c\":12.34}", "{\"c\":0,\"t\":1700000000}")) {
            RestTemplate rest = new RestTemplate();
            MockRestServiceServer server = MockRestServiceServer.createServer(rest);
            server.expect(requestTo(org.hamcrest.Matchers.containsString("symbol=AAPL")))
                    .andRespond(withSuccess(body, org.springframework.http.MediaType.APPLICATION_JSON));
            FinnhubClient client = new FinnhubClient("test-secret", rest, 2);
            assertThatThrownBy(() -> client.fetchQuote("AAPL")).isInstanceOf(MarketDataUnavailableException.class);
            server.verify();
        }
    }

    @Test
    void providerCircuitOpensAfterRepeatedFailuresAndAvoidsMoreRequests() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rest);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("symbol=TSLA")))
                .andRespond(withServerError());
        FinnhubClient client = new FinnhubClient("test-secret", rest, 1, 1, 30);

        assertThatThrownBy(() -> client.fetchQuote("TSLA")).isInstanceOf(MarketDataUnavailableException.class);
        assertThatThrownBy(() -> client.fetchQuote("TSLA")).isInstanceOf(MarketDataUnavailableException.class);
        server.verify();
    }

    @Test
    void refreshRejectsFutureQuotesAndDoesNotUndoAdminTradingHalt() {
        StockRepository stocks = mock(StockRepository.class);
        FinnhubClient client = mock(FinnhubClient.class);
        StockQuoteUpdateService updater = mock(StockQuoteUpdateService.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        Stock stock = stock();
        stock.setTradable(false);
        when(stocks.findAll()).thenReturn(List.of(stock));
        when(client.fetchQuote("TST")).thenReturn(quote(Instant.now().plusSeconds(30).getEpochSecond()));

        new ExternalPriceService(stocks, client, updater, meters, 120).refreshPrices();

        assertThat(stock.getPrice()).isEqualByComparingTo("10.0000");
        assertThat(stock.isTradable()).isFalse();
        assertThat(meters.counter("market_data.refresh", "outcome", "invalid_timestamp").count()).isEqualTo(1);
        verify(updater, never()).updateIfNewer(anyString(), any(), any());
    }

    @Test
    void refreshRejectsStaleQuotesInsteadOfMarkingThemFresh() {
        StockRepository stocks = mock(StockRepository.class);
        FinnhubClient client = mock(FinnhubClient.class);
        StockQuoteUpdateService updater = mock(StockQuoteUpdateService.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        Stock stock = stock();
        LocalDateTime priorTimestamp = stock.getLastUpdated();
        when(stocks.findAll()).thenReturn(List.of(stock));
        when(client.fetchQuote("TST")).thenReturn(quote(Instant.now().minusSeconds(600).getEpochSecond()));

        new ExternalPriceService(stocks, client, updater, meters, 120).refreshPrices();

        assertThat(stock.getPrice()).isEqualByComparingTo("10.0000");
        assertThat(stock.getLastUpdated()).isEqualTo(priorTimestamp);
        assertThat(meters.counter("market_data.refresh", "outcome", "stale_quote").count()).isEqualTo(1);
        verify(updater, never()).updateIfNewer(anyString(), any(), any());
    }

    @Test
    void successfulRefreshUsesProviderTimestampAndPreservesAdminHalt() {
        StockRepository stocks = mock(StockRepository.class);
        FinnhubClient client = mock(FinnhubClient.class);
        StockQuoteUpdateService updater = mock(StockQuoteUpdateService.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        Stock stock = stock();
        stock.setTradable(false);
        when(stocks.findAll()).thenReturn(List.of(stock));
        when(client.fetchQuote("TST")).thenReturn(quote(Instant.now().minusSeconds(2).getEpochSecond()));
        when(updater.updateIfNewer(eq("TST"), any(BigDecimal.class), any(LocalDateTime.class))).thenReturn(true);

        new ExternalPriceService(stocks, client, updater, meters, 120).refreshPrices();

        verify(updater).updateIfNewer(eq("TST"), eq(new BigDecimal("20.1234")), any(LocalDateTime.class));
        assertThat(stock.isTradable()).isFalse();
        assertThat(meters.counter("market_data.refresh", "outcome", "success").count()).isEqualTo(1);
    }

    private Stock stock() {
        Stock stock = new Stock();
        stock.setSymbol("TST");
        stock.setPrice(new BigDecimal("10"));
        stock.setLastUpdated(LocalDateTime.now().minusSeconds(10));
        stock.setTradable(true);
        return stock;
    }

    private FinhubQuoteResponse quote(long timestamp) {
        FinhubQuoteResponse response = new FinhubQuoteResponse();
        response.setCurrentPrice(new BigDecimal("20.1234"));
        response.setQuoteEpochSeconds(timestamp);
        return response;
    }
}
