package com.example.bff.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Value("${KEYCLOAK_SERVER_URL:http://localhost:8443}")
    private String serverUrl;

    @Value("${KEYCLOAK_REALM:event-ticketing}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:bff-client}")
    private String clientId;

    @Value("${KEYCLOAK_CLIENT_SECRET:secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
