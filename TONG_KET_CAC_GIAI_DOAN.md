# TỔNG KẾT DỰ ÁN & BỐI CẢNH TIẾP NỐI PHÁT TRIỂN
## Dự án: Shared Budget Management (Quản Lý Ngân Sách Chung)

> **Tài liệu này được tạo ra nhằm lưu giữ toàn bộ bối cảnh kiến trúc, các quyết định quan trọng và tiến độ thực tế của dự án để bất kỳ phiên làm việc (hoặc AI conversation mới) nào cũng có thể bắt nhịp và tiếp tục phát triển ngay lập tức mà không làm mất mát ngữ cảnh.**  
> 🧭 **Dành cho Onboarding / Review mã nguồn**: Xem lộ trình thứ tự review codebase từng package và phân tích chi tiết từng file tại **[HUONG_DAN_REVIEW_CODEBASE.md](HUONG_DAN_REVIEW_CODEBASE.md)**.

---

## 📌 1. BỐI CẢNH & CÁC QUYẾT ĐỊNH KIẾN TRÚC QUAN TRỌNG (ADR)

1. **Công nghệ cốt lõi**:
   * **Java 21**, **Spring Boot 3.x**, **Maven**.
   * **MySQL 8.0** kết hợp **Spring Data JPA** & **Hibernate 7.x**.
   * **MapStruct 1.6.3** + `lombok-mapstruct-binding 0.2.0`: Tự động sinh mã Mapper hiệu năng cao khi compile (không dùng Reflection).
   * **Jakarta Bean Validation**: Validate dữ liệu đầu vào tại Controller.
2. **Quyết định về OAuth2 và Spring Mail**:
   * **Chưa tích hợp ở giai đoạn đầu**: Để phục vụ lộ trình học tập từ gốc lên ngọn, toàn bộ các phần liên quan đến OAuth2 và Spring Mail (như `spendingLimit`, `emailVerified`, token xác thực mail) đã được dọn sạch khỏi các package ở Giai đoạn 1-3.
   * **Vị trí tích hợp hợp lý**:
     * **OAuth2 (Google Login)**: Tích hợp tại **Giai đoạn 6** (sau khi đã nắm vững Spring Security 6 và cơ chế sinh JWT ở Giai đoạn 5).
     * **Spring Mail**: Tích hợp tại **Giai đoạn 8** (sau khi đã có Redis ở Giai đoạn 7 để lưu mã OTP / Token quên mật khẩu có thời hạn `TTL = 5 phút`).
3. **Cơ chế xác thực tạm thời ở Giai đoạn 1-3**:
   * Hiện tại chưa có Spring Security, các endpoint cần danh tính người dùng đang tạm thời nhận `userId` qua query parameter (ví dụ: `POST /api/v1/budgets?userId=1`).
   * Khi hoàn thành **Giai đoạn 5 (Security & JWT)**, toàn bộ các tham số này sẽ được thay thế bằng `@AuthenticationPrincipal UserDetails` lấy từ `SecurityContextHolder`.

---

## 🚀 2. CHI TIẾT CÁC GIAI ĐOẠN ĐÃ HOÀN THÀNH (100% TESTED & COMPILED)

### 🔹 Giai đoạn 1: Thiết Kế Cơ Sở Dữ Liệu & Entity Model
* **7 Entities**:
  * `User` (`users`): Thông tin tài khoản, mật khẩu, họ tên, vai trò toàn cục.
  * `Budget` (`budgets`): Ngân sách chung, đơn vị tiền tệ (`currency`), người tạo (`createdBy`).
  * `BudgetMember` (`budget_members`): Bảng trung gian giải quyết quan hệ Nhiều - Nhiều giữa `User` và `Budget`. Lưu `role` (`OWNER`, `EDITOR`, `VIEWER`), trạng thái lời mời `status` (`PENDING`, `ACCEPTED`, `DECLINED`), và mã mời `inviteToken`.
  * `Transaction` (`transactions`): Giao dịch thu (`INCOME`) hoặc chi (`EXPENSE`), số tiền, ngày giao dịch, người tạo (`BudgetMember`).
  * `Tag` (`tags`): Nhãn danh mục phân loại chi tiêu.
  * `TransactionTag` (`transaction_tags`): Bảng trung gian Nhiều - Nhiều giữa `Transaction` và `Tag`.
  * `RefreshToken` (`refresh_tokens`): Lưu trữ token làm mới phiên đăng nhập JWT đa thiết bị.
