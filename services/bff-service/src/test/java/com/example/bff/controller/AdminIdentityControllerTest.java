package com.example.bff.controller;

import com.example.bff.service.AdminIdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminIdentityService adminIdentityService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_whenAdmin_returns200Ok() throws Exception {
        Map<String, Object> body = Map.of("roles", List.of("EVENT_ORGANIZER", "STAFF"));

        mockMvc.perform(post("/api/v1/admin/users/user-1/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User roles successfully updated and all sessions terminated"));

        verify(adminIdentityService).updateUserRoles("user-1", List.of("EVENT_ORGANIZER", "STAFF"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateRoles_whenNonAdmin_returns403Forbidden() throws Exception {
        Map<String, Object> body = Map.of("roles", List.of("ADMIN"));

        mockMvc.perform(post("/api/v1/admin/users/user-1/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verify(adminIdentityService, never()).updateUserRoles(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_whenLdapUser_returns400BadRequest() throws Exception {
        Map<String, Object> body = Map.of("roles", List.of("STAFF"));

        doThrow(new IllegalArgumentException("Cannot modify roles for Enterprise LDAP federated user."))
                .when(adminIdentityService).updateUserRoles("ldap-user", List.of("STAFF"));

        mockMvc.perform(post("/api/v1/admin/users/ldap-user/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot modify roles for Enterprise LDAP federated user."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetPassword_whenAdmin_returns200Ok() throws Exception {
        Map<String, Object> body = Map.of("newPassword", "NewStrongPassword123!");

        mockMvc.perform(post("/api/v1/admin/users/user-1/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User password successfully reset and all sessions terminated"));

        verify(adminIdentityService).resetUserPassword("user-1", "NewStrongPassword123!");
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void resetPassword_whenStaff_returns403Forbidden() throws Exception {
        Map<String, Object> body = Map.of("newPassword", "NewStrongPassword123!");

        mockMvc.perform(post("/api/v1/admin/users/user-1/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verify(adminIdentityService, never()).resetUserPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void logoutAll_whenAdmin_returns200Ok() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/user-1/logout-all")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All sessions successfully terminated for user"));

        verify(adminIdentityService).logoutAllSessions("user-1");
    }
}
