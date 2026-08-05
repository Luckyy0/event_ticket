# KEYCLOAK REALM CONFIGURATION GUIDE (REALM-DEV.JSON)

Tài liệu hướng dẫn chi tiết từng khối cấu hình trong file `realm-dev.json` của Keycloak IAM.

---

## 1. Cấu hình Realm Tổng quan (Realm General Settings)
- **`realm` (`event-ticketing`)**: Tên định danh của Realm. Tất cả các token, client, role và user đều thuộc không gian của Realm này.
- **`enabled` (`true`)**: Kích hoạt Realm hoạt động.

## 2. Đăng ký & Xác thực Email (User Registration & Email Verification)
- **`registrationAllowed` (`true`)**: Cho phép người dùng tự đăng ký tài khoản trên giao diện Web Keycloak.
- **`registrationEmailAsUsername` (`false`)**: Người dùng nhập `username` và `email` riêng biệt.
- **`verifyEmail` (`true`)**: Bắt buộc xác thực địa chỉ email qua link gửi từ hệ thống trước khi tài khoản được sử dụng.
- **`resetPasswordAllowed` (`true`)**: Cho phép tự khôi phục / đặt lại mật khẩu qua email.
- **`duplicateEmailsAllowed` (`false`)**: Không cho phép trùng email giữa các tài khoản khác nhau trong cùng Realm.

## 3. Cấu hình Máy chủ Gửi Email (SMTP Server)
- **`host` (`mailpit`) / `port` (`1025`)**: Kết nối đến mock SMTP server Mailpit trong Docker network để gửi email xác thực trong môi trường local dev.

## 4. Thời hạn Phiên & Token (Session & Token Lifespans)
- **`accessTokenLifespan` (`300`)**: Access Token có hạn 5 phút (300s), tăng tính bảo mật cho microservices.
- **`ssoSessionIdleTimeout` (`1800`)**: Phiên SSO hết hạn sau 30 phút không hoạt động.
- **`revokeRefreshToken` (`true`) & `refreshTokenMaxReuse` (`0`)**: Áp dụng Refresh Token Rotation (thu hồi token cũ sau khi refresh) nhằm chống lại tấn công Replay Attack.

## 5. Tích hợp Đăng nhập Google (Google Identity Provider)
- **`identityProviders` (`alias: google`)**:
  - `providerId`: `"google"`.
  - `trustEmail`: `true` (tin cậy email đã được Google xác thực).
  - `firstBrokerLoginFlowAlias`: `"first broker login"` (tự động liên kết tài khoản nếu email đã tồn tại).
- **`identityProviderMappers`**:
  - Tự động map các trường `email`, `given_name`, `family_name` từ Google Profile sang tài khoản Keycloak.
  - Gán mặc định Realm Role `CUSTOMER` cho tài khoản đăng nhập qua Google.

## 6. Phân quyền Vai trò (Realm Roles)
- **`ADMIN`**: Quản trị viên toàn hệ thống.
- **`CUSTOMER`**: Khách hàng mua vé thông thường.
- **`STAFF`**: Nhân viên soát vé và vận hành sự kiện.
- **`ORGANIZER`**: Nhà tổ chức sự kiện.

## 7. Đồng bộ Người dùng Doanh nghiệp (OpenLDAP Federation)
- **`providerId` (`ldap`)**:
  - `importEnabled`: `true` (nhập và lưu người dùng vào Postgres DB cục bộ của Keycloak để tối ưu tốc độ đọc).
  - `changedSyncPeriod`: `300` (đồng bộ delta 5 phút / lần cho các tài khoản mới hoặc có chỉnh sửa).
  - `fullSyncPeriod`: `86400` (đối soát toàn bộ danh bạ 24 giờ / lần để dọn dẹp các tài khoản đã bị xóa trên LDAP).

## 8. Cấu hình OAuth2 Clients (Clients)
- **`bff-client` (Backend-For-Frontend)**:
  - Confidential Client có client-secret (`secret`).
  - Standard Flow (Authorization Code Flow) + PKCE (`S256`).
  - Chứa `api-client-audience` mapper để gắn `"aud": "api-client"` vào Access Token.
