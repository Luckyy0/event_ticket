package com.example.bff.integration;

import com.example.bff.config.TestSessionConfig;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSessionConfig.class)
class BffAuthIntegrationTest {

    static final KeycloakContainer keycloak;

    static {
        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.3.0")
                .withRealmImportFile("realm-test.json")
                .withStartupTimeout(Duration.ofMinutes(3));
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        String authServerUrl = keycloak.getAuthServerUrl();
        registry.add("spring.security.oauth2.client.provider.keycloak.authorization-uri",
                () -> authServerUrl + "/realms/event-ticketing/protocol/openid-connect/auth");
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> authServerUrl + "/realms/event-ticketing/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.client.provider.keycloak.jwk-set-uri",
                () -> authServerUrl + "/realms/event-ticketing/protocol/openid-connect/certs");
        registry.add("spring.security.oauth2.client.provider.keycloak.user-info-uri",
                () -> authServerUrl + "/realms/event-ticketing/protocol/openid-connect/userinfo");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRedirectToKeycloak_withRealKeycloak() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection());
    }
}
