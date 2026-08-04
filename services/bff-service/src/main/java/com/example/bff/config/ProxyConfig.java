package com.example.bff.config;

import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class ProxyConfig {

    @Value("${API_GATEWAY_URL:http://localhost:8081}")
    private String apiGatewayUrl;

    @Bean
    public RouterFunction<ServerResponse> gatewayRoute() {
        return route("api-gateway")
                .route(request -> request.path().startsWith("/api/v1/") && !request.path().startsWith("/api/v1/admin/users"),
                        http(apiGatewayUrl))
                .filter(TokenRelayFilterFunctions.tokenRelay()) // Automatically handles token attach and refresh using OAuth2AuthorizedClientManager
                .before(addRequestHeader("X-Correlation-ID", UUID.randomUUID().toString()))
                .build();
    }
}
