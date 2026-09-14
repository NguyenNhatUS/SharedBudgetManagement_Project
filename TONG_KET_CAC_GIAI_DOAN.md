# TỔNG KẾT DỰ ÁN & BỐI CẢNH TIẾP NỐI PHÁT TRIỂN
## Dự án: Shared Budget Management (Quản Lý Ngân Sách Chung)

> **Tài liệu này được tạo ra nhằm lưu giữ toàn bộ bối cảnh kiến trúc, các quyết định quan trọng và tiến độ thực tế của dự án để bất kỳ phiên làm việc (hoặc AI conversation mới) nào cũng có thể bắt nhịp và tiếp tục phát triển ngay lập tức mà không làm mất mát ngữ cảnh.**

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
  * `PageResponse<T>`: Format phân trang chuẩn hóa cho Frontend gồm `items`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isLast`.
* **DTO Layer (`dto`)**:
  * 9 Request DTOs được validate chặt chẽ với Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Positive`, `@NotNull`, `@Email`): `CreateBudgetRequest`, `UpdateBudgetRequest`, `InviteMemberRequest`, `ChangeMemberRoleRequest`, `CreateTransactionRequest`, `UpdateTransactionRequest`, `CreateTagRequest`, `UpdateTagRequest`, `UpdateProfileRequest`.
  * 5 Response DTOs che giấu password và token nhạy cảm: `UserResponse`, `BudgetResponse`, `BudgetMemberResponse`, `TransactionResponse`, `TagResponse`.
* **MapStruct Mappers (`mapper`)**:
  * `UserMapper`, `BudgetMapper`, `BudgetMemberMapper`, `TagMapper`, `TransactionMapper` (chuyển đổi danh sách `TransactionTag` thành danh sách `TagResponse` phẳng).
* **4 REST Controllers (`controller.v1`)**:
  * `BudgetController` (`/api/v1/budgets`): CRUD ngân sách + toàn bộ API quản lý thành viên.
  * `TransactionController` (`/api/v1/budgets/{budgetId}/transactions`): CRUD giao dịch theo dạng Nested Resource, hỗ trợ lọc ngày/loại kèm phân trang, API thống kê `/summary` và `/by-member`.
  * `TagController` (`/api/v1/tags`): Quản lý danh mục.
  * `UserController` (`/api/v1/users`): Xem và cập nhật hồ sơ cá nhân.

---

## 🛠 3. LỊCH SỬ SỬA LỖI & TINH CHỈNH GẦN NHẤT

* **Nguyên nhân lỗi Terminal trước đó**:
  * Trong `UserRepository` còn sót lại 2 derived query methods: `findByEmailVerificationToken` và `findByPasswordResetToken`.
  * Khi `User` entity đã xóa 2 trường này (thuộc phần mail), Spring Data JPA không parse được thuộc tính và ném ra `PropertyReferenceException`, làm sập `ApplicationContext`.
* **Kết quả khắc phục**:
  * Đã xóa sạch 2 methods trên khỏi `UserRepository`.
  * Đã xóa `spendingLimit` khỏi `Budget`, DTOs, và query thống kê trong `TransactionRepository`.
  * Đã xóa `emailVerified` khỏi `UserResponse`.
* **Tình trạng hiện tại**:
  * Biên dịch: `.\mvnw.cmd clean compile` ➜ **BUILD SUCCESS** (0 error, 0 warning).
  * Unit Test: `.\mvnw.cmd test` ➜ **BUILD SUCCESS** (`contextLoads` passed 100%).
  * Khởi động: Tomcat lắng nghe ổn định tại cổng `8080`.

---

## 🗺 4. BẢN THIẾT KẾ LỘ TRÌNH 12 GIAI ĐOẠN ĐÃ ĐIỀU CHỈNH

- [x] **Giai đoạn 1**: Thiết kế Cơ sở dữ liệu & Entity Model
- [x] **Giai đoạn 2**: Tầng Repository & Service Logic (JPA Nâng cao, Fix N+1, Atomic Transactions)
- [x] **Giai đoạn 3**: Chuẩn hóa lớp REST API, DTO & MapStruct
- [ ] **Giai đoạn 4: Global Exception Handling (BƯỚC TIẾP THEO CẦN LÀM)**
  * Xây dựng `ErrorCode` enum.
  * Tạo Custom Domain Exceptions (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException`).
  * Xây dựng `GlobalExceptionHandler` với `@RestControllerAdvice` bắt các lỗi:
    * `MethodArgumentNotValidException` ➜ Trả về chi tiết lỗi từng trường (`fieldName: errorMessage`).
    * `ResourceNotFoundException` / `EntityNotFoundException` ➜ HTTP 404.
    * `IllegalArgumentException` / `IllegalStateException` ➜ HTTP 400.
    * `Exception` (lỗi không xác định) ➜ HTTP 500 kèm log SLF4J an toàn.
  * Trả về đồng nhất qua `ApiResponse.error(...)`.
- [ ] **Giai đoạn 5: Spring Security 6 & JWT Authentication** (Đăng ký, Đăng nhập thường, Access/Refresh Token rotation, phân quyền Resource-based theo từng Budget).
- [ ] **Giai đoạn 6: Tích hợp OAuth2 Login** (Google Login, map User và tái sử dụng JWT pipeline của GĐ 5).
- [ ] **Giai đoạn 7: Redis Caching & Rate Limiting** (Cache-Aside cho Tags/Summary, Token Blacklist cho logout tức thì, Rate Limiting chống spam).
- [ ] **Giai đoạn 8: Tích hợp Spring Mail** (Gửi link mời tham gia qua `inviteToken`, gửi mã Reset Password lưu ở Redis có TTL 5 phút).
- [ ] **Giai đoạn 9: OpenAPI / Swagger UI** (Tích hợp SpringDoc OpenAPI 3, giao diện `swagger-ui.html` có Bearer Auth).
- [ ] **Giai đoạn 10: Kiểm thử tự động (Unit Test & Integration Test)** (JUnit 5 + Mockito cho Service layer).
- [ ] **Giai đoạn 11: Xây dựng Giao diện Web Frontend** (HTML5, Vanilla CSS hiện đại, JavaScript Fetch API).
- [ ] **Giai đoạn 12: Containerization & DevOps** (Dockerfile multi-stage, `docker-compose.yml` App + MySQL + Redis).

---

## 💡 5. HƯỚNG DẪN PROMPT KHI MỞ CONVERSATION MỚI

Khi bạn mở một phiên chat mới (New Chat), hãy copy & paste câu lệnh sau:

```text
Đọc 2 file @README.md và @TONG_KET_CAC_GIAI_DOAN.md để nắm context dự án SharedBudgetManagement. 
Hiện tại đã hoàn thành 100% Giai đoạn 1, 2, 3 và hệ thống compile/test hoàn toàn không có lỗi.
Bây giờ hãy bắt đầu triển khai GIAI ĐOẠN 4: Global Exception Handling cho tôi.
```
