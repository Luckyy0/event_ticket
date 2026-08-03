# Tổng quan Ngữ cảnh Hệ thống

Nền tảng bán vé áp dụng **kiến trúc vi dịch vụ hướng sự kiện (event-driven microservices)** để xử lý quy mô lớn và lưu lượng truy cập bùng nổ trong các đợt mở bán sự kiện phổ biến. Các vi dịch vụ cung cấp khả năng mở rộng và triển khai độc lập cho các bối cảnh cốt lõi như Hàng tồn kho (Inventory) và Đơn hàng (Order). Cách tiếp cận hướng sự kiện thông qua Kafka giúp tách rời các dịch vụ và cho phép xử lý nền bất đồng bộ cho các luồng không trọng yếu như thông báo, đồng thời ngăn chặn các kịch bản nguyên khối phân tán. Chúng tôi sử dụng **Kiến trúc Lục giác (Ports & Adapters)** cho các dịch vụ cốt lõi để tách biệt logic kinh doanh khỏi các khuôn khổ (framework) và cơ sở hạ tầng. Chiến lược lưu trữ đa nền tảng được sử dụng: **PostgreSQL** để đảm bảo tính nhất quán quan hệ chặt chẽ và tính chất ACID trong các lĩnh vực quan trọng (Đơn hàng, Thanh toán), **MongoDB** cho dữ liệu danh mục linh hoạt, đọc nhiều (Sự kiện, Địa điểm), và **Redis** cho các thao tác nguyên tử, cực kỳ nhanh chóng cần thiết để ngăn chặn việc bán quá mức (over-selling) trong quá trình đặt trước vé. Mẫu **BFF (Backend-for-Frontend)** được sử dụng cùng với API Gateway để tổng hợp các lệnh gọi và quản lý phiên an toàn cho trình duyệt, ủy quyền quản lý danh tính cho **Keycloak**. Tính **nhất quán cuối cùng (Eventual consistency)** được áp dụng qua ranh giới các dịch vụ thông qua tin nhắn bất đồng bộ, trong khi tính nhất quán mạnh mẽ được duy trì bên trong từng dịch vụ riêng lẻ. **Việc bán vé quá mức được ngăn chặn** bằng cách thực hiện các đặt chỗ nguyên tử bằng các tập lệnh Redis Lua như là bước đầu tiên của luồng đặt vé trước khi truyền tới PostgreSQL và các dịch vụ khác.

## Biểu đồ Ngữ cảnh Hệ thống

```mermaid
C4Context
    title Biểu đồ Ngữ cảnh Hệ thống cho Nền tảng Bán vé Sự kiện

    Person(customer, "Khách hàng", "Người dùng của nền tảng muốn mua vé tham dự sự kiện.")
    Person(organizer, "Nhà tổ chức sự kiện", "Tạo sự kiện, thiết lập loại vé và quản lý địa điểm.")
    Person(admin, "Quản trị viên nền tảng", "Quản lý nền tảng, người dùng và xử lý hỗ trợ.")

    System_Boundary(platform_boundary, "Nền tảng Bán vé Sự kiện") {
        System(platform, "Hệ thống Bán vé", "Cho phép khách hàng duyệt các sự kiện và mua vé. Cho phép nhà tổ chức quản lý sự kiện của họ.")
    }

    System_Ext(keycloak, "Keycloak (IAM)", "Nhà cung cấp quản lý Truy cập và Danh tính.")
    System_Ext(payment_gateway, "Cổng Thanh toán", "Nhà cung cấp thanh toán bên ngoài (ví dụ: Stripe, PayPal).")
    System_Ext(email_provider, "Nhà cung cấp Email", "Dịch vụ gửi email bên ngoài (ví dụ: SendGrid).")

    Rel(customer, platform, "Duyệt sự kiện, mua vé", "HTTPS")
    Rel(organizer, platform, "Quản lý sự kiện và địa điểm", "HTTPS")
    Rel(admin, platform, "Quản lý nền tảng", "HTTPS")

    Rel(platform, keycloak, "Xác thực người dùng, xác thực token", "HTTPS")
    Rel(platform, payment_gateway, "Xử lý thanh toán, hoàn tiền", "HTTPS")
    Rel(platform, email_provider, "Gửi email giao dịch", "SMTP/HTTPS")
```
