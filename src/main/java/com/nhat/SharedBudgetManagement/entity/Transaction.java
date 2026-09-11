package com.nhat.SharedBudgetManagement.entity;

import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một khoản thu/chi thuộc về một {@link Budget}.
 * <p>
 * Mỗi Transaction do một {@link BudgetMember} tạo (phải có quyền OWNER hoặc EDITOR).
 * Có thể gắn nhiều {@link Tag} thông qua entity trung gian {@link TransactionTag}.
 * </p>
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_budget", columnList = "budget_id"),
        @Index(name = "idx_txn_created_by", columnList = "created_by"),
        @Index(name = "idx_txn_date", columnList = "transaction_date"),
        @Index(name = "idx_txn_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Loại giao dịch: INCOME (thu) hoặc EXPENSE (chi).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * Số tiền giao dịch. Luôn dương — type quyết định thu hay chi.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Mô tả ngắn về giao dịch.
     */
    @Column(nullable = false, length = 255)
    private String description;

    /**
     * Ghi chú chi tiết (tuỳ chọn).
     */
    @Column(length = 1000)
    private String note;

    /**
     * Ngày thực hiện giao dịch (do user chọn, có thể khác ngày tạo record).
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ====================== Relationships ======================

    /**
     * Budget mà giao dịch thuộc về.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    /**
     * Thành viên (BudgetMember) đã tạo giao dịch này.
     * Lưu theo BudgetMember thay vì User để biết chính xác user nào, trong vai trò nào,
     * đã tạo giao dịch trong Budget nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private BudgetMember createdBy;

    /**
     * Danh sách tag gắn với giao dịch.
     * mappedBy = "transaction" → TransactionTag là owning side.
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionTag> transactionTags = new ArrayList<>();

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
