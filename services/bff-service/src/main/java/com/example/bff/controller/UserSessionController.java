package com.example.bff.controller;

import com.example.bff.service.AdminIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserSessionController {

    private final AdminIdentityService adminIdentityService;

    public UserSessionController(AdminIdentityService adminIdentityService) {
        this.adminIdentityService = adminIdentityService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String locale = oidcUser.getClaimAsString("locale");
        if (locale == null || locale.isBlank()) {
            locale = "vi"; // Default locale
        }

        String birthYear = oidcUser.getClaimAsString("birth_year");

        return ResponseEntity.ok(Map.of(
            "userId", oidcUser.getSubject(),
            "username", oidcUser.getPreferredUsername() != null ? oidcUser.getPreferredUsername() : "",
            "email", oidcUser.getEmail() != null ? oidcUser.getEmail() : "",
            "fullName", oidcUser.getFullName() != null ? oidcUser.getFullName() : "",
            "roles", oidcUser.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .toList(),
            "locale", locale,
            "birthYear", birthYear != null ? birthYear : ""
        ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (oidcUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String userId = oidcUser.getSubject();
        adminIdentityService.logoutAllSessions(userId);

        new SecurityContextLogoutHandler().logout(request, response, null);

        return ResponseEntity.ok(Map.of("message", "Successfully logged out from all devices"));
    }
}
