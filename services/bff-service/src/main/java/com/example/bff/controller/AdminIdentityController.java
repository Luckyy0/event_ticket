package com.example.bff.controller;

import com.example.bff.dto.ResetPasswordRequest;
import com.example.bff.dto.UpdateRolesRequest;
import com.example.bff.service.AdminIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIdentityController {

    private final AdminIdentityService adminIdentityService;

    public AdminIdentityController(AdminIdentityService adminIdentityService) {
        this.adminIdentityService = adminIdentityService;
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<?> updateRoles(
            @PathVariable("userId") String userId,
            @RequestBody UpdateRolesRequest request) {
        try {
            adminIdentityService.updateUserRoles(userId, request.getRoles());
            return ResponseEntity.ok(Map.of("message", "User roles successfully updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable("userId") String userId,
            @RequestBody ResetPasswordRequest request) {
        try {
            adminIdentityService.resetUserPassword(userId, request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "User password successfully reset and all sessions terminated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/logout-all")
    public ResponseEntity<?> logoutAll(@PathVariable("userId") String userId) {
        try {
            adminIdentityService.logoutAllSessions(userId);
            return ResponseEntity.ok(Map.of("message", "All sessions successfully terminated for user"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
