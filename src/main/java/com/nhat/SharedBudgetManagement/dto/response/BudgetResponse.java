package com.nhat.SharedBudgetManagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private Long id;
    private String name;
    private String description;
    private String currency;
    private UserResponse createdBy;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
