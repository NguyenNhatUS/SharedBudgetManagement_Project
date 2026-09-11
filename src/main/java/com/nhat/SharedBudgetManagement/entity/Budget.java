package com.nhat.SharedBudgetManagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một ngân sách chung (gia đình, nhóm bạn, du lịch...).
 * <p>
 * Một Budget được tạo bởi một User (trở thành OWNER) và có thể có nhiều thành viên
 * thông qua entity trung gian {@link BudgetMember}. Mỗi Budget chứa nhiều {@link Transaction}.
 * </p>
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Đơn vị tiền tệ (VND, USD...). Mặc định VND.
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Ngưỡng chi tiêu để cảnh báo (gửi mail khi tổng chi vượt ngưỡng).
     * Null nếu không thiết lập cảnh báo.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal spendingLimit;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ====================== Relationships ======================

    /**
     * Người tạo Budget (trở thành OWNER).
     * Quan hệ ManyToOne — nhiều Budget có thể do cùng một User tạo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Danh sách thành viên của Budget.
     * mappedBy = "budget" → BudgetMember là owning side.
     */
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetMember> members = new ArrayList<>();

    /**
     * Danh sách giao dịch thuộc Budget này.
     */
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

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
