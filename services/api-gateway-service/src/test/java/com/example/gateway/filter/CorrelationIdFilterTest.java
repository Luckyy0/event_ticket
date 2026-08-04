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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
    }

    @Test
    void shouldGenerateCorrelationId_whenNotProvided() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange finalExchange = capturedExchange.get();
        String reqCorrelationId = finalExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(reqCorrelationId).isNotNull().isNotBlank();

        String resCorrelationId = finalExchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(resCorrelationId).isEqualTo(reqCorrelationId);
    }

    @Test
    void shouldPreserveCorrelationId_whenAlreadyPresent() {
        String existingId = "client-correlation-id-999";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(correlationIdFilter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange finalExchange = capturedExchange.get();
        String reqCorrelationId = finalExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(reqCorrelationId).isEqualTo(existingId);

        String resCorrelationId = finalExchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(resCorrelationId).isEqualTo(existingId);
    }
}
