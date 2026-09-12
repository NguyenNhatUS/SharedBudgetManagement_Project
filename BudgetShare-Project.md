# DỰ ÁN ĐỀ XUẤT

# BudgetShare

## Hệ thống Quản lý Chi tiêu Nhóm / Ngân sách chung — Spring Boot Monolith

## 1. Sơ bộ về dự án

### 1.1. Ý tưởng

BudgetShare là hệ thống backend cho phép nhiều người dùng cùng quản lý chung một ngân sách (gia đình, nhóm bạn ở ghép, nhóm du lịch...): theo dõi thu/chi, phân loại giao dịch, mời thành viên tham gia và phân quyền theo vai trò trong từng ngân sách cụ thể.

Điểm khác biệt so với SmartBank (dự án trước): SmartBank tập trung vào nghiệp vụ core banking (account, transaction, bảo mật giao dịch). BudgetShare tập trung vào bài toán authorization phức tạp hơn — quyền của một user không cố định toàn hệ thống mà phụ thuộc vào từng resource (Budget) cụ thể — cùng với việc thiết kế quan hệ nhiều-nhiều qua entity trung gian một cách có nghiệp vụ thật, không phải minh hoạ suông.

### 1.2. Actor & vai trò

- **User**: tài khoản cá nhân, có thể tham gia nhiều Budget.
- **Budget**: một ngân sách chung, do một User tạo (trở thành OWNER).
- **BudgetMember**: entity trung gian giữa User và Budget, lưu role (OWNER / EDITOR / VIEWER), trạng thái mời (PENDING / ACCEPTED), thời điểm tham gia.
- **Transaction**: một khoản thu/chi thuộc về một Budget, do một Member tạo, có thể gắn nhiều Tag.
- **Tag / Category**: nhãn phân loại giao dịch (ăn uống, đi lại, hoá đơn...), quan hệ nhiều-nhiều với Transaction qua TransactionTag.

### 1.3. Tính năng chính

- Đăng ký / đăng nhập / đăng xuất, xác thực JWT với refresh token.
- Tạo Budget, mời thành viên qua email, chấp nhận/từ chối lời mời, đổi vai trò thành viên, rời nhóm.
- CRUD giao dịch thu/chi trong Budget, gắn nhãn (Tag), phân trang & sắp xếp theo ngày/số tiền/danh mục.
- Thống kê chi tiêu theo khoảng thời gian, theo thành viên, theo Tag.
- Phân quyền theo resource: OWNER toàn quyền, EDITOR thêm/sửa giao dịch, VIEWER chỉ xem — áp dụng đúng theo từng Budget, không phải quyền toàn cục.
- Gửi email xác nhận đăng ký, quên mật khẩu, mời tham gia Budget, cảnh báo chi tiêu vượt ngưỡng.

## 2. Các công nghệ sử dụng

| Nhóm | Công nghệ / Kỹ thuật cụ thể |
|---|---|
| Ngôn ngữ & Core | Java 21, Spring Boot 3.x, Maven |
| Persistence | Spring Data JPA + Hibernate, MySQL; Pagination & Sorting; Query Method; N+1 Fix (JOIN FETCH / @EntityGraph / batch-size); @Transactional (propagation, isolation, rollback rules) |
| Quan hệ dữ liệu | ManyToMany mô phỏng qua Entity trung gian: User–Budget qua BudgetMember (role, joinedAt, status); Transaction–Tag qua TransactionTag |
| RESTful API | MapStruct (DTO mapping), ApiResponse wrapper chuẩn hoá response, API Versioning (URI-based /api/v1) |
| Exception Handling | @RestControllerAdvice, @ExceptionHandler, custom Exception hierarchy (BusinessException, ResourceNotFoundException...), ErrorResponse class, xử lý MethodArgumentNotValidException |
| Security | Spring Security 6, JWT (access token + refresh token rotation), luồng Register/Login/Logout, RBAC (role toàn cục ADMIN/USER) kết hợp Resource-based Authorization (role theo từng Budget: OWNER/EDITOR/VIEWER), OAuth2 Login (Google) |
| Spring Mail | Xác nhận đăng ký (email verification token), quên mật khẩu (reset token có TTL), mời tham gia Budget, thông báo giao dịch bất thường — dùng template Thymeleaf |
| Redis | Cache (Cache-Aside: category, thống kê chi tiêu), lưu Refresh Token / Token Blacklist, Rate Limiting (login, invite, forgot-password) bằng AOP + Lua script |
| Testing | JUnit 5 + Mockito cho toàn bộ Service layer; Testcontainers (MySQL, Redis) cho Integration Test tối thiểu các luồng chính |
| Monitoring & Logging | Spring Boot Actuator (health, metrics cơ bản), SLF4J + Logback (structured logging, correlation id qua MDC) |
| Message Queue | Chưa triển khai ở bản đầu — để trống interface, bổ sung RabbitMQ/Kafka ở giai đoạn sau (ví dụ: gửi mail bất đồng bộ) |
| DevOps | Docker + Docker Compose (app, MySQL, Redis, Nginx), Nginx reverse proxy, CI/CD với GitHub Actions (build → test → build image → deploy) |

