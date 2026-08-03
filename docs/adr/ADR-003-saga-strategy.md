# ADR-003: Chiến lược Saga
## Trạng thái: Đã chấp nhận
## Bối cảnh: 
Quá trình đặt vé trải rộng trên nhiều dịch vụ: Hàng tồn kho (Đặt), Đơn hàng (Tạo), Thanh toán (Xử lý), Hàng tồn kho (Xác nhận), và Vé (Phát hành). Chúng ta cần một cơ chế để đảm bảo tính nhất quán dữ liệu qua các dịch vụ này mà không sử dụng các giao dịch phân tán (2PC), vốn khóa tài nguyên và giảm tính khả dụng.

## Quyết định: 
Chúng ta sẽ sử dụng **Saga dựa trên Choreography (Biên đạo)** cho luồng đặt vé.
- Các dịch vụ xuất bản các sự kiện domain lên Kafka (ví dụ: `InventoryReserved`, `OrderPaid`).
- Các dịch vụ khác lắng nghe các sự kiện này và phản ứng tương ứng.
- Không có dịch vụ điều phối (orchestrator) trung tâm nào ra lệnh.

## Hậu quả: 
- **Tích cực**: Các dịch vụ được tách rời cao độ. Không có điểm lỗi đơn lẻ (như một bộ điều phối). Phù hợp tự nhiên cho kiến trúc hướng sự kiện của chúng ta.
- **Tiêu cực**: Luồng tổng thể không được định nghĩa rõ ràng trong mã ở một nơi duy nhất; nó nảy sinh từ các tương tác. Việc gỡ lỗi một saga thất bại yêu cầu truy tìm các sự kiện qua nhiều dịch vụ. Nó có thể trở nên khó quản lý nếu số lượng các bước tăng lên đáng kể.

## Đánh đổi: 
Chúng ta đánh đổi sự kiểm soát tập trung và khả năng hiển thị của một Orchestrator (Điều phối viên) để lấy sự liên kết lỏng lẻo và khả năng phục hồi của Choreography.
- **Khả năng Quan sát (Observability)**: Chúng ta sẽ giảm thiểu việc thiếu khả năng hiển thị tập trung bằng cách triển khai theo dõi phân tán (ví dụ: OpenTelemetry) truyền một `trace_id` qua tất cả các tiêu đề Kafka, cho phép chúng ta hình dung việc thực thi saga trong các công cụ APM.
- **Xử lý Hết giờ (Timeout Handling)**: Các dịch vụ khởi tạo một bước (ví dụ: Hàng tồn kho giữ một vé trong 15 phút) tự chịu trách nhiệm về thời gian chờ của chính nó. Nếu một đặt chỗ hết hạn, Hàng tồn kho xuất bản `InventoryReleased`, hoạt động như một trình kích hoạt bù trừ cho Đơn hàng.
