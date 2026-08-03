# Cấu trúc Thư mục Dự án

Giả định bố cục **Monorepo** như đã quyết định trong ADR-001.

```text
event_ticket/
├── apps/                               # Các ứng dụng Frontend
│   ├── web-app/                        # Cổng thông tin người tiêu dùng Next.js/React
│   ├── admin-panel/                    # Bảng điều khiển quản trị React
│   └── bff/                            # Backend-for-Frontend Node.js/Go
├── services/                           # Các vi dịch vụ Backend
│   ├── api-gateway/                    # Spring Cloud Gateway
│   ├── catalog-service/                # Java/Spring Boot
│   ├── inventory-service/              # Java/Spring Boot
│   ├── order-service/                  # Java/Spring Boot
│   ├── payment-service/                # Java/Spring Boot
│   ├── ticket-service/                 # Java/Spring Boot
│   ├── user-service/                   # Java/Spring Boot
│   ├── notification-service/           # Java/Spring Boot
│   ├── checkin-service/                # Java/Spring Boot
│   ├── search-service/                 # Java/Spring Boot
│   ├── recommendation-service/         # Python
│   └── analytics-service/              # Java/Spring Boot
├── libs/                               # Các thư viện chia sẻ (nội bộ)
│   ├── shared-dto/                     # Đối tượng Truyền Dữ liệu (DTO) chung
│   ├── shared-events/                  # Lược đồ Sự kiện Kafka (Avro/JSON)
│   ├── shared-auth/                    # Tiện ích phân tích JWT
│   └── shared-observability/           # Thiết lập Theo dõi/Số liệu (Tracing/Metrics)
├── infrastructure/                     # Cơ sở hạ tầng dưới dạng Mã & Cấu hình
│   ├── docker-compose.yml              # Môi trường phát triển cục bộ
│   ├── k8s/                            # Tệp kê khai Kubernetes (Manifests)
│   ├── terraform/                      # Cung cấp đám mây (Cloud provisioning)
│   └── keycloak/                       # Tệp xuất realm của Keycloak
├── docs/                               # Tài liệu
│   ├── architecture/                   # Biểu đồ kiến trúc và luồng (C4, UML)
│   │   └── uml/                        # Các tệp nguồn UML/Mermaid/PlantUML
│   ├── adr/                            # Hồ sơ Quyết định Kiến trúc (ADR)
│   └── api/                            # Thông số OpenAPI/Swagger
├── tools/                              # Tập lệnh xây dựng và triển khai
├── .github/                            # Các luồng công việc (workflows) GitHub Actions
├── package.json                        # Cấu hình gói gốc Monorepo (vd: Nx/Lerna)
└── README.md                           # Tài liệu gốc của dự án
```