**Ghi chú:** bảng trên bám sát đúng danh sách công nghệ đã chọn, kèm ánh xạ cụ thể vào nghiệp vụ của BudgetShare để tránh triển khai hình thức (ví dụ: Rate Limiting áp cho login/invite/forgot-password thay vì áp chung chung mọi API).

## 3. Cấu trúc thư mục sơ bộ

Kiến trúc Monolith phân lớp cổ điển (Controller → Service → Repository), tách thêm các package theo nghiệp vụ cross-cutting (security, ratelimit, mail):

```
com.example.budgetshare
├── config/ → SecurityConfig, RedisConfig, MailConfig, OAuth2Config, OpenApiConfig
├── controller/
│   └── v1/ → BudgetController, TransactionController, AuthController, TagController...
├── dto/
│   ├── request/ → RegisterRequest, CreateBudgetRequest, InviteMemberRequest...
│   └── response/ → BudgetResponse, TransactionResponse, PageResponse...
├── entity/ → User, Budget, BudgetMember, Transaction, Tag, TransactionTag, RefreshToken
├── repository/ → JPA Repository + Query Method (findBy..., @EntityGraph, @Query JOIN FETCH)
├── service/
│   └── impl/ → BudgetServiceImpl, TransactionServiceImpl, AuthServiceImpl...
├── mapper/ → MapStruct mapper interfaces (BudgetMapper, TransactionMapper...)
├── security/ → JwtTokenProvider, JwtAuthFilter, CustomUserDetailsService, BudgetPermissionEvaluator (resource-based authorization)
├── exception/ → GlobalExceptionHandler, BusinessException, ErrorResponse
├── common/ → ApiResponse<T>, PageResponse<T>, Constants, ErrorCode enum
├── ratelimit/ → RateLimit annotation + AOP Aspect (Redis Lua script)
├── mail/ → MailService, templates (Thymeleaf: verify-email.html, reset-password.html, invite-member.html)
└── util/ → DateUtils, SecurityUtils (lấy current user từ SecurityContext)

src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
├── templates/mail/ → các file Thymeleaf ở trên
└── db/migration/ → Flyway script (V1__init.sql, V2__add_tag.sql...) (khuyến nghị dùng Flyway thay vì ddl-auto)

src/test/java/... → cấu trúc test mirror theo package service/ và controller/

docker/
├── Dockerfile
├── docker-compose.yml → app, mysql, redis, nginx
└── nginx/nginx.conf

.github/workflows/ci-cd.yml
```

## 4. Các bước để hoàn thiện đồ án

Thứ tự các bước được sắp xếp theo nguyên tắc: dựng xong phần lõi dữ liệu và API chuẩn trước, sau đó mới thêm bảo mật, rồi mới đến các tính năng phụ trợ (mail, cache) và cuối cùng là kiểm thử + vận hành. Làm theo đúng thứ tự tránh việc phải sửa lại API sau khi đã viết test.

### Giai đoạn 1 — Thiết kế dữ liệu

