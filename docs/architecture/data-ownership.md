# Ma trận Quyền sở hữu Dữ liệu

| Thực thể Dữ liệu | Dịch vụ Sở hữu | Lưu trữ | Nguồn Sự thật | Đọc bởi (các dịch vụ khác) |
|---|---|---|---|---|
| Sự kiện (Event) | Dịch vụ Catalog | MongoDB | Dịch vụ Catalog | Tìm kiếm, Gợi ý, Phân tích, BFF, Đơn hàng |
| Địa điểm (Venue) | Dịch vụ Catalog | MongoDB | Dịch vụ Catalog | Tìm kiếm, BFF |
| Loại vé (TicketType) | Dịch vụ Catalog | MongoDB | Dịch vụ Catalog | Hàng tồn kho, Đơn hàng, BFF |
| Buổi diễn (Show) | Dịch vụ Catalog | MongoDB | Dịch vụ Catalog | Hàng tồn kho, Đơn hàng, BFF |
| Hàng tồn kho (Inventory) | Dịch vụ Inventory | PostgreSQL / Redis | Dịch vụ Inventory | Đơn hàng, Catalog, BFF (Kiểm tra số lượng) |
| Đặt chỗ (Reservation) | Dịch vụ Inventory | PostgreSQL / Redis | Dịch vụ Inventory | Đơn hàng (qua Saga) |
| Đơn hàng (Order) | Dịch vụ Order | PostgreSQL | Dịch vụ Order | Thanh toán, Vé, Phân tích, BFF |
| Thanh toán (Payment) | Dịch vụ Payment | PostgreSQL | Dịch vụ Payment | Đơn hàng (qua Saga) |
| Vé (Ticket) | Dịch vụ Ticket | PostgreSQL | Dịch vụ Ticket | Check-in, BFF, Người dùng |
| Hồ sơ Ng.dùng (UserProfile) | Dịch vụ User | PostgreSQL | Dịch vụ User | BFF, Đơn hàng, Vé |
| Nhật ký KS (AuditLog) | Dịch vụ Admin | PostgreSQL | Dịch vụ Admin | — |
| Sự kiện Hộp thư đi (OutboxEvent) | (Mỗi Dịch vụ) | PostgreSQL | Dịch vụ Tương ứng | Kafka Connector |
