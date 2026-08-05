-- =============================================================================
-- KHỞI TẠO CƠ SỞ DỮ LIỆU BAN ĐẦU CHO HỆ THỐNG TICKETING VÀ KEYCLOAK (PG-INIT.SQL)
-- =============================================================================
-- Script này tự động được PostgreSQL Docker Container thực thi 1 lần duy nhất 
-- khi container khởi tạo vùng dữ liệu (volume) lần đầu tiên.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. CẤU HÌNH CƠ SỞ DỮ LIỆU VÀ TÀI KHOẢN CHO KEYCLOAK IAM
-- -----------------------------------------------------------------------------
-- [Dòng 1]: Tạo một User (Role) riêng biệt trong PostgreSQL có tên là 'keycloak' với mật khẩu là 'password'.
-- Mục đích: Đảm bảo nguyên tắc bảo mật tối thiểu (Least Privilege). Keycloak sẽ dùng tài khoản này
-- để kết nối vào database thay vì dùng tài khoản superuser chung của hệ thống.
CREATE USER keycloak WITH PASSWORD 'password';

-- [Dòng 2]: Tạo một Database độc lập có tên là 'keycloak' và chỉ định chủ sở hữu (Owner) là user 'keycloak'.
-- Mục đích: Tách biệt hoàn toàn hàng trăm bảng dữ liệu quản trị danh tính (users, realms, tokens, roles) 
-- của Keycloak ra khỏi dữ liệu nghiệp vụ của các microservices (vé, đơn hàng, thanh toán).
CREATE DATABASE keycloak OWNER keycloak;

-- [Dòng 3]: Trao toàn bộ quyền quản trị (CREATE, CONNECT, TEMPORARY, SELECT, INSERT, UPDATE, DELETE...) 
-- trên Database 'keycloak' cho User 'keycloak'.
-- Mục đích: Cho phép Keycloak tự động thực thi các script Liquibase khởi tạo schema, tạo bảng, 
-- và nâng cấp phiên bản dữ liệu nội bộ của chính nó khi Keycloak Server khởi động.
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;


-- -----------------------------------------------------------------------------
-- 2. CẤU HÌNH CÁC CƠ SỞ DỮ LIỆU ĐỘC LẬP CHO TỪNG MICROSERVICE (DATABASE-PER-SERVICE)
-- -----------------------------------------------------------------------------
-- Tạo Database riêng cho Order Service (quản lý đơn hàng, giỏ hàng, saga state)
CREATE DATABASE order_service_db;

-- Tạo Database riêng cho Payment Service (quản lý giao dịch thanh toán, lịch sử hoàn tiền)
CREATE DATABASE payment_service_db;

-- Tạo Database riêng cho Ticket Service (quản lý mã vé QR, trạng thái phát hành vé)
CREATE DATABASE ticket_service_db;

-- Tạo Database riêng cho Inventory Service (quản lý tồn kho vé theo thời gian thực)
CREATE DATABASE inventory_service_db;

-- Phân quyền truy cập toàn bộ các Database nghiệp vụ trên cho tài khoản ứng dụng "user"
GRANT ALL PRIVILEGES ON DATABASE order_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE ticket_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE inventory_service_db TO "user";