* **4 Enums**: `UserRole`, `BudgetRole`, `MemberStatus`, `TransactionType`.
* **Ràng buộc toàn vẹn**:
  * Composite Unique `@UniqueConstraint(name = "uk_budget_member", columnNames = {"user_id", "budget_id"})`.
  * Composite Unique `@UniqueConstraint(name = "uk_transaction_tag", columnNames = {"transaction_id", "tag_id"})`.
  * Lifecycle callbacks `@PrePersist`, `@PreUpdate` tự động gán `createdAt`, `updatedAt`.

---

### 🔹 Giai đoạn 2: Tầng Repository & Service Logic (JPA Nâng Cao)
* **Giải quyết triệt để vấn đề N+1 Query**:
  * `application.properties`: Cấu hình `spring.jpa.properties.hibernate.default_batch_fetch_size=10`.
  * `BudgetRepository`: Sử dụng `@EntityGraph(attributePaths = {"members", "members.user"})` để load Budget kèm toàn bộ thành viên chỉ với 1 câu JOIN FETCH.
* **7 Repositories**:
  * `UserRepository`, `BudgetRepository`, `BudgetMemberRepository`, `TransactionRepository`, `TagRepository`, `TransactionTagRepository`, `RefreshTokenRepository`.
  * Hỗ trợ phân trang và sắp xếp `Pageable` cho danh sách giao dịch.
  * Các câu query thống kê JPQL: `sumAmountByBudgetIdGroupByType` (Tổng thu/chi) và `sumExpenseByBudgetIdGroupByMember` (Chi tiêu theo thành viên).
* **5 Services (Interface + Implementation)**:
  * **Tính nguyên tử (Atomic Transaction)**: Trong `BudgetServiceImpl.createBudget`, hệ thống tạo `Budget` và tự động ghi nhận ngay bản ghi `BudgetMember` với vai trò `OWNER` trong 1 transaction duy nhất (nếu lỗi sẽ rollback toàn bộ).
  * **Quy tắc bảo vệ OWNER**: Không cho phép hạ quyền, xoá, hoặc để `OWNER` tự rời nhóm mà chưa chuyển giao quyền sở hữu.
  * **Quản lý lời mời qua Token**: Mời thành viên bằng `inviteToken` dạng UUID. Khi người dùng Chấp nhận (`acceptInvite`) hoặc Từ chối (`declineInvite`), token tự động bị gán về `null` để ngăn chặn Replay Attack.

---

### 🔹 Giai đoạn 3: Chuẩn Hóa Lớp REST API, DTO & MapStruct
* **Chuẩn hóa cấu trúc Response (`common`)**:
  * `ApiResponse<T>`: Format chuẩn gồm `status`, `message`, `data`, `timestamp`.
  * `PageResponse<T>`: Format phân trang & sắp xếp chuẩn hóa cho Frontend gồm `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `empty`, `sortBy`, `sortDirection`. Tích hợp static factory method `PageResponse.from(page, mapper)` giúp chuyển đổi trực tiếp từ Spring Data `Page<Entity>` sang DTO trong 1 dòng lệnh.
* **DTO Layer (`dto`)**:
  * 9 Request DTOs được validate chặt chẽ với Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Positive`, `@NotNull`, `@Email`): `CreateBudgetRequest`, `UpdateBudgetRequest`, `InviteMemberRequest`, `ChangeMemberRoleRequest`, `CreateTransactionRequest`, `UpdateTransactionRequest`, `CreateTagRequest`, `UpdateTagRequest`, `UpdateProfileRequest`.
  * 5 Response DTOs che giấu password và token nhạy cảm: `UserResponse`, `BudgetResponse`, `BudgetMemberResponse`, `TransactionResponse`, `TagResponse`.
* **MapStruct Mappers (`mapper`)**:
  * `UserMapper`, `BudgetMapper`, `BudgetMemberMapper`, `TagMapper`, `TransactionMapper` (chuyển đổi danh sách `TransactionTag` thành danh sách `TagResponse` phẳng).
