package com.example.bff.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/login/**", "/api/auth/callback").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new SpaAuthenticationEntryPoint())
            )
            .oauth2Login(oauth2 -> oauth2
                .loginProcessingUrl("/api/auth/callback")
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestResolver(customAuthorizationRequestResolver())
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(keycloakGrantedAuthoritiesMapper())
                )
                .successHandler(successHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "SESSION")
            )
            .csrf(csrf -> {
                CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                csrfRepository.setCookieCustomizer(cookie -> cookie
                    .sameSite("Lax")
                    // .secure(true) // Uncomment in production with HTTPS
                );
                
                csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
            });

        return http.build();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION"); 
        serializer.setUseHttpOnlyCookie(true); // Protects against XSS
        serializer.setSameSite("Lax"); // Protects against CSRF
        // serializer.setUseSecureCookie(true); // Uncomment in production with HTTPS
        return serializer;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Tùy biến bộ giải quyết yêu cầu ủy quyền OAuth2 (OAuth2AuthorizationRequestResolver).
     * 
     * Khi người dùng khởi tạo luồng đăng nhập (chuyển hướng sang Keycloak), Spring Security sẽ xây dựng
     * URL Authorization Request. Phương thức này cho phép trích xuất các tham số từ request của trình duyệt (FE)
     * và chuyển tiếp trực tiếp vào URL gửi sang Keycloak:
     * 
     * 1. Tham số "kc_idp_hint" (Keycloak Identity Provider Hint):
     *    - Ví dụ: /oauth2/authorization/keycloak?kc_idp_hint=ldap-enterprise (hoặc google, facebook, github)
     *    - Ý nghĩa: Hướng dẫn Keycloak bỏ qua màn hình đăng nhập mặc định và chuyển hướng thẳng đến nhà cung cấp
     *      danh tính cụ thể (Direct Social/Enterprise Login bypass).
     * 
     * 2. Tham số "prompt" (Chuẩn OpenID Connect Prompt Parameter):
     *    - Ví dụ: /oauth2/authorization/keycloak?prompt=login
     *    - Ý nghĩa: Ép buộc Keycloak phải yêu cầu người dùng nhập lại mật khẩu (Re-authentication) ngay cả khi
     *      phiên làm việc (SSO Session) vẫn còn hiệu lực, phục vụ cho các nghiệp vụ thanh toán hoặc bảo mật cao.
     *    - "prompt=consent": Ép buộc Keycloak hiển thị màn hình hỏi người dùng có đồng ý cấp quyền (Consent Screen).
     * 
     * @return Bộ resolver tùy biến đã được cấu hình các tham số mở rộng.
     */
    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        // Khởi tạo resolver mặc định với base URL bắt đầu luồng OAuth2 là "/oauth2/authorization"
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        
        // Thiết lập bộ tùy biến URL gửi sang Authorization Server (Keycloak)
        defaultResolver.setAuthorizationRequestCustomizer(customizer -> {
            // Lấy thông tin HttpServletRequest hiện tại từ luồng xử lý (ThreadLocal)
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                
                // Trích xuất tham số "kc_idp_hint" nếu có và gắn vào Authorization Request gửi Keycloak
                String idpHint = request.getParameter("kc_idp_hint");
                if (idpHint != null && !idpHint.isBlank()) {
                    customizer.additionalParameters(params -> params.put("kc_idp_hint", idpHint));
                }
                
                // Trích xuất tham số "prompt" nếu có và gắn vào Authorization Request gửi Keycloak
                String prompt = request.getParameter("prompt");
                if (prompt != null && !prompt.isBlank()) {
                    customizer.additionalParameters(params -> params.put("prompt", prompt));
                }
            }
        });
        
        return defaultResolver;
    }

    /**
     * Chuyển đổi và ánh xạ các vai trò (Roles) từ Keycloak thành GrantedAuthority của Spring Security.
     * 
     * TẠI SAO CẦN ĐOẠN XỬ LÝ IF-ELSE (OidcUserAuthority vs OAuth2UserAuthority)?
     * -----------------------------------------------------------------------------------------
     * Trong Spring Security OAuth2 / OIDC, thông tin danh tính của người dùng có thể đến từ 2 nguồn:
     * 
     * 1. Nhánh if (authority instanceof OidcUserAuthority):
     *    - Áp dụng khi luồng xác thực là OpenID Connect chuẩn (có scope "openid").
     *    - Keycloak trả về "ID Token" (chuỗi JWT).
     *    - Các Realm Roles thường được Keycloak đóng gói trực tiếp vào claim "realm_access.roles" của ID Token.
     *    - Ta trích xuất claim từ ID Token thông qua: oidcUserAuthority.getIdToken().getClaim("realm_access").
     * 
     * 2. Nhánh else if (authority instanceof OAuth2UserAuthority):
     *    - Áp dụng khi xác thực qua OAuth 2.0 thông thường (không có ID Token) HOẶC khi Spring Security gọi
     *      tiếp sang "UserInfo Endpoint" (/userinfo của Keycloak) để tải thêm thông tin thuộc tính mở rộng.
     *    - Dữ liệu trả về từ UserInfo endpoint được lưu trữ trong map attributes của OAuth2UserAuthority.
     *    - Ta trích xuất claim qua: oauth2UserAuthority.getAttributes().get("realm_access").
     * 
     * Ý NGHĨA THIẾT KẾ:
     * - Đảm bảo tính tương thích và linh hoạt (Resilience): Dù Keycloak cấu hình đính kèm roles ở ID Token
     *   hay ở UserInfo Endpoint, BFF đều giải mã và lấy được đầy đủ danh sách vai trò.
     * - Chuẩn hóa tiền tố: Chuyển đổi từ "CUSTOMER" thành "ROLE_CUSTOMER" để tương thích với các annotation
     *   bảo mật tiêu chuẩn của Spring như @PreAuthorize("hasRole('CUSTOMER')").
     */
    private GrantedAuthoritiesMapper keycloakGrantedAuthoritiesMapper() {
        return (Collection<? extends GrantedAuthority> authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                // Trường hợp 1: Trích xuất roles từ ID Token (chuẩn OpenID Connect)
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    Map<String, Object> realmAccess = oidcUserAuthority.getIdToken().getClaim("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                        mappedAuthorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(Collectors.toList()));
                    }
                // Trường hợp 2: Trích xuất roles từ UserInfo Endpoint attributes (OAuth2 / UserInfo)
                } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    Map<String, Object> realmAccess = (Map<String, Object>) oauth2UserAuthority.getAttributes().get("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                        mappedAuthorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(Collectors.toList()));
                    }
                }
                // Giữ lại authority gốc (chứa các thông tin mặc định như OIDC scope, sub...)
                mappedAuthorities.add(authority);
            });

            // Gán role mặc định ROLE_CUSTOMER nếu tài khoản từ các nhà cung cấp bên thứ 3 (Google/Social) chưa có role Keycloak
            boolean hasAnyRole = mappedAuthorities.stream()
                    .anyMatch(a -> a.getAuthority().startsWith("ROLE_"));
            if (!hasAnyRole) {
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            }

            return mappedAuthorities;
        };
    }

    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}");
        return successHandler;
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new SimpleUrlAuthenticationSuccessHandler(frontendUrl);
    }

    /**
     * Điểm chặn xử lý lỗi xác thực dành riêng cho ứng dụng Single Page Application (SPA như React/Vue/Next.js).
     * 
     * TẠI SAO CẦN CLASS NÀY? (Vấn đề CORS & AJAX trong kiến trúc SPA):
     * 1. Hành vi mặc định của Spring Security:
     *    Khi người dùng chưa đăng nhập gọi vào bất kỳ URL nào, Spring Security mặc định trả về HTTP 302 Redirect
     *    chuyển hướng sang trang đăng nhập Keycloak (/oauth2/authorization/keycloak).
     * 
     * 2. Lỗi phát sinh với SPA (React/Axios/Fetch):
     *    Khi Frontend gọi API ngầm qua AJAX (ví dụ: axios.get("/api/v1/orders")), nếu nhận mã 302 Redirect, trình duyệt
     *    sẽ tự động gửi AJAX request sang Keycloak. Điều này dẫn tới:
     *    - Lỗi CORS (Cross-Origin Resource Sharing Error) do Keycloak chặn request AJAX cross-origin.
     *    - Hoặc Axios nhận về mã HTML của trang login Keycloak thay vì lỗi JSON, khiến Frontend bị crash.
     * 
     * 3. Giải pháp của SpaAuthenticationEntryPoint:
     *    - Nếu là API request ("/api/**"): Trả về mã HTTP 401 Unauthorized để Axios Interceptor ở Frontend bắt lỗi
     *      và chủ động điều hướng người dùng sang luồng đăng nhập.
     *    - Nếu là truy cập trang trực tiếp từ thanh địa chỉ trình duyệt (Non-API): Chuyển hướng 302 sang Keycloak.
     */
    static class SpaAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
            if (request.getRequestURI().startsWith("/api/")) {
                // Trả về HTTP 401 thuần túy cho các cuộc gọi AJAX/Fetch từ Frontend
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                // Chuyển hướng 302 sang trang đăng nhập Keycloak khi người dùng truy cập trực tiếp từ trình duyệt
                response.sendRedirect("/oauth2/authorization/keycloak");
            }
        }
    }
}
