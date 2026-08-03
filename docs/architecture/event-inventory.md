# Danh sách Sự kiện

Tài liệu này liệt kê tất cả các sự kiện domain được xuất bản lên Kafka để giao tiếp bất đồng bộ giữa các dịch vụ.

| Loại Sự kiện | Phiên bản | Người xuất bản | Người tiêu thụ | Chủ đề Kafka (Topic) | Khóa (Key) | Mô tả |
|---|---|---|---|---|---|---|
| `EventCreated` | v1 | Catalog | Search, Recommendation | `catalog.events` | EventID | Được xuất bản khi một sự kiện mới được tạo |
| `EventUpdated` | v1 | Catalog | Search, Recommendation | `catalog.events` | EventID | Được xuất bản khi chi tiết sự kiện thay đổi |
| `EventCancelled` | v1 | Catalog | Search, Order, Notif | `catalog.events` | EventID | Được xuất bản khi một sự kiện bị hủy |
| `InventoryReserved` | v1 | Inventory | Order | `inventory.reservations` | ReservationID | Được xuất bản khi vé được giữ thành công |
| `InventoryReleased` | v1 | Inventory | Analytics | `inventory.reservations` | ReservationID | Được xuất bản khi đặt chỗ hết hạn hoặc bị hủy |
| `InventoryConfirmed`| v1 | Inventory | Analytics | `inventory.reservations` | ReservationID | Được xuất bản khi vé bị trừ vĩnh viễn |
| `OrderCreated` | v1 | Order | Payment, Inventory | `order.orders` | OrderID | Được xuất bản khi một đơn hàng mới đang chờ thanh toán |
| `OrderPaid` | v1 | Order | Inventory, Ticket, Notif | `order.orders` | OrderID | Được xuất bản khi một đơn hàng được thanh toán thành công |
| `OrderCancelled` | v1 | Order | Inventory, Notif | `order.orders` | OrderID | Được xuất bản khi một đơn hàng hết giờ hoặc thanh toán thất bại |
| `OrderRefunded` | v1 | Order | Inventory, Ticket | `order.orders` | OrderID | Được xuất bản khi một khoản hoàn tiền được xử lý |
| `PaymentIntentCreated`| v1 | Payment | Analytics | `payment.payments` | PaymentID | Được xuất bản khi ý định thanh toán được tạo với cổng |
| `PaymentSucceeded` | v1 | Payment | Order | `payment.payments` | PaymentID | Được xuất bản khi cổng xác nhận thanh toán |
| `PaymentFailed` | v1 | Payment | Order | `payment.payments` | PaymentID | Được xuất bản khi cổng báo thanh toán thất bại |
| `TicketIssued` | v1 | Ticket | Check-in, Notif | `ticket.tickets` | TicketID | Được xuất bản khi một mã QR/PDF của vé được tạo ra |
| `TicketCancelled` | v1 | Ticket | Check-in | `ticket.tickets` | TicketID | Được xuất bản khi một vé bị vô hiệu hóa |
| `TicketScanned` | v1 | Check-in | Analytics | `checkin.scans` | TicketID | Được xuất bản khi một vé được sử dụng tại địa điểm |