Vẽ ERD, định nghĩa Entity: User, Budget, BudgetMember, Transaction, Tag, TransactionTag, RefreshToken. Xác định rõ owning side của quan hệ ManyToMany-qua-entity-trung-gian.

### Giai đoạn 2 — CRUD nền tảng & JPA nâng cao

Repository với Query Method, Pagination & Sorting cho danh sách giao dịch. Viết lại các truy vấn load Budget kèm Member/Transaction bằng JOIN FETCH hoặc @EntityGraph để fix N+1 — nên bật log SQL và đếm số query trước/sau để chứng minh hiệu quả. Xác định ranh giới @Transactional cho các thao tác nhiều bước (ví dụ: tạo Budget + tự thêm OWNER vào BudgetMember).

### Giai đoạn 3 — Chuẩn hoá lớp REST API

Viết DTO cho từng use case, cấu hình MapStruct mapper. Thiết kế ApiResponse<T> wrapper thống nhất (status, message, data, timestamp). Áp dụng API Versioning ở mức URI (/api/v1/...).

### Giai đoạn 4 — Exception Handling toàn diện

Xây dựng hệ thống Exception tuỳ chỉnh theo domain (BudgetNotFoundException, InvalidBudgetRoleException...), GlobalExceptionHandler bắt tất cả, ErrorResponse có errorCode để FE phân biệt được loại lỗi, xử lý riêng lỗi validate của Bean Validation.

### Giai đoạn 5 — Bảo mật: Authentication & Authorization

Luồng Register (kèm email verify) → Login (JWT access + refresh) → Logout (thu hồi refresh token). Refresh Token nên lưu ở Redis kèm cơ chế rotation (mỗi lần refresh cấp token mới, token cũ bị vô hiệu) để chống replay. Sau đó dựng RBAC toàn cục (ADMIN/USER) và bổ sung Resource-based Authorization cho từng Budget (ví dụ dùng PermissionEvaluator hoặc custom AOP kiểm tra role trong BudgetMember trước khi cho thao tác). Cuối cùng tích hợp OAuth2 Login (Google) và map user OAuth2 vào cùng bảng User.

### Giai đoạn 6 — Spring Mail

Cấu hình SMTP, template Thymeleaf cho: xác nhận đăng ký (token TTL ngắn), quên mật khẩu (reset token dùng một lần), mời tham gia Budget (link accept-invite), cảnh báo chi tiêu vượt ngưỡng ngân sách.

### Giai đoạn 7 — Redis: Cache, Session, Rate Limiting

Cache-Aside cho dữ liệu đọc nhiều ít đổi (danh sách Tag, thống kê tổng hợp theo tháng). Lưu Refresh Token / token blacklist trong Redis. Rate Limiting bằng AOP + Lua script cho các API nhạy cảm: login, forgot-password, invite-member — tái sử dụng thuật toán đã làm ở SmartBank (Sliding Window Counter) thay vì làm lại từ đầu.

### Giai đoạn 8 — Testing

Unit test toàn bộ Service layer với JUnit 5 + Mockito (mock Repository, MailService). Bổ sung Integration Test cho các luồng quan trọng (đăng ký, tạo Budget, phân quyền sai bị chặn) dùng Testcontainers để test sát môi trường thật (MySQL, Redis) thay vì H2.

### Giai đoạn 9 — Monitoring & Logging

Bật Spring Boot Actuator (/health, /metrics cơ bản). Chuẩn hoá logging bằng SLF4J/Logback, thêm correlation id qua MDC để trace một request xuyên suốt các log.

### Giai đoạn 10 — DevOps

Viết Dockerfile multi-stage cho app, docker-compose.yml gồm app + MySQL + Redis + Nginx. Nginx đóng vai trò reverse proxy (và có thể làm luôn TLS termination). Dựng pipeline CI/CD với GitHub Actions: build → chạy test → build image → (tuỳ chọn) deploy.

### Giai đoạn 11 — Mở rộng sau (không bắt buộc ở bản đầu)

Tích hợp Message Queue (RabbitMQ/Kafka) để xử lý bất đồng bộ, ví dụ: publish event khi tạo giao dịch → consumer gửi mail cảnh báo, thay vì gọi MailService đồng bộ trong luồng chính.
