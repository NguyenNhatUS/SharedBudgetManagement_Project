package com.nhat.SharedBudgetManagement.entity;

import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity trung gian giữa {@link User} và {@link Budget}.
 * <p>
 * Thay vì dùng {@code @ManyToMany} thuần tuý, dùng entity trung gian này để lưu thêm
 * nghiệp vụ: vai trò trong Budget (OWNER/EDITOR/VIEWER), trạng thái lời mời
 * (PENDING/ACCEPTED/DECLINED), và thời điểm tham gia.
 * </p>
 * <p>
 * Đây là owning side của cả hai quan hệ ManyToOne (User, Budget).
 * Composite unique constraint trên (user_id, budget_id) đảm bảo mỗi user
 * chỉ có một tư cách thành viên trong mỗi Budget.
 * </p>
 */
@Entity
@Table(name = "budget_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_budget_member", columnNames = {"user_id", "budget_id"})
}, indexes = {
        @Index(name = "idx_bm_user", columnList = "user_id"),
        @Index(name = "idx_bm_budget", columnList = "budget_id"),
        @Index(name = "idx_bm_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User tham gia Budget.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Budget mà user tham gia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    /**
     * Vai trò của thành viên trong Budget cụ thể này (Resource-based Authorization).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetRole role;

    /**
     * Trạng thái lời mời: PENDING khi mới mời, ACCEPTED khi chấp nhận, DECLINED khi từ chối.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.PENDING;

    /**
     * Token mời tham gia (gửi qua email). Null sau khi đã xử lý lời mời.
     */
    @Column(length = 255)
    private String inviteToken;

    /**
     * Thời điểm tham gia (chấp nhận lời mời). Null khi còn PENDING.
     */
    private LocalDateTime joinedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ====================== Lifecycle callbacks ======================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
