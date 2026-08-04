package com.example.bff.controller;

import com.example.bff.config.TestSessionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSessionConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRedirectToKeycloak_whenLoginRequested() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("http://localhost:8443/realms/event-ticketing/protocol/openid-connect/auth")));
    }

    @Test
    void shouldRedirectToKeycloak_whenLoginEndpointRequested() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/keycloak"));
    }

    @Test
    void shouldRedirectToKeycloakWithGoogleIdpHint_whenGoogleLoginRequested() throws Exception {
        mockMvc.perform(get("/api/auth/login/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/keycloak?kc_idp_hint=google"));
    }

    @Test
    void shouldIncludeKcIdpHintInKeycloakAuthUrl_whenProvided() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak?kc_idp_hint=google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        startsWith("http://localhost:8443/realms/event-ticketing/protocol/openid-connect/auth"),
                        containsString("kc_idp_hint=google")
                )));
    }

    @Test
    void shouldReturn401_whenNoCookieProvided() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireCsrfToken_whenLogoutRequested() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden()); // Missing CSRF
    }

    @Test
    void shouldLogout_whenCsrfTokenProvided() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().is3xxRedirection()); // Redirects after logout
    }
}
