package com.example.bff.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRedirectToKeycloak_whenLoginRequested() throws Exception {
        mockMvc.perform(get("/api/auth/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost:8443/realms/event-ticketing/protocol/openid-connect/auth?response_type=code&client_id=bff-client&scope=openid%20profile%20email&state=*&redirect_uri=*&nonce=*&code_challenge=*&code_challenge_method=S256"));
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
