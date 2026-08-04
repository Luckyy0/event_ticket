package com.example.gateway.resourceserver;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Utility helpers for Resource Server method-level security and ownership authorization.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Extracts current authenticated user UUID from JWT subject ('sub').
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new AccessDeniedException("User is not authenticated via JWT");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }

    /**
     * Verifies that the current user owns the specified resource, or has ADMIN role.
     */
    public static void validateOwnership(String resourceOwnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Unauthenticated request");
        }

        // Admins can bypass ownership checks
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        String currentUserId = getCurrentUserId();
        if (!currentUserId.equals(resourceOwnerId)) {
            throw new AccessDeniedException("Access Denied: Current user does not own this resource");
        }
    }
}
