package com.example.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${backend.baseUrl}")
    private String backendBaseUrl;

    @Bean(name = "backendWebClient")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .baseUrl(backendBaseUrl);
    }
}