* **4 REST Controllers (`controller.v1`)**:
  * `BudgetController` (`/api/v1/budgets`): CRUD ngân sách + toàn bộ API quản lý thành viên. Áp dụng `PageResponse` có phân trang & sắp xếp cho `GET /api/v1/budgets` (mặc định sort `createdAt,desc`) và `GET /api/v1/budgets/{budgetId}/members` (mặc định sort `joinedAt,asc`).
  * `TransactionController` (`/api/v1/budgets/{budgetId}/transactions`): CRUD giao dịch theo dạng Nested Resource, hỗ trợ lọc ngày/loại kèm phân trang & sắp xếp trả về `PageResponse` (mặc định sort `transactionDate,desc`), API thống kê `/summary` và `/expense-by-member`.
  * `TagController` (`/api/v1/tags`): Quản lý danh mục, hỗ trợ phân trang & sắp xếp qua `PageResponse` (mặc định sort `name,asc`).
  * `UserController` (`/api/v1/users`): Xem và cập nhật hồ sơ cá nhân.

### 🔹 Giai đoạn 4: Global Exception Handling (Xử Lý Ngoại Lệ Toàn Cục)
* **Tài liệu kiểm tra chi tiết**: Xem file [`KIEM_TRA_GIAI_DOAN_4.md`](KIEM_TRA_GIAI_DOAN_4.md).
* **Chuẩn hóa danh mục mã lỗi (`exception.ErrorCode`)**:
  * Enum tập trung định nghĩa 18 mã lỗi định danh (`VALIDATION_FAILED`, `USER_NOT_FOUND`, `BUDGET_NOT_FOUND`, `TAG_ALREADY_EXISTS`, `CANNOT_MODIFY_OWNER`,...).
* **Hệ thống Custom Domain Exceptions (`exception`)**:
  * `AppException`: Runtime exception gốc chứa `ErrorCode` và `customMessage`.
  * `ResourceNotFoundException` (HTTP 404), `BadRequestException` (HTTP 400), `ConflictException` (HTTP 409), `ForbiddenException` (HTTP 403), `UnauthorizedException` (HTTP 401).
* **Bộ điều hướng xử lý ngoại lệ toàn cục (`exception.GlobalExceptionHandler`)**:
  * Sử dụng `@RestControllerAdvice` bắt và gói toàn bộ exception về chuẩn `ApiResponse<T>`.
  * Bắt `MethodArgumentNotValidException` ➜ Trả về `Map<String, String>` chi tiết lỗi từng trường (`fieldName: errorMessage`).
  * Bắt `AppException` và các lớp con ➜ Trả về HTTP status và message tương ứng.
  * Bắt `EntityNotFoundException` (JPA) ➜ Trả về HTTP 404.
  * Bắt `IllegalArgumentException`, `IllegalStateException` ➜ Trả về HTTP 400.
  * Bắt `Exception` (lỗi 500) ➜ Ghi log SLF4J chi tiết stacktrace và ẩn lỗi nhạy cảm khỏi client.
* **Refactor toàn bộ tầng Service**:
  * Các service (`UserServiceImpl`, `BudgetServiceImpl`, `BudgetMemberServiceImpl`, `TransactionServiceImpl`, `TagServiceImpl`) đều ném đúng Custom Domain Exception.
* **Unit Testing (`GlobalExceptionHandlerTest`)**:
  * 8 test cases kiểm thử toàn diện các luồng bắt lỗi, nâng tổng số test của dự án lên 12 tests.

---

## 🛠 3. LỊCH SỬ SỬA LỖI & TINH CHỈNH GẦN NHẤT

* **Hoàn thiện Giai đoạn 4: Global Exception Handling**:
  * Xây dựng `ErrorCode`, `AppException`, các domain exceptions (400, 403, 404, 409) và `GlobalExceptionHandler`.
  * Refactor toàn bộ service layer sang domain exception.
  * Bổ sung 8 test cases `GlobalExceptionHandlerTest`.
  * Tạo tài liệu đối soát [`KIEM_TRA_GIAI_DOAN_4.md`](KIEM_TRA_GIAI_DOAN_4.md).
