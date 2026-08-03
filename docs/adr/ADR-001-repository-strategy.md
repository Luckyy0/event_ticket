# ADR-001: Chiến lược Kho lưu trữ (Repository)
## Trạng thái: Đã chấp nhận
## Bối cảnh: 
Chúng ta đang xây dựng một kiến trúc vi dịch vụ hướng sự kiện với 12 dịch vụ riêng biệt, một API Gateway, và một BFF. Chúng ta cần quyết định cách tổ chức mã nguồn cho các dịch vụ này, cấu hình cơ sở hạ tầng, và các thư viện chia sẻ. Hai lựa chọn chính là Polyrepo (mỗi dịch vụ một kho lưu trữ) hoặc Monorepo (tất cả các dịch vụ trong một kho lưu trữ duy nhất). Người dùng đã yêu cầu rõ ràng việc sử dụng Monorepo.

## Quyết định: 
Chúng ta sẽ sử dụng chiến lược **Monorepo** cho toàn bộ dự án nền tảng bán vé. Tất cả các vi dịch vụ, ứng dụng frontend, cấu hình cơ sở hạ tầng (như Docker Compose), và các thư viện chia sẻ sẽ nằm trong một kho lưu trữ Git duy nhất.

## Hậu quả: 
- **Tích cực**: Đơn giản hóa việc quản lý phụ thuộc giữa các dịch vụ. Dễ dàng chia sẻ các sự kiện domain, DTO, và thư viện tiện ích hơn. Việc tái cấu trúc qua nhiều dịch vụ hoặc cập nhật một phụ thuộc dùng chung có thể được thực hiện trong một pull request duy nhất. Đơn giản hóa việc giới thiệu cho các nhà phát triển mới vì họ chỉ cần sao chép một kho lưu trữ. Góc nhìn thống nhất về toàn bộ hệ thống cho các luồng CI/CD.
- **Tiêu cực**: Kho lưu trữ có thể trở nên rất lớn theo thời gian, có khả năng làm chậm các thao tác git. Các luồng CI phải được cấu hình cẩn thận để chỉ xây dựng và kiểm tra các dịch vụ bị thay đổi (ví dụ: sử dụng các công cụ như Nx, Bazel, hoặc thiết lập đa mô-đun Gradle/Maven) để tránh thời gian xây dựng lâu. Tăng rủi ro kết nối chặt chẽ các dịch vụ nếu ranh giới không được thực thi nghiêm ngặt bằng công cụ.

## Đánh đổi: 
Chúng ta đánh đổi sự cô lập nghiêm ngặt và phiên bản hóa độc lập của thiết lập đa kho lưu trữ (multi-repo) để lấy sự tiện lợi, khả năng khám phá, và dễ dàng tái cấu trúc của một monorepo. Chúng ta phải đầu tư sớm vào CI/CD mạnh mẽ và công cụ xây dựng để quản lý quy mô của monorepo một cách hiệu quả.
