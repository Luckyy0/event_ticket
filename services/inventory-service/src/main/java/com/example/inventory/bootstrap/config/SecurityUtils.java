package com.example.inventory.bootstrap.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException ignored) {
                    return UUID.nameUUIDFromBytes(subject.getBytes());
                }
            }
        }
        // Fallback for non-JWT auth in tests or anonymous
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