* **Hoàn thiện Pagination & Sorting với `PageResponse<T>`**:
  * Bổ sung đầy đủ metadata sắp xếp và trạng thái vào `PageResponse`: `first`, `empty`, `sortBy`, `sortDirection`.
  * Cung cấp helper `PageResponse.from(page, mapper)` tối ưu hoá code chuyển đổi DTO ở Controller.
  * Tích hợp `PageResponse` đồng bộ cho toàn bộ các endpoint danh sách:
    * `GET /api/v1/budgets?userId={id}&page=0&size=10&sort=createdAt,desc`
    * `GET /api/v1/budgets/{budgetId}/members?page=0&size=20&sort=joinedAt,asc`
    * `GET /api/v1/budgets/{budgetId}/transactions?page=0&size=20&sort=transactionDate,desc`
    * `GET /api/v1/tags?page=0&size=20&sort=name,asc`
  * Bổ sung Unit Test `PageResponseTest` kiểm thử đầy đủ các kịch bản phân trang, sắp xếp và mapping.
* **Nguyên nhân lỗi Terminal trước đó**:
  * Trong `UserRepository` còn sót lại 2 derived query methods: `findByEmailVerificationToken` và `findByPasswordResetToken`.
  * Khi `User` entity đã xóa 2 trường này (thuộc phần mail), Spring Data JPA không parse được thuộc tính và ném ra `PropertyReferenceException`, làm sập `ApplicationContext`.
* **Kết quả khắc phục**:
  * Đã xóa sạch 2 methods trên khỏi `UserRepository`.
  * Đã xóa `spendingLimit` khỏi `Budget`, DTOs, và query thống kê trong `TransactionRepository`.
  * Đã xóa `emailVerified` khỏi `UserResponse`.
* **Tình trạng hiện tại**:
  * Biên dịch: `.\mvnw.cmd clean compile` ➜ **BUILD SUCCESS** (78 source files, 0 error, 0 warning).
  * Unit Test: `.\mvnw.cmd test` ➜ **BUILD SUCCESS** (37/37 tests passed 100%).
  * Khởi động: Tomcat lắng nghe ổn định tại cổng `8080`.
* **Hoàn thành Giai đoạn 5: Spring Security 6 & JWT Authentication**:
  * Tích hợp thư viện JJWT `0.12.6` và cấu hình bí mật HMAC-SHA256 trong `application.properties`.
  * Xây dựng tầng bảo mật cốt lõi: `UserPrincipal`, `CustomUserDetailsService`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `SecurityConfig`.
  * Hoàn thiện xác thực phi trạng thái (Stateless), xoay vòng Refresh Token (Token Rotation) chống Replay Attack, hỗ trợ phòng thủ thu hồi toàn bộ token khi phát hiện token cũ bị tái sử dụng.
  * Phân quyền Resource-Based Authorization theo từng Budget (`OWNER`, `EDITOR`, `VIEWER`) bằng component `BudgetSecurity` kết hợp biểu thức SpEL trong `@PreAuthorize`.
  * **Xóa bỏ hoàn toàn việc truyền `userId` qua query param**: Thay thế triệt để bằng `@AuthenticationPrincipal UserPrincipal` và `SecurityUtils.getCurrentUserId()`.
  * Bổ sung endpoint lấy thông tin cá nhân hiện tại `GET /api/v1/users/me`.
  * Bổ sung 25 Unit Tests mới cho tầng bảo mật và xác thực (`JwtTokenProviderTest`, `BudgetSecurityTest`, `AuthServiceTest`, `AuthControllerTest`), nâng tổng số test lên 37/37 tests passed 100%.
  * Tạo tài liệu đối soát [`KIEM_TRA_GIAI_DOAN_5.md`](KIEM_TRA_GIAI_DOAN_5.md).

