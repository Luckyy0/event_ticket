# Danh sách Cơ sở Dữ liệu

Tài liệu này phác thảo các bảng cơ sở dữ liệu chính được sử dụng trên các vi dịch vụ. Mỗi dịch vụ sở hữu lược đồ/cơ sở dữ liệu riêng.

| Dịch vụ | Bảng / Bộ sưu tập | Lưu trữ | Cột Khóa | Chỉ mục Khóa (Indexes) | Ràng buộc Khóa | Mục đích |
|---|---|---|---|---|---|---|
| **Catalog** | `events` (Col) | MongoDB | `_id`, `title`, `date`, `venue_id` | `venue_id`, `date` | — | Lưu trữ chi tiết sự kiện |
| **Catalog** | `venues` (Col) | MongoDB | `_id`, `name`, `location` | `name` | — | Lưu trữ thông tin địa điểm |
| **Catalog** | `ticket_types` (Col)| MongoDB | `_id`, `event_id`, `price` | `event_id` | — | Xác định các mức giá cho mỗi sự kiện |
| **Inventory** | `inventory_items` | PostgreSQL | `id`, `ticket_type_id`, `total_qty`, `available_qty` | `ticket_type_id` | `available_qty >= 0` | Nguồn sự thật cho số lượng vé |
| **Inventory** | `reservations` | PostgreSQL | `id`, `ticket_type_id`, `qty`, `status`, `expires_at` | `status`, `expires_at` | — | Theo dõi vé tạm giữ |
| **Inventory** | `outbox_events` | PostgreSQL | `id`, `aggregate_type`, `aggregate_id`, `payload` | `aggregate_id` | — | Hộp thư đi giao dịch (Outbox) cho Kafka |
| **Order** | `orders` | PostgreSQL | `id`, `user_id`, `total_amount`, `status` | `user_id`, `status` | — | Tổng hợp đơn hàng cốt lõi |
| **Order** | `order_items` | PostgreSQL | `id`, `order_id`, `ticket_type_id`, `qty` | `order_id` | FK tới `orders` | Các mục (vé) cho một đơn hàng |
| **Order** | `outbox_events` | PostgreSQL | `id`, `aggregate_type`, `aggregate_id`, `payload` | `aggregate_id` | — | Hộp thư đi giao dịch cho Kafka |
| **Payment** | `payments` | PostgreSQL | `id`, `order_id`, `amount`, `status`, `stripe_id` | `order_id`, `stripe_id` | Unique `stripe_id` | Theo dõi các ý định thanh toán |
| **Ticket** | `tickets` | PostgreSQL | `id`, `order_id`, `user_id`, `qr_code`, `status` | `order_id`, `user_id`, `qr_code` | Unique `qr_code` | Vé điện tử được phát hành |
| **User** | `user_profiles` | PostgreSQL | `id`, `keycloak_id`, `email`, `preferences` | `keycloak_id`, `email` | Unique `email` | Dữ liệu người dùng mở rộng |
| **Check-in** | `check_in_records` | PostgreSQL | `id`, `ticket_id`, `scanned_at`, `gate_id` | `ticket_id` | — | Nhật ký quét vé |
| **(Tất cả)** | `processed_events` | PostgreSQL | `event_id`, `processed_at` | `event_id` | PK `event_id` | Kho lưu trữ khóa tính lũy đẳng (Inbox pattern) |
