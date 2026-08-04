package com.example.gateway.resourceserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceServerSecurityPatternTest {

    private JwtAuthConverter jwtAuthConverter;

    @BeforeEach
    void setUp() {
        jwtAuthConverter = new JwtAuthConverter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractRealmAndClientRoles_fromKeycloakJwt() {
        Jwt jwt = Jwt.withTokenValue("sample-jwt")
                .header("alg", "RS256")
                .claim("sub", "user-uuid-101")
                .claim("preferred_username", "organizer1")
                .claim("scope", "openid profile email")
                .claim("realm_access", Map.of("roles", List.of("EVENT_ORGANIZER", "CUSTOMER")))
                .claim("resource_access", Map.of("ticket-client", Map.of("roles", List.of("TICKET_ISSUER"))))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken authToken = jwtAuthConverter.convert(jwt);

        assertThat(authToken).isNotNull();
        assertThat(authToken.getName()).isEqualTo("organizer1");

        Collection<String> authorities = authToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).contains(
                "SCOPE_openid",
                "SCOPE_profile",
                "SCOPE_email",
                "ROLE_EVENT_ORGANIZER",
                "ROLE_CUSTOMER",
                "ROLE_TICKET_ISSUER"
        );
    }

    @Test
    void shouldAllowAccess_whenUserOwnsResource() {
        Jwt jwt = Jwt.withTokenValue("sample-jwt")
                .header("alg", "RS256")
                .claim("sub", "user-owner-123")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken authToken = jwtAuthConverter.convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Current user is user-owner-123 accessing resource owned by user-owner-123
        SecurityUtils.validateOwnership("user-owner-123");
    }

    @Test
    void shouldThrowAccessDenied_whenUserDoesNotOwnResource() {
        Jwt jwt = Jwt.withTokenValue("sample-jwt")
                .header("alg", "RS256")
                .claim("sub", "user-attacker-456")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken authToken = jwtAuthConverter.convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Attacker accessing resource owned by user-victim-789
        assertThatThrownBy(() -> SecurityUtils.validateOwnership("user-victim-789"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Current user does not own this resource");
    }

    @Test
    void shouldAllowAdmin_toBypassOwnershipCheck() {
        Jwt jwt = Jwt.withTokenValue("sample-jwt")
                .header("alg", "RS256")
                .claim("sub", "admin-user-000")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken authToken = jwtAuthConverter.convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Admin accessing resource owned by user-victim-789 is permitted
        SecurityUtils.validateOwnership("user-victim-789");
    }
}