* **Hoàn thành Giai đoạn 6: Tích hợp OAuth2 Login (Google Login)**:
  * Cấu hình Google OAuth2 Client trong `application.properties` (Client ID, Client Secret, scopes `openid`, `profile`, `email`).
  * Mở rộng `User` entity với enum `AuthProvider` (`LOCAL`, `GOOGLE`) và trường `providerId`.
  * Triển khai `CustomOAuth2UserService` kế thừa `DefaultOAuth2UserService` để đồng bộ thông tin Google profile vào cơ sở dữ liệu MySQL (hỗ trợ Account Linking và Provider Migration).
  * Nâng cấp `UserPrincipal` thực thi đồng thời cả `UserDetails` và `OAuth2User`, đảm bảo tính tương thích tuyệt đối giữa hệ thống JWT nội bộ và Social Login.
  * Tích hợp `OAuth2AuthenticationSuccessHandler` sinh cặp Access Token (JWT) và Refresh Token nội bộ, lưu vào database và chuyển hướng an toàn về Frontend SPA qua query parameters.
  * Tích hợp `OAuth2AuthenticationFailureHandler` bắt lỗi xác thực và chuyển hướng thông báo về Frontend.
  * Xử lý triệt để lỗi Circular Dependency (`SecurityConfig` <-> `SuccessHandler` <-> `AuthService`).
  * Bổ sung bộ Unit Tests cho OAuth2 (`CustomOAuth2UserServiceTest`, `OAuth2AuthenticationSuccessHandlerTest`), nâng tổng số test lên **44/44 tests passed 100%**.
  * Tạo tài liệu đối soát [`KIEM_TRA_GIAI_DOAN_6.md`](KIEM_TRA_GIAI_DOAN_6.md), cẩm nang chuyên sâu [`OAUTH2_EXPLANATION.md`](OAUTH2_EXPLANATION.md) và giáo trình thực chiến [`HUONG_DAN_UNIT_TEST_MOCKITO.md`](HUONG_DAN_UNIT_TEST_MOCKITO.md).

## 🗺 4. BẢN THIẾT KẾ LỘ TRÌNH ĐÃ ĐIỀU CHỈNH THEO YÊU CẦU MỚI

- [x] **Giai đoạn 1**: Thiết kế Cơ sở dữ liệu & Entity Model (JPA Entity, Enums, Composite Constraints, Indexes)
- [x] **Giai đoạn 2**: Tầng Repository & Service Logic (JPA Nâng cao, Fix N+1 `@EntityGraph`, Atomic Transactions)
- [x] **Giai đoạn 3**: Chuẩn hóa lớp REST API, DTO & MapStruct (ApiResponse, PageResponse, Validation `@Valid`)
- [x] **Giai đoạn 4**: Global Exception Handling (`@RestControllerAdvice`, `ErrorCode`, Domain Exception Hierarchy)
- [x] **Giai đoạn 5**: Spring Security 6 & JWT Authentication (Stateless, Token Rotation, Resource-Based RBAC `BudgetSecurity`)
- [x] **Giai đoạn 6**: Tích hợp OAuth2 Login (Google Login, OIDC, CustomOAuth2UserService, JWT Handlers, Account Linking)
- [x] **Giai đoạn 7: Redis Caching & Rate Limiting (ĐÃ HOÀN THÀNH - 56/56 Tests Passed)**
  * *Báo cáo kiểm thử & so sánh*: Xem chi tiết tại **[KIEM_THU_GIAI_DOAN_7.md](KIEM_THU_GIAI_DOAN_7.md)**.
  * *Cẩm nang lý thuyết & dự án mẫu độc lập*: Xem chi tiết tại **[HUONG_DAN_REDIS_CACHING_RATE_LIMIT.md](HUONG_DAN_REDIS_CACHING_RATE_LIMIT.md)**.
  * Cache-Aside cho Tags và thống kê thu chi Summary bằng Spring Data Redis với GenericJackson2JsonRedisSerializer.
  * Token Blacklist lưu ở Redis cho tính năng Logout tức thì và phòng vệ Token Revocation (kiểm tra tại `JwtAuthenticationFilter`).
  * Rate Limiting chống spam và tấn công DoS bằng Redis Atomic Counter (`@RateLimit` + `RateLimitInterceptor`, trả về HTTP 429).
