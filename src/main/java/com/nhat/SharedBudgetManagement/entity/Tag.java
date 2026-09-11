package com.nhat.SharedBudgetManagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho nhãn / danh mục phân loại giao dịch (ăn uống, đi lại, hoá đơn...).
 * <p>
 * Quan hệ nhiều-nhiều với {@link Transaction} thông qua entity trung gian {@link TransactionTag}.
 * </p>
 */
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Mã màu hex để hiển thị UI (ví dụ: #FF6B6B). Tuỳ chọn.
     */
    @Column(length = 7)
    private String color;

    /**
     * Icon name (ví dụ: "utensils", "car", "receipt"). Tuỳ chọn.
     */
    @Column(length = 50)
    private String icon;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ====================== Relationships ======================

    /**
     * Danh sách liên kết với Transaction qua TransactionTag.
     * mappedBy = "tag" → TransactionTag là owning side.
     */
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionTag> transactionTags = new ArrayList<>();

    // ====================== Lifecycle callbacks ======================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
