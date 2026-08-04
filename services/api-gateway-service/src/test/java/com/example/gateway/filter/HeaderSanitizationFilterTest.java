package com.example.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderSanitizationFilterTest {

    private HeaderSanitizationFilter headerSanitizationFilter;

    @BeforeEach
    void setUp() {
        headerSanitizationFilter = new HeaderSanitizationFilter();
    }

    @Test
    void shouldStripSpoofedHeaders_beforeForwarding() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header("X-User-Id", "attacker-injected-id")
                .header("X-Roles", "ROLE_ADMIN")
                .header("X-User-Email", "attacker@evil.com")
                .header("X-Authenticated-User", "root")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Correlation-Id", "corr-12345")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(headerSanitizationFilter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange finalExchange = capturedExchange.get();
        HttpHeaders forwardedHeaders = finalExchange.getRequest().getHeaders();

        assertThat(forwardedHeaders.containsKey("X-User-Id")).isFalse();
        assertThat(forwardedHeaders.containsKey("X-Roles")).isFalse();
        assertThat(forwardedHeaders.containsKey("X-User-Email")).isFalse();
        assertThat(forwardedHeaders.containsKey("X-Authenticated-User")).isFalse();

        // Preserved legitimate headers
        assertThat(forwardedHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(forwardedHeaders.getFirst("X-Correlation-Id")).isEqualTo("corr-12345");
    }
}
