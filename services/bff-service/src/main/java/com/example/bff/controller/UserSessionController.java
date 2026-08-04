package com.example.bff.controller;

import com.example.bff.service.AdminIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserSessionController {

    private final AdminIdentityService adminIdentityService;

    public UserSessionController(AdminIdentityService adminIdentityService) {
        this.adminIdentityService = adminIdentityService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String userId = "";
        String username = "";
        String email = "";
        boolean emailVerified = false;
        String fullName = "";
        String locale = "vi";
        String birthYear = "";
        List<String> roles = List.of();

        if (principal instanceof OidcUser oidcUser) {
            userId = oidcUser.getSubject() != null ? oidcUser.getSubject() : "";
            username = oidcUser.getPreferredUsername() != null ? oidcUser.getPreferredUsername() : "";
            email = oidcUser.getEmail() != null ? oidcUser.getEmail() : "";
            Boolean ev = oidcUser.getEmailVerified();
            if (ev != null) {
                emailVerified = ev;
            }
            fullName = oidcUser.getFullName() != null ? oidcUser.getFullName() : "";
            String loc = oidcUser.getClaimAsString("locale");
            if (loc != null && !loc.isBlank()) {
                locale = loc;
            }
            birthYear = oidcUser.getClaimAsString("birth_year");
            roles = oidcUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        } else if (principal instanceof OAuth2User oauth2User) {
            userId = oauth2User.getName() != null ? oauth2User.getName() : "";
            email = (String) oauth2User.getAttributes().getOrDefault("email", "");
            Object ev = oauth2User.getAttributes().get("email_verified");
            if (ev instanceof Boolean b) {
                emailVerified = b;
            }
            username = !email.isBlank() ? email : userId;
            fullName = (String) oauth2User.getAttributes().getOrDefault("name", "");
            String loc = (String) oauth2User.getAttributes().get("locale");
            if (loc != null && !loc.isBlank()) {
                locale = loc;
            }
            roles = oauth2User.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "username", username,
            "email", email,
            "emailVerified", emailVerified,
            "fullName", fullName,
            "roles", roles,
            "locale", locale,
            "birthYear", birthYear != null ? birthYear : ""
        ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(
            @AuthenticationPrincipal Object principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        if (principal instanceof OidcUser oidcUser) {
            String userId = oidcUser.getSubject();
            adminIdentityService.logoutAllSessions(userId);
        }

        new SecurityContextLogoutHandler().logout(request, response, null);

        return ResponseEntity.ok(Map.of("message", "Successfully logged out from all devices"));
    }
}
