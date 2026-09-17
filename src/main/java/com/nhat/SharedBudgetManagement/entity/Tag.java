package com.nhat.SharedBudgetManagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho nhãn / danh mục phân loại giao dịch (ăn uống, đi lại, hoá đơn...).
 * Quan hệ nhiều-nhiều với {Transaction} thông qua entity trung gian {TransactionTag}.
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
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;


    @Column(length = 7)
    private String color;


    @Column(length = 50)
    private String icon;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @JsonIgnore
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionTag> transactionTags = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
