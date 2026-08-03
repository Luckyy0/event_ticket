# ADR-002: Chiến lược Nhất quán Hàng tồn kho
## Trạng thái: Đã chấp nhận
## Bối cảnh: 
Ngăn chặn bán quá mức (over-selling - bán nhiều vé hơn sức chứa) là thách thức kỹ thuật quan trọng nhất trong việc bán vé sự kiện. Các sự kiện có nhu cầu cao có thể thấy hàng ngàn yêu cầu mỗi giây. Một cơ sở dữ liệu quan hệ truyền thống (PostgreSQL) có thể bị nghẽn khóa (lock contention) nếu được sử dụng trực tiếp để trừ vé nguyên tử ở quy mô này. Chúng ta cần một chiến lược đảm bảo không bán quá mức trong khi vẫn duy trì thông lượng cao.

## Quyết định: 
Chúng ta sẽ sử dụng cách tiếp cận kết hợp: **Redis** cho việc đặt chỗ nguyên tử ở luồng nhanh (fast-path) và **PostgreSQL** là nguồn sự thật bền vững.
1. **Luồng Ghi (Write Flow)**: Các yêu cầu đặt chỗ đến Dịch vụ Hàng tồn kho (Inventory). Nó thực thi một tập lệnh Lua duy nhất trong Redis để kiểm tra tính khả dụng và giảm bộ đếm một cách nguyên tử.
2. Nếu thành công trong Redis, dịch vụ lưu chi tiết đặt chỗ vào PostgreSQL và ghi một sự kiện `InventoryReserved` vào bảng Hộp thư đi (Outbox) trong cùng một giao dịch.
3. **Nguồn Sự thật**: PostgreSQL là nguồn sự thật cuối cùng. Redis được coi là một bộ nhớ đệm nhất quán cao cho *sức chứa hiện có*.

## Hậu quả: 
- **Tích cực**: Thông lượng cực cao và độ trễ thấp cho các yêu cầu đặt chỗ. Không có nghẽn khóa cơ sở dữ liệu trong các đợt tăng đột biến lưu lượng. Đảm bảo tuyệt đối chống bán quá mức nhờ việc thực thi tập lệnh Lua đơn luồng trong Redis.
- **Tiêu cực**: Tăng độ phức tạp kiến trúc. Yêu cầu xử lý cẩn thận các trường hợp biên nơi Redis và PostgreSQL bị lệch dữ liệu.

## Đánh đổi: 
Chúng ta đánh đổi sự đơn giản trong vận hành (một DB duy nhất) để lấy hiệu suất và kiểm soát đồng thời cần thiết trong các sự kiện có nhu cầu cao. Chúng ta phải triển khai các công việc đối chiếu (reconciliation jobs) để đảm bảo sức chứa của Redis phù hợp với sức chứa được tính toán của PostgreSQL trong trường hợp các nút Redis gặp sự cố.
