package com.example.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class JwtValidationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void shouldReturn401_whenNoTokenProvided_toProtectedRoute() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowAccess_whenTokenIsValid() {
        Jwt jwt = Jwt.withTokenValue("valid-jwt-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .claim("sub", "user-uuid-123")
                .claim("iss", "http://localhost:8443/realms/event-ticketing")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        when(reactiveJwtDecoder.decode("valid-jwt-token")).thenReturn(Mono.just(jwt));

        webTestClient.get()
                .uri("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .exchange()
                // In test without downstream mock, gateway routes it; not 401
                .expectStatus().isNotFound(); // 404 means passed auth filter and hit route (no downstream running in mock)
    }

    @Test
    void shouldReturn401_whenTokenIsExpired() {
        when(reactiveJwtDecoder.decode("expired-token"))
                .thenReturn(Mono.error(new BadJwtException("Jwt is expired")));

        webTestClient.get()
                .uri("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401_whenTokenHasInvalidSignature() {
        when(reactiveJwtDecoder.decode("invalid-signature-token"))
                .thenReturn(Mono.error(new BadJwtException("Invalid signature")));

        webTestClient.get()
                .uri("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-signature-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401_whenIssuerDoesNotMatch() {
        when(reactiveJwtDecoder.decode("invalid-issuer-token"))
                .thenReturn(Mono.error(new BadJwtException("Issuer does not match")));

        webTestClient.get()
                .uri("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-issuer-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401_whenTokenIsMalformed() {
        when(reactiveJwtDecoder.decode("malformed-token"))
                .thenReturn(Mono.error(new BadJwtException("Malformed JWT")));

        webTestClient.get()
                .uri("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer malformed-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowPublicAccess_toCatalogGetEndpoint_withoutToken() {
        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isNotFound(); // Public pass-through, not 401
    }

    @Test
    void shouldAllowPublicAccess_toPaymentWebhook_withoutToken() {
        webTestClient.post()
                .uri("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"event\":\"payment.success\"}")
                .exchange()
                .expectStatus().isNotFound(); // Public pass-through, not 401
    }
}
