package com.ZyroTrade.DTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate marketDataRestTemplate(RestTemplateBuilder builder,
            @Value("${finhub.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${finhub.read-timeout-ms:3000}") long readTimeoutMs) {
        return builder.setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs)).build();
    }
}
