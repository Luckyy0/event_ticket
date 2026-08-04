package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

    private static final List<String> FORBIDDEN_INBOUND_HEADERS = List.of(
            "X-User-Id",
            "X-Roles",
            "X-User-Email",
            "X-Authenticated-User"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        // Strip any spoofed headers coming from outside
        for (String headerName : FORBIDDEN_INBOUND_HEADERS) {
            requestBuilder.headers(headers -> headers.remove(headerName));
        }

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(requestBuilder.build())
                .build();

        return chain.filter(sanitizedExchange);
    }

    @Override
    public int getOrder() {
        // Run right after CorrelationIdFilter
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
