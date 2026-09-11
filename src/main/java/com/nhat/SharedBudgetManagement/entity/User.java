package com.nhat.SharedBudgetManagement.entity;

import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho tài khoản người dùng.
 * <p>
 * Một User có thể tham gia nhiều Budget thông qua entity trung gian {@link BudgetMember}.
 * Role toàn cục (ADMIN/USER) được quản lý qua {@link UserRole},
 * còn role theo từng Budget được quản lý trong {@link BudgetMember}.
 * </p>
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Mật khẩu đã mã hoá (BCrypt). Nullable khi user đăng nhập qua OAuth2.
     */
    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String fullName;

    @Column(length = 500)
    private String avatarUrl;

    /**
     * Phương thức xác thực: LOCAL (email/password) hoặc GOOGLE (OAuth2).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * ID từ nhà cung cấp OAuth2 (Google sub). Null khi LOCAL.
     */
    @Column(length = 255)
    private String providerId;

    /**
     * Vai trò toàn cục trong hệ thống (ADMIN / USER).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    /**
     * Email đã xác thực hay chưa. Mặc định false khi đăng ký LOCAL.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Token dùng để xác thực email (TTL ngắn). Null sau khi verify thành công.
     */
    @Column(length = 255)
    private String emailVerificationToken;

    /**
     * Token dùng để reset mật khẩu (dùng một lần). Null khi chưa yêu cầu.
     */
    @Column(length = 255)
    private String passwordResetToken;

    /**
     * Thời điểm hết hạn của passwordResetToken.
     */
    private LocalDateTime passwordResetTokenExpiry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ====================== Relationships ======================

    /**
     * Danh sách tư cách thành viên (Budget mà user tham gia).
     * mappedBy = "user" → BudgetMember là owning side.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetMember> budgetMemberships = new ArrayList<>();

    /**
     * Danh sách refresh token đang hiệu lực của user.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    // ====================== Lifecycle callbacks ======================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
