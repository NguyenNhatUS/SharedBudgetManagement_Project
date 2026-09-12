package com.nhat.SharedBudgetManagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity trung gian giữa {Transaction} và {Tag}.

 * Tương tự {BudgetMember}, dùng entity trung gian thay vì {@ManyToMany} thuần tuý
 * để có thể mở rộng thêm field nghiệp vụ trong tương lai (ví dụ: ai gắn tag, thời điểm gắn).
 * Đây là owning side của cả hai quan hệ ManyToOne (Transaction, Tag).
 * Composite unique constraint trên (transaction_id, tag_id) đảm bảo mỗi tag
 * chỉ được gắn một lần cho mỗi giao dịch.

 */
@Entity
@Table(name = "transaction_tags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_tag", columnNames = {"transaction_id", "tag_id"})
}, indexes = {
        @Index(name = "idx_tt_transaction", columnList = "transaction_id"),
        @Index(name = "idx_tt_tag", columnList = "tag_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
