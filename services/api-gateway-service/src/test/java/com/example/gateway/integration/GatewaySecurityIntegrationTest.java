package com.example.gateway.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {

    static final KeycloakContainer keycloak;

    static {
        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:21.1.1")
                .withRealmImportFile("realm-test.json")
                .withStartupTimeout(Duration.ofMinutes(3));
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/event-ticketing");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/event-ticketing/protocol/openid-connect/certs");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowPublicAccess_withoutToken() {
        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isNotFound(); // Public pass-through
    }

    @Test
    void shouldDenyAccessToProtectedEndpoint_withoutToken() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldPropagateCorrelationId_inResponseHeader() {
        webTestClient.get()
                .uri("/api/v1/events")
                .header("X-Correlation-Id", "custom-trace-id-12345")
                .exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "custom-trace-id-12345");
    }
}