- [x] **Giai đoạn 8: Tích hợp Spring Mail & Password Reset OTP with Redis (ĐÃ HOÀN THÀNH - 127/127 Tests Passed)**
  * *Báo cáo kiểm thử & so sánh*: Xem chi tiết tại **[KIEM_THU_GIAI_DOAN_8.md](KIEM_THU_GIAI_DOAN_8.md)**.
  * *Cẩm nang lý thuyết & dự án mẫu độc lập*: Xem chi tiết tại **[HUONG_DAN_SPRING_MAIL_OTP_REDIS.md](HUONG_DAN_SPRING_MAIL_OTP_REDIS.md)**.
  * Tích hợp `spring-boot-starter-mail` gửi email HTML giao diện Gradient responsive, chuyên nghiệp.
  * Xử lý gửi mail hoàn toàn bất đồng bộ (`@Async("mailTaskExecutor")`) qua `ThreadPoolTaskExecutor`, trả về HTTP response tức thì (< 50ms).
  * Tự động gửi email mời tham gia ngân sách qua `inviteToken` kèm liên kết chấp nhận/từ chối.
  * Tính năng Quên & Đặt lại mật khẩu: sinh mã OTP 6 số bảo mật cao (`SecureRandom`), quản lý thời hạn ngắn hạn trong Redis (`TTL = 5 phút`), tự động thu hồi toàn bộ phiên đăng nhập (Refresh Tokens) khi đổi mật khẩu để bảo vệ tài khoản.
  * Áp dụng `@RateLimit` bảo vệ các endpoint gửi email chống spam.
- [ ] **Giai đoạn 9: Containerization & DevOps (Dockerfile & Docker Compose) (BƯỚC TIẾP THEO CẦN LÀM)**
  * *Cẩm nang lý thuyết & dự án mẫu độc lập*: Xem chi tiết tại **[CAC_KIEN_THUC_CAN_CO_DE_HOAN_THANH_GIAI_DOAN_9.md](CAC_KIEN_THUC_CAN_CO_DE_HOAN_THANH_GIAI_DOAN_9.md)**.
  * Viết Multi-stage `Dockerfile` tối ưu dung lượng image (< 200MB) dựa trên Eclipse Temurin JRE Alpine/Distroless.
  * Xây dựng `docker-compose.yml` tích hợp hệ sinh thái chạy độc lập 1 lệnh: **App + MySQL 8 + Redis**.
  * Cấu hình biến môi trường (`.env`), quản lý healthcheck cho database và cache để đảm bảo thứ tự khởi động (startup dependency).
- [ ] **Giai đoạn 10: CI/CD Pipeline (GitHub Actions)**
  * Xây dựng workflow GitHub Actions: Tự động kích hoạt khi push code/tạo PR.
  * Tự động chạy toàn bộ 127 unit tests, kiểm tra style, build Docker image, kiểm tra bảo mật và phát hành bản build.
- [ ] **Giai đoạn 11: OpenAPI 3 / Swagger UI & API Documentation**
  * Tích hợp SpringDoc OpenAPI 3 với giao diện trực quan `swagger-ui.html`.
  * Cấu hình Bearer JWT Authentication trên Swagger để test trực tiếp các API có bảo mật.
  * Bổ sung mô tả chi tiết, schema validation và ví dụ minh họa cho từng endpoint.
- [ ] **Giai đoạn 12: Tích hợp Message Queue (RabbitMQ / Event-Driven Architecture - Mở rộng nếu kịp thời gian)**
  * Tách rời (Decouple) luồng xử lý chính với các tác vụ nặng ngầm: Chuyển việc gửi email và bắn thông báo sang mô hình Event-Driven qua RabbitMQ.
  * Cấu hình Exchange, Routing Keys và Queues (`email.invite.queue`, `email.reset-pwd.queue`, `alert.budget-overrun.queue`).
  * Triển khai Dead Letter Queue (DLQ) và Retry Mechanism với Exponential Backoff.
  * Cập nhật `docker-compose.yml` thêm RabbitMQ broker.

---

## 💡 5. HƯỚNG DẪN PROMPT KHI MỞ CONVERSATION MỚI

Khi bạn mở một phiên chat mới (New Chat), hãy copy & paste câu lệnh sau:

```text
Đọc 2 file @README.md và @TONG_KET_CAC_GIAI_DOAN.md để nắm context dự án SharedBudgetManagement. 
Hiện tại đã hoàn thành 100% Giai đoạn 1, 2, 3, 4, 5, 6, 7, 8 và toàn bộ hệ thống kiểm thử tự động đã PASS hoàn toàn (127/127 tests passed).
Bây giờ hãy bắt đầu triển khai GIAI ĐOẠN 9: Containerization & DevOps (Dockerfile đa tầng & Docker Compose tích hợp App + MySQL + Redis) cho tôi.
```



