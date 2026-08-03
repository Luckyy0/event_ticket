# Danh sách API

Tài liệu này cung cấp danh sách tóm tắt các API REST trên tất cả các dịch vụ.

| Dịch vụ | Phương thức | Đường dẫn | Xác thực | Mô tả |
|---|---|---|---|---|
| **BFF** | POST | `/login` | Không | Bắt đầu luồng đăng nhập OIDC với Keycloak |
| **BFF** | GET | `/callback` | Không | Xử lý callback OIDC, đổi mã lấy token |
| **BFF** | POST | `/logout` | Phiên | Kết thúc phiên người dùng |
| **BFF** | POST | `/refresh` | Phiên | Làm mới access token |
| **Catalog** | GET | `/api/v1/events` | Không | Danh sách và tìm kiếm các sự kiện sắp tới |
| **Catalog** | GET | `/api/v1/events/{id}` | Không | Lấy chi tiết sự kiện cụ thể |
| **Catalog** | POST | `/api/v1/events` | Admin/Org | Tạo một sự kiện mới |
| **Catalog** | POST | `/api/v1/events/{id}/ticket-types` | Admin/Org | Tạo các loại vé cho một sự kiện |
| **Inventory** | GET | `/api/v1/inventory/events/{id}` | Không | Lấy số lượng vé hiện có |
| **Inventory** | POST | `/api/v1/inventory/reservations` | Người dùng | Đặt vé trước (nguyên tử) |
| **Order** | POST | `/api/v1/orders` | Người dùng | Tạo đơn hàng mới (Nội bộ/BFF thường qua Kafka, nhưng API đồng bộ vẫn tồn tại) |
| **Order** | GET | `/api/v1/orders/{id}` | Người dùng | Lấy trạng thái và chi tiết đơn hàng |
| **Order** | GET | `/api/v1/users/{userId}/orders` | Người dùng | Danh sách đơn hàng của người dùng |
| **Order** | POST | `/api/v1/orders/{id}/cancel` | User/Admin | Hủy một đơn hàng chưa thanh toán |
| **Payment** | POST | `/api/v1/payments/intent` | Người dùng | Tạo ý định thanh toán (trả về Client Secret) |
| **Payment** | POST | `/api/v1/payments/webhook` | Không | Nơi nhận Webhook Stripe (xác minh chữ ký) |
| **Ticket** | GET | `/api/v1/users/{userId}/tickets` | Người dùng | Danh sách vé hợp lệ của người dùng |
| **Ticket** | GET | `/api/v1/tickets/{id}/download` | Người dùng | Tải vé dưới dạng PDF |
| **Check-in** | POST | `/api/v1/checkin/scan` | Nhân viên | Quét và xác thực mã QR của vé |
| **Admin** | GET | `/api/v1/admin/dashboard` | Admin | Lấy số liệu nền tảng |
| **Admin** | POST | `/api/v1/admin/users/{id}/ban` | Admin | Cấm tài khoản người dùng |
