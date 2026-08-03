# Ranh giới và Trách nhiệm của các Dịch vụ

| Tên Dịch vụ | Tóm tắt Trách nhiệm | Các Sự kiện Xuất bản | Các Sự kiện Tiêu thụ | Lưu trữ Chính | API Mở ra (Tóm tắt) | Phụ thuộc |
|---|---|---|---|---|---|---|
| **Dịch vụ Danh mục (Catalog)** | Quản lý vòng đời của sự kiện, địa điểm, và loại vé. | `EventCreated`, `EventUpdated`, `EventCancelled`, `TicketTypeCreated` | — | MongoDB | CRUD cho Sự kiện, Địa điểm, Loại vé | — |
| **Dịch vụ Hàng tồn kho (Inventory)** | Quản lý tình trạng sẵn có của vé và thực hiện các đặt chỗ nguyên tử. | `InventoryReserved`, `InventoryReleased`, `InventoryConfirmed` | `OrderCreated`, `OrderCancelled`, `PaymentFailed` | Redis, PostgreSQL | Kiểm tra tình trạng, Đặt vé trước | — |
| **Dịch vụ Đơn hàng (Order)** | Quản lý đơn hàng của khách hàng, điều phối saga đặt vé. | `OrderCreated`, `OrderPaid`, `OrderCancelled`, `OrderRefunded` | `InventoryReserved`, `InventoryReleased`, `PaymentSucceeded`, `PaymentFailed` | PostgreSQL | Tạo Đơn hàng, Lấy Trạng thái, Hủy Đơn hàng | Inventory (Đồng bộ để kiểm tra trực tiếp, Bất đồng bộ cho saga) |
| **Dịch vụ Thanh toán (Payment)** | Xử lý thanh toán qua cổng bên ngoài và xử lý webhooks. | `PaymentSucceeded`, `PaymentFailed`, `PaymentRefunded` | `OrderCreated`, `OrderRefundRequested` | PostgreSQL | Tạo Ý định Thanh toán, Gọi lại Webhook | Stripe/Cổng Bên ngoài |
| **Dịch vụ Vé (Ticket)** | Phát hành vé điện tử (mã QR) sau khi thanh toán được xác nhận. | `TicketIssued`, `TicketCancelled` | `OrderPaid`, `OrderRefunded` | PostgreSQL | Lấy Vé theo Đơn/Người dùng, Tải PDF | — |
| **Dịch vụ Người dùng (User)** | Quản lý hồ sơ người dùng, sở thích, và vai trò. | `UserCreated`, `UserUpdated` | — | PostgreSQL | CRUD Hồ sơ Người dùng | Keycloak (Đồng bộ) |
| **Dịch vụ Thông báo (Notification)** | Gửi email giao dịch và SMS cho người dùng. | — | `OrderPaid`, `OrderCancelled`, `TicketIssued`, `EventCancelled` | — (Không trạng thái) | — | Nhà cung cấp Email (Đồng bộ) |
| **Dịch vụ Check-in** | Xác thực vé tại lối vào sự kiện và ngăn chặn vào cửa nhiều lần. | `TicketScanned`, `CheckInFailed` | `TicketIssued`, `TicketCancelled` | PostgreSQL | Quét Vé, Lấy Thống kê Check-in | Dịch vụ Vé (Đồng bộ dự phòng) |
| **Dịch vụ Tìm kiếm (Search)** | Cung cấp tìm kiếm toàn văn bản và lọc nhanh cho sự kiện. | — | `EventCreated`, `EventUpdated`, `EventCancelled` | Elasticsearch | Tìm kiếm Sự kiện, Lọc Sự kiện | — |
| **Dịch vụ Gợi ý (Recommendation)**| Cung cấp gợi ý sự kiện cá nhân hóa dựa trên lịch sử người dùng. | — | `OrderPaid`, `EventCreated` | PostgreSQL (hoặc Graph) | Lấy Gợi ý | — |
| **Dịch vụ Phân tích (Analytics)** | Tổng hợp các số liệu kinh doanh (doanh số, số người tham dự) để báo cáo. | — | Tất cả sự kiện domain chính | ClickHouse/Postgres | Lấy Bảng điều khiển, Xuất Báo cáo | — |
| **Dịch vụ Quản trị (Admin)** | Cung cấp các chức năng quản trị tập trung và cấu hình nền tảng. | `ConfigUpdated` | — | PostgreSQL | Cấm Người dùng, Ghi đè Cấu hình | Tất cả các dịch vụ (Đồng bộ) |
