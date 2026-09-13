package com.nhat.SharedBudgetManagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ Refresh Token cho luồng JWT authentication.
 * <p>
 * Mỗi User có thể có nhiều refresh token (đăng nhập từ nhiều thiết bị).
 * Khi refresh, áp dụng cơ chế token rotation: cấp token mới, thu hồi token cũ
 * (đánh dấu {@code revoked = true}) để chống replay attack.
 * </p>
 * <p>
 * <b>Lưu ý:</b> Trong môi trường production, refresh token nên được lưu ở Redis
 * kèm TTL để tự động hết hạn. Entity này phục vụ cho persistence layer ban đầu
 * và có thể chuyển sang Redis ở giai đoạn 7.
 * </p>
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_token", columnList = "token", unique = true),
        @Index(name = "idx_rt_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 255)
    private String token;


    @Column(nullable = false)
    private LocalDateTime expiryDate;


    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra token có còn hiệu lực không (chưa hết hạn và chưa bị thu hồi).
     */
    public boolean isValid() {
        return !this.revoked && this.expiryDate.isAfter(LocalDateTime.now());
    }
}
