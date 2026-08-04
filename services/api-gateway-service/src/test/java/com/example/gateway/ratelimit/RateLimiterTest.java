package com.example.gateway.ratelimit;

import com.example.gateway.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class RateLimiterTest {

    private RateLimitConfig rateLimitConfig;
    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        keyResolver = rateLimitConfig.userOrIpKeyResolver();
    }

    @Test
    void shouldResolveToIpAddress_whenUnauthenticated() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
                .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("192.168.1.50")
                .verifyComplete();
    }

    @Test
    void shouldResolveToPrincipalName_whenAuthenticated() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request)
                .mutate()
                .principal(reactor.core.publisher.Mono.just(new UsernamePasswordAuthenticationToken("user-uuid-999", "n/a")))
                .build();

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user-uuid-999")
                .verifyComplete();
    }
}