- **`api-client` (Resource Server)**:
  - `bearerOnly`: `true` (đại diện cho toàn bộ cụm backend microservices nhận Bearer Token).


## 9
```text
{
  // =========================================================================
  // 1. CẤU HÌNH REALM TỔNG QUAN (REALM GENERAL SETTINGS)
  // =========================================================================
  // Tên định danh của Realm trong hệ thống Keycloak
  "realm": "event-ticketing",
  // Trạng thái kích hoạt Realm (true: đang hoạt động)
  "enabled": true,

  // =========================================================================
  // 2. CẤU HÌNH ĐĂNG KÝ VÀ XÁC THỰC EMAIL (USER REGISTRATION & EMAIL VERIFICATION)
  // =========================================================================
  // Cho phép người dùng tự đăng ký tài khoản mới trên giao diện Keycloak
  "registrationAllowed": true,
  // false: Người dùng nhập username và email riêng biệt (không bắt buộc lấy email làm username)
  "registrationEmailAsUsername": false,
  // Cho phép người dùng tự khôi phục / đặt lại mật khẩu qua email
  "resetPasswordAllowed": true,
  // Cho phép ghi nhớ trạng thái đăng nhập (Remember Me)
  "rememberMe": true,
  // Bắt buộc xác thực địa chỉ email khi tạo mới hoặc cập nhật tài khoản
  "verifyEmail": true,
  // Cho phép người dùng đăng nhập bằng cả username hoặc địa chỉ email
  "loginWithEmailAllowed": true,
  // Không cho phép trùng lặp email giữa các tài khoản khác nhau trong cùng Realm
  "duplicateEmailsAllowed": false,
  // Không cho phép người dùng thay đổi username sau khi đã tạo
  "editUsernameAllowed": false,

  // =========================================================================
  // 3. CẤU HÌNH MÁY CHỦ GỬI EMAIL (SMTP SERVER CONFIGURATION)
  // Phục vụ việc gửi email xác thực tài khoản, đổi mật khẩu (kết nối Mailpit trong local dev)
  // =========================================================================
  "smtpServer": {
    "host": "mailpit",
    "port": "1025",
    "from": "noreply@event-ticketing.local",
    "fromDisplayName": "Event Ticketing System",
    "ssl": "false",
    "starttls": "false",
    "auth": "false"
  },

  // =========================================================================
  // 4. CẤU HÌNH THỜI HẠN VÀ PHIÊN LÀM VIỆC (SESSION & TOKEN LIFESPANS)
  // =========================================================================
  // Thời gian sống của Access Token (300 giây = 5 phút)
  "accessTokenLifespan": 300,
  // Thời gian hết hạn của phiên SSO khi người dùng không hoạt động (1800 giây = 30 phút)
  "ssoSessionIdleTimeout": 1800,
  // Thời gian tối đa của một phiên SSO (36000 giây = 10 giờ)
  "ssoSessionMaxLifespan": 36000,
  // Thu hồi Refresh Token cũ sau khi cấp Refresh Token mới (chống Replay Attack)
  "revokeRefreshToken": true,
  // Số lần tối đa Refresh Token có thể được tái sử dụng (0: không cho tái sử dụng)
  "refreshTokenMaxReuse": 0,

  // =========================================================================
  // 5. ĐA NGÔN NGỮ (INTERNATIONALIZATION)
  // =========================================================================
  // Bật tính năng đa ngôn ngữ cho các trang đăng nhập/đăng ký của Keycloak
  "internationalizationEnabled": true,
  // Danh sách các ngôn ngữ được hỗ trợ (Tiếng Việt & Tiếng Anh)
  "supportedLocales": [
    "vi",
    "en"
  ],
  // Ngôn ngữ mặc định hiển thị cho người dùng
  "defaultLocale": "vi",

  // =========================================================================
  // 6. CÁC VAI TRÒ VÀ QUYỀN TRUY CẬP TRONG REALM (REALM ROLES)
  // =========================================================================
  // Vai trò mặc định gán cho mọi người dùng mới đăng ký
  "defaultRoles": [
    "CUSTOMER"
  ],
  "roles": {
    "realm": [
      {
        "name": "CUSTOMER",
        "description": "Khách hàng mua vé xem sự kiện (mặc định cho tài khoản tự đăng ký)"
      },
      {
        "name": "EVENT_ORGANIZER",
        "description": "Nhà tổ chức sự kiện, có quyền tạo và quản lý sự kiện"
      },
      {
        "name": "STAFF",
        "description": "Nhân viên soát vé và check-in tại sự kiện"
      },
      {
        "name": "ADMIN",
        "description": "Quản trị viên toàn hệ thống"
      },
      {
        "name": "SUPPORT",
        "description": "Nhân viên hỗ trợ khách hàng"
      }
    ]
  },

  // =========================================================================
  // 7. CLIENT SCOPES PHÂN QUYỀN CHO MICROSERVICES (OAUTH2 SCOPES)
  // =========================================================================
  "clientScopes": [
    {
      "name": "event:read",
      "protocol": "openid-connect"
    },
    {
      "name": "event:write",
      "protocol": "openid-connect"
    },
    {
      "name": "inventory:read",
      "protocol": "openid-connect"
    },
    {
      "name": "inventory:manage",
      "protocol": "openid-connect"
    },
    {
      "name": "order:read",
      "protocol": "openid-connect"
    },
    {
      "name": "order:create",
      "protocol": "openid-connect"
    },
    {
      "name": "order:cancel",
      "protocol": "openid-connect"
    },
    {
      "name": "payment:create",
      "protocol": "openid-connect"
    },
    {
      "name": "payment:refund",
      "protocol": "openid-connect"
    },
    {
      "name": "ticket:read",
      "protocol": "openid-connect"
    },
    {
      "name": "ticket:checkin",
      "protocol": "openid-connect"
    }
  ],

  // =========================================================================
  // 8. TÍCH HỢP NGUỒN NGƯỜI DÙNG DOANH NGHIỆP QUA LDAP (LDAP FEDERATION)
  // Phục vụ đồng bộ nhân viên nội bộ (Staff, Admin) từ OpenLDAP
  // =========================================================================
  "components": {
    "org.keycloak.storage.UserStorageProvider": [
      {
        "id": "ldap-enterprise-provider",
        "name": "ldap-enterprise",
        "providerId": "ldap",
        "subComponents": {
          "org.keycloak.storage.ldap.mappers.LDAPStorageMapper": [
            // Ánh xạ thuộc tính username giữa Keycloak và LDAP (uid)
            {
              "id": "username-mapper",
              "name": "username",
              "providerId": "user-attribute-ldap-mapper",
              "config": {
                "ldap.attribute": ["uid"],
                "is.mandatory.in.ldap": ["true"],
                // Chuẩn Production: false để đọc từ DB/Cache cục bộ siêu tốc, định kỳ 5 phút đồng bộ ngầm
                "always.read.value.from.ldap": ["false"],
                "user.model.attribute": ["username"],
                "read.only": ["true"]
              }
            },
            // Ánh xạ thuộc tính email giữa Keycloak và LDAP (mail)
            {
              "id": "email-mapper",
              "name": "email",
              "providerId": "user-attribute-ldap-mapper",
              "config": {
                "ldap.attribute": ["mail"],
                "is.mandatory.in.ldap": ["false"],
                "always.read.value.from.ldap": ["false"],
                "user.model.attribute": ["email"],
                "read.only": ["true"]
              }
            },
            // Ánh xạ thuộc tính firstName (givenName)
            {
              "id": "first-name-mapper",
              "name": "first name",
              "providerId": "user-attribute-ldap-mapper",
              "config": {
                "ldap.attribute": ["givenName"],
                "is.mandatory.in.ldap": ["false"],
                "always.read.value.from.ldap": ["false"],
                "user.model.attribute": ["firstName"],
                "read.only": ["true"]
              }
            },
            // Ánh xạ thuộc tính lastName (sn)
            {
              "id": "last-name-mapper",
              "name": "last name",
              "providerId": "user-attribute-ldap-mapper",
              "config": {
                "ldap.attribute": ["sn"],
                "is.mandatory.in.ldap": ["true"],
                "always.read.value.from.ldap": ["false"],
                "user.model.attribute": ["lastName"],
                "read.only": ["true"]
              }
            },
            // Gán Realm Role CUSTOMER mặc định cho mọi tài khoản đồng bộ từ LDAP
            {
              "id": "customer-role-mapper",
              "name": "customer role",
              "providerId": "hardcoded-ldap-role-mapper",
              "config": {
                "role": ["CUSTOMER"]
              }
            }
          ]
        },
        "config": {
          // Trạng thái kích hoạt kết nối đến nguồn người dùng LDAP (true: bật)
          "enabled": ["true"],
          // Cho phép Keycloak nạp và lưu trữ người dùng vào Database cục bộ để tối ưu tốc độ đọc & tìm kiếm
          "importEnabled": ["true"],
          // Độ ưu tiên của nhà cung cấp này khi tìm kiếm người dùng (0 là ưu tiên cao nhất)
          "priority": ["0"],
          // Chế độ chỉnh sửa: READ_ONLY (chỉ đọc, không cho phép Keycloak ghi đè hoặc thay đổi dữ liệu ngược về LDAP)
          "editMode": ["READ_ONLY"],
          // Đồng bộ đăng ký mới: false (không tự động đẩy người dùng mới đăng ký trên Keycloak vào LDAP)
          "syncRegistrations": ["false"],
          // Loại nhà cung cấp LDAP: "other" (chuẩn OpenLDAP; ngoài ra có Active Directory, Red Hat Directory Server, v.v.)
          "vendor": ["other"],
          // Thuộc tính trên LDAP dùng làm username hiển thị trong Keycloak ("uid")
          "usernameLDAPAttribute": ["uid"],
          // Thuộc tính Relative Distinguished Name trên LDAP ("uid")
          "rdnLDAPAttribute": ["uid"],
          // Thuộc tính định danh duy nhất (UUID) của bản ghi trong LDAP ("entryUUID" cho OpenLDAP, "objectGUID" cho Active Directory)
          "uuidLDAPAttribute": ["entryUUID"],
          // Các lớp đối tượng (objectClass) trong LDAP đại diện cho thực thể người dùng
          "userObjectClasses": ["inetOrgPerson, organizationalPerson"],
          // Địa chỉ URL và cổng kết nối đến máy chủ LDAP
          "connectionUrl": ["ldap://openldap:389"],
          // Đường dẫn cây thư mục (Base DN) chứa danh sách người dùng trong LDAP
          "usersDn": ["ou=users,dc=example,dc=org"],
          // Phương thức xác thực khi Keycloak kết nối đến LDAP ("simple": username/password)
          "authType": ["simple"],
          // Tài khoản Bind DN có quyền quản trị để đọc danh bạ LDAP
          "bindDn": ["cn=admin,dc=example,dc=org"],
          // Mật khẩu của tài khoản Bind DN
          "bindCredential": ["adminpassword"],
          // Phạm vi tìm kiếm trong cây LDAP: "1" = ONE_LEVEL (chỉ tìm trong thư mục hiện tại), "2" = SUBTREE (tìm toàn bộ cây con)
          "searchScope": ["1"],
          // Xác thực chính sách mật khẩu tại Keycloak: false (để LDAP tự quản lý chính sách mật khẩu)
          "validatePasswordPolicy": ["false"],
          // Tin cậy email lấy từ LDAP mà không cần gửi email xác thực lại
          "trustEmail": ["true"],
          // Cấu hình Truststore SPI khi kết nối TLS/SSL ("always")
          "useTruststoreSpi": ["always"],
          // Bật cơ chế Connection Pooling để tái sử dụng kết nối mạng đến LDAP, tăng hiệu năng
          "connectionPooling": ["true"],
          // Bật phân trang khi truy vấn danh sách người dùng lớn từ LDAP để tránh nghẽn bộ nhớ
          "pagination": ["true"],
          // =====================================================================
          // CƠ CHẾ ĐỒNG BỘ CHUẨN PRODUCTION (HYBRID BACKGROUND DELTA + FULL SYNC)
          // =====================================================================
          // Số lượng bản ghi đồng bộ trên mỗi đợt quét (batch size: 1000 người dùng mỗi batch)
          "batchSizeForSync": ["1000"],
          // Chu kỳ quét các người dùng có thay đổi mới: 300 giây (5 phút / lần) dựa vào modifyTimestamp
          "changedSyncPeriod": ["300"],
          // Chu kỳ quét đối soát toàn bộ danh bạ: 86400 giây (24 giờ / lần) để dọn dẹp user đã bị xóa trên LDAP
          "fullSyncPeriod": ["86400"]
        }
      }
    ]
  },

  // =========================================================================
  // 9. CÁC CLIENT OAUTH2 / OPENID CONNECT (OAUTH2 CLIENTS)
  // =========================================================================
  "clients": [
    // -----------------------------------------------------------------------
    // BFF Client: Ứng dụng Backend-For-Frontend đóng vai trò Confidential Client
    // -----------------------------------------------------------------------
    {
      "clientId": "bff-client",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "secret",
      // URL chuyển hướng sau khi xác thực thành công qua Keycloak
      "redirectUris": [
        "http://localhost:8080/api/auth/callback"
      ],
      // Nguồn gốc web được phép gửi request CORS
      "webOrigins": [
        "http://localhost:3000"
      ],
      // URL chuyển hướng sau khi đăng xuất
      "postLogoutRedirectUris": [
        "http://localhost:8080"
      ],
      // Bật Authorization Code Flow (chuẩn OIDC cho ứng dụng web)
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      // Bật Service Accounts (Client Credentials Flow) để BFF gọi Admin REST API
      "serviceAccountsEnabled": true,
      "publicClient": false,
      "frontchannelLogout": false,
      "protocol": "openid-connect",
      "attributes": {
        // Sử dụng PKCE với phương thức S256 để tăng cường bảo mật
        "pkce.code.challenge.method": "S256"
      },
      // Các Scope mặc định luôn được cấp cho Client khi đăng nhập
      "defaultClientScopes": [
        "web-origins",
        "acr",
        "roles",
        "profile",
        "email",
        "event:read",
        "inventory:read",
        "ticket:read"
      ],
      // Các Scope tùy chọn cần Client chỉ định rõ trong tham số scope khi đăng nhập
      "optionalClientScopes": [
        "event:write",
        "inventory:manage",
        "order:read",
        "order:create",
        "order:cancel",
        "payment:create",
        "payment:refund",
        "ticket:checkin"
      ],
      // Protocol Mappers: Trích xuất thông tin người dùng vào ID Token và Access Token
      "protocolMappers": [
        // Ánh xạ trường email vào ID Token và UserInfo
        {
          "name": "email",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-property-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "email",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "email",
            "jsonType.label": "String"
          }
        },
        // Ánh xạ trạng thái xác thực email (email_verified: true/false)
        {
          "name": "email_verified",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-property-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "emailVerified",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "email_verified",
            "jsonType.label": "boolean"
          }
        },
        // Ánh xạ ngôn ngữ ưa thích (locale) của người dùng vào token
        {
          "name": "locale",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-attribute-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "locale",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "locale",
            "jsonType.label": "String"
          }
        },
        // Ánh xạ năm sinh (birth_year) vào token
        {
          "name": "birth_year",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-attribute-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "birth_year",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "birth_year",
            "jsonType.label": "String"
          }
        },
        // Ánh xạ Audience ("aud": "api-client") vào Access Token để chỉ định token dành riêng cho hệ thống Backend API
        {
          "name": "api-client-audience",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "consentRequired": false,
          "config": {
            "included.client.audience": "api-client",
            "id.token.claim": "false",
            "access.token.claim": "true"
          }
        }
      ]
    },
    // -----------------------------------------------------------------------
    // API Client: Đại diện cho cụm Backend Microservices (Resource Servers)
    // Phục vụ kiểm tra tính hợp lệ của Audience (chỉ chấp nhận token có "aud": "api-client")
    // và quản lý phân quyền phạm vi API (Client Roles / Permissions)
    // -----------------------------------------------------------------------
    {
      "clientId": "api-client",
      "name": "Backend Microservices Resource Server",
      "description": "Resource server đại diện cho các API microservices phía sau",
      "enabled": true,
      "bearerOnly": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "publicClient": false,
      "protocol": "openid-connect"
    }
  ],

  // =========================================================================
  // 10. TÀI KHOẢN MẪU DÙNG CHO MÔI TRƯỜNG PHÁT TRIỂN / TEST (SEED USERS)
  // =========================================================================
  "users": [
    // Tài khoản khách hàng mẫu
    {
      "username": "customer1",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Customer",
      "lastName": "One",
      "email": "customer1@example.com",
      "attributes": {
        "locale": ["vi"],
        "birth_year": ["1995"]
      },
      "credentials": [
        {
          "type": "password",
          "value": "test1234",
          "temporary": false
        }
      ],
      "realmRoles": [
        "CUSTOMER"
      ]
    },
    // Tài khoản nhà tổ chức sự kiện mẫu
    {
      "username": "organizer1",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Organizer",
      "lastName": "One",
      "email": "organizer1@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "test1234",
          "temporary": false
        }
      ],
      "realmRoles": [
        "EVENT_ORGANIZER"
      ]
    },
    // Tài khoản nhân viên soát vé mẫu
    {
      "username": "staff1",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Staff",
      "lastName": "One",
      "email": "staff1@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "test1234",
          "temporary": false
        }
      ],
      "realmRoles": [
        "STAFF"
      ]
    },
    // Tài khoản quản trị viên mẫu
    {
      "username": "admin1",
      "enabled": true,
      "emailVerified": true,
      "firstName": "Admin",
      "lastName": "One",
      "email": "admin1@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "test1234",
          "temporary": false
        }
      ],
      "realmRoles": [
        "ADMIN"
      ]
    },
    // Service Account dành cho bff-client để gọi Keycloak Admin REST API (quản lý user, logout phiên)
    {
      "username": "service-account-bff-client",
      "enabled": true,
      "serviceAccountClientId": "bff-client",
      "clientRoles": {
        "realm-management": [
          "manage-users",
          "view-users",
          "realm-admin"
        ]
      },
      "realmRoles": [
        "ADMIN"
      ]
    }
  ],

  // =========================================================================
  // 11. NHÀ CUNG CẤP DANH TÍNH MẠNG XÃ HỘI (GOOGLE IDENTITY PROVIDER)
  // Xử lý đăng nhập bằng Google và cơ chế tự động phát hiện, hợp nhất tài khoản trùng email
  // =========================================================================
  "identityProviders": [
    {
      "alias": "google",
      "displayName": "Google",
      "providerId": "google",
      "enabled": true,
      "updateProfileFirstLoginMode": "off",
      // Tin cậy email trả về từ Google (cho phép phát hiện và liên kết với tài khoản sẵn có)
      "trustEmail": true,
      "storeToken": false,
      "addReadTokenRoleOnCreate": false,
      "authenticateByDefault": false,
      "linkOnly": false,
      // Kích hoạt luồng first broker login để nhắc người dùng liên kết khi đã có tài khoản trùng email
      "firstBrokerLoginFlowAlias": "first broker login",
      "config": {
        "clientId": "${env.GOOGLE_CLIENT_ID:your-google-client-id-hint.apps.googleusercontent.com}",
        "clientSecret": "${env.GOOGLE_CLIENT_SECRET:your-google-client-secret-hint}",
        "defaultScope": "openid profile email",
        "syncMode": "IMPORT",
        "useJwksUrl": "true"
      }
    }
  ],

  // =========================================================================
  // 12. CÁC HÀNH ĐỘNG BẮT BUỘC ĐỐI VỚI NGƯỜI DÙNG (REQUIRED ACTIONS)
  // =========================================================================
  "requiredActions": [
    // Bắt buộc xác thực email qua link gửi tới hòm thư
    {
      "alias": "VERIFY_EMAIL",
      "name": "Verify Email",
      "providerId": "VERIFY_EMAIL",
      "enabled": true,
      "defaultAction": false,
      "priority": 50,
      "config": {}
    },
    // Yêu cầu cập nhật hồ sơ cá nhân
    {
      "alias": "UPDATE_PROFILE",
      "name": "Update Profile",
      "providerId": "UPDATE_PROFILE",
      "enabled": true,
      "defaultAction": false,
      "priority": 20,
      "config": {}
    },
    // Yêu cầu đổi mật khẩu (khi mật khẩu tạm thời hoặc hết hạn)
    {
      "alias": "UPDATE_PASSWORD",
      "name": "Update Password",
      "providerId": "UPDATE_PASSWORD",
      "enabled": true,
      "defaultAction": false,
      "priority": 30,
      "config": {}
    },
    // Kích hoạt xác thực lại email khi người dùng thay đổi địa chỉ email của mình
    {
      "alias": "UPDATE_EMAIL",
      "name": "Update Email",
      "providerId": "UPDATE_EMAIL",
      "enabled": true,
      "defaultAction": false,
      "priority": 40,
      "config": {}
    }
  ],

  // =========================================================================
  // 13. ÁNH XẠ PHÂN QUYỀN GIỮA ROLE VÀ CLIENT SCOPE (SCOPE MAPPINGS)
  // Quy định: Khi Client yêu cầu một Client Scope thì người dùng phải có Role tương ứng
  // mới được cấp Scope đó vào Token (Delegated Authorization / Principle of Least Privilege)
  // =========================================================================
  "scopeMappings": [
    // Scope xem thông tin sự kiện: Áp dụng cho mọi vai trò
    {
      "clientScope": "event:read",
      "roles": ["CUSTOMER", "ORGANIZER", "STAFF", "ADMIN"]
    },
    // Scope tạo & sửa sự kiện: Chỉ dành cho Nhà tổ chức và Quản trị viên
    {
      "clientScope": "event:write",
      "roles": ["ORGANIZER", "ADMIN"]
    },
    // Scope xem tồn kho vé: Khách hàng, Nhà tổ chức, Quản trị viên
    {
      "clientScope": "inventory:read",
      "roles": ["CUSTOMER", "ORGANIZER", "ADMIN"]
    },
    // Scope quản lý và điều chỉnh kho vé: Nhà tổ chức và Quản trị viên
    {
      "clientScope": "inventory:manage",
      "roles": ["ORGANIZER", "ADMIN"]
    },
    // Scope xem lịch sử đơn hàng: Khách hàng, Nhà tổ chức, Quản trị viên
    {
      "clientScope": "order:read",
      "roles": ["CUSTOMER", "ORGANIZER", "ADMIN"]
    },
    // Scope tạo và đặt mua vé: Khách hàng và Quản trị viên
    {
      "clientScope": "order:create",
      "roles": ["CUSTOMER", "ADMIN"]
    },
    // Scope hủy đơn hàng: Khách hàng và Quản trị viên
    {
      "clientScope": "order:cancel",
      "roles": ["CUSTOMER", "ADMIN"]
    },
    // Scope tạo giao dịch thanh toán: Khách hàng và Quản trị viên
    {
      "clientScope": "payment:create",
      "roles": ["CUSTOMER", "ADMIN"]
    },
    // Scope xử lý hoàn tiền: Chỉ dành riêng cho Quản trị viên toàn hệ thống
    {
      "clientScope": "payment:refund",
      "roles": ["ADMIN"]
    },
    // Scope xem chi tiết vé điện tử: Áp dụng cho mọi vai trò
    {
      "clientScope": "ticket:read",
      "roles": ["CUSTOMER", "ORGANIZER", "STAFF", "ADMIN"]
    },
    // Scope quét mã QR và check-in vé: Nhân viên soát vé và Quản trị viên
    {
      "clientScope": "ticket:checkin",
      "roles": ["STAFF", "ADMIN"]
    }
  ]
}

```