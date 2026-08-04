package com.example.bff.controller;

import com.example.bff.service.AdminIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminIdentityService adminIdentityService;

    @Test
    void getCurrentUser_whenUnauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_whenAuthenticated_returnsProfileWithLocaleAndBirthYear() throws Exception {
        Map<String, Object> claims = Map.of(
                "sub", "user-uuid-1234",
                "preferred_username", "johndoe",
                "email", "john@example.com",
                "name", "John Doe",
                "locale", "en",
                "birth_year", "1995"
        );

        OidcIdToken idToken = new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUser oidcUser = new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                idToken
        );

        mockMvc.perform(get("/api/auth/me").with(oidcLogin().oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-uuid-1234"))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.birthYear").value("1995"));
    }

    @Test
    void getCurrentUser_whenLocaleMissing_defaultsToVietnamese() throws Exception {
        Map<String, Object> claims = Map.of(
                "sub", "user-uuid-5678",
                "preferred_username", "nguyenvan"
        );

        OidcIdToken idToken = new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUser oidcUser = new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                idToken
        );

        mockMvc.perform(get("/api/auth/me").with(oidcLogin().oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-uuid-5678"))
                .andExpect(jsonPath("$.locale").value("vi")); // Defaults to Vietnamese
    }

    @Test
    void logoutAllDevices_whenAuthenticated_callsLogoutAndClearsSession() throws Exception {
        Map<String, Object> claims = Map.of("sub", "user-uuid-1234");
        OidcIdToken idToken = new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUser oidcUser = new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                idToken
        );

        mockMvc.perform(post("/api/auth/logout-all")
                        .with(csrf())
                        .with(oidcLogin().oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out from all devices"));

        verify(adminIdentityService).logoutAllSessions("user-uuid-1234");
    }
}
