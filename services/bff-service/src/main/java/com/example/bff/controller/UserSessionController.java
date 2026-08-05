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

    /**
     * API trích xuất thông tin người dùng đang đăng nhập trong phiên hiện tại (GET /api/auth/me).
     * 
     * 1. VAI TRÒ CỦA API NÀY TRONG MÔ HÌNH BFF:
     *    - Frontend (React/Vue) không lưu trữ trực tiếp Access Token (tránh rủi ro rò rỉ XSS).
     *    - Sau khi đăng nhập xong, Frontend chỉ cần gọi GET /api/auth/me kèm Cookie SESSION.
     *    - BFF đọc thông tin từ SecurityContext và trả về JSON chuẩn (userId, username, email, roles,...)
     *      để Frontend hiển thị tên người dùng và phân quyền giao diện (Conditional UI Rendering).
     * 
     * 2. TẠI SAO CÓ IF-ELSE CHO OidcUser VÀ OAuth2User?
     *    - "OidcUser": Đại diện cho người dùng đăng nhập qua chuẩn OpenID Connect (OIDC) - có chứa ID Token.
     *    - "OAuth2User": Đại diện cho người dùng đăng nhập qua OAuth 2.0 thuần túy (như GitHub) - chỉ có User Attributes Map.
     * 
     * 3. SPRING SẼ TỰ ĐỘNG ĐƯA LOẠI NÀO VÀO?
     *    - Với Keycloak (có cấu hình scope "openid"): Spring Security sẽ LUÔN đưa đối tượng OidcUser vào (nhánh 1).
     *    - Với các nhà cung cấp chỉ hỗ trợ OAuth2 thường (như GitHub): Spring Security sẽ đưa OAuth2User vào (nhánh 2).
     * 
     * @param principal Đối tượng danh tính người dùng được Spring Security tự động inject từ SecurityContext.
     * @return Thông tin người dùng chuẩn hóa dưới dạng JSON.
     */
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

        // Nhánh 1: Xử lý khi đăng nhập qua OpenID Connect (Keycloak, Google OIDC, Azure AD)
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
        // Nhánh 2: Xử lý khi đăng nhập qua OAuth2 thuần (GitHub, Facebook, hoặc provider không có scope openid)
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

    /**
     * API đăng xuất người dùng khỏi TẤT CẢ các thiết bị đang đăng nhập (POST /api/auth/logout-all).
     * 
     * TẠI SAO CẦN CẢ 2 BƯỚC (adminIdentityService + SecurityContextLogoutHandler)?
     * -----------------------------------------------------------------------------------------
     * 1. Bước 1: adminIdentityService.logoutAllSessions(userId) -> XỬ LÝ PHÍA KEYCLOAK (TOÀN CỤC)
     *    - Gọi sang Keycloak Admin REST API để thu hồi toàn bộ User Sessions và Refresh Tokens của user trên Keycloak.
     *    - Khiến tất cả các thiết bị khác (điện thoại, máy tính khác) bị vô hiệu hóa khi cố làm mới token.
     * 
     * 2. Bước 2: SecurityContextLogoutHandler().logout(request, response, null) -> XỬ LÝ PHÍA BFF (CỤC BỘ)
     *    - Hủy ngay lập tức HTTP Session lưu trong Redis của thiết bị hiện tại (session.invalidate()).
     *    - Xóa Authentication trong bộ nhớ ThreadLocal (SecurityContextHolder.clearContext()).
     *    - Trả về chỉ thị xóa Cookie SESSION trên trình duyệt hiện tại.
     * 
     * NẾU THIẾU BƯỚC 2:
     *    - Thiết bị hiện tại vẫn còn giữ Session Cookie và Access Token cục bộ (còn hạn vài phút).
     *    - Người dùng bấm đăng xuất nhưng F5 lại vẫn thấy tài khoản đang hoạt động cho đến khi token hết hạn.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(
            @AuthenticationPrincipal Object principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        // 1. Vô hiệu hóa toàn bộ session trên Keycloak (áp dụng cho mọi thiết bị khác)
        if (principal instanceof OidcUser oidcUser) {
            String userId = oidcUser.getSubject();
            adminIdentityService.logoutAllSessions(userId);
        }

        // 2. Hủy ngay phiên làm việc cục bộ và xóa cookie trên chính thiết bị đang gửi request này
        new SecurityContextLogoutHandler().logout(request, response, null);

        return ResponseEntity.ok(Map.of("message", "Successfully logged out from all devices"));
    }
}
