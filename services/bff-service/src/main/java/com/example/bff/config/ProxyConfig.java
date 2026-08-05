package com.example.bff.config;

import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * Cấu hình Reverse Proxy chuyển tiếp yêu cầu từ BFF sang API Gateway (Spring Cloud Gateway Server MVC).
 * 
 * Lớp cấu hình này đảm nhận các nhiệm vụ cốt lõi trong mô hình Backend-For-Frontend (BFF):
 * 1. Chuyển tiếp (Proxy) các request nghiệp vụ từ trình duyệt (FE) sang API Gateway phía sau.
 * 2. Đính kèm Access Token (Token Relay) từ Session của người dùng vào HTTP Authorization Header.
 * 3. Sinh mã Correlation ID (X-Correlation-ID) phục vụ truy vết phân tán (Distributed Tracing).
 */
@Configuration
public class ProxyConfig {

    /**
     * Địa chỉ URL của API Gateway nội bộ (mặc định trỏ tới http://localhost:8081 nếu không có biến môi trường).
     */
    @Value("${API_GATEWAY_URL:http://localhost:8081}")
    private String apiGatewayUrl;

    /**
     * Khai báo RouterFunction định tuyến các yêu cầu HTTP.
     *
     * @return RouterFunction chuyển tiếp request kèm theo bộ lọc Token Relay và Header bổ sung.
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayRoute() {
        return route("api-gateway")
                // Điều kiện định tuyến:
                // - Khớp tất cả các đường dẫn bắt đầu bằng "/api/v1/" (các API nghiệp vụ của hệ thống).
                // - Loại trừ "/api/v1/admin/users" vì API quản trị người dùng này được xử lý trực tiếp tại BFF (gọi Keycloak Admin API).
                .route(request -> request.path().startsWith("/api/v1/") && !request.path().startsWith("/api/v1/admin/users"),
                        http(apiGatewayUrl))

                // Bộ lọc Token Relay (Cực kỳ quan trọng trong mô hình BFF):
                // - Trình duyệt chỉ gửi Cookie (Session ID) an toàn về BFF (tránh lộ Access Token ở Frontend).
                // - Token Relay sẽ tự động trích xuất OAuth2 Access Token từ Session trong Redis/Memory.
                // - Tự động làm mới (Refresh Token) nếu token hết hạn qua OAuth2AuthorizedClientManager.
                // - Gắn token vào header "Authorization: Bearer <access_token>" trước khi chuyển tiếp sang API Gateway.
                .filter(TokenRelayFilterFunctions.tokenRelay())

                // Bộ lọc bổ sung Header truy vết phân tán (Distributed Tracing):
                // - Mỗi request khi đi qua BFF sẽ được sinh một mã UUID duy nhất gắn vào header "X-Correlation-ID".
                // - Giúp log của toàn bộ chuỗi microservices (Gateway, Catalog, Inventory, Order) có chung một ID để debug.
                .before(addRequestHeader("X-Correlation-ID", UUID.randomUUID().toString()))

                // Xây dựng và hoàn tất chuỗi cấu hình route
                .build();
    }
}

