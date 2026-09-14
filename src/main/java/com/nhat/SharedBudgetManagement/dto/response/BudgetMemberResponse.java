package com.nhat.SharedBudgetManagement.dto.response;

import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetMemberResponse {
    private Long id;
    private UserResponse user;
    private BudgetRole role;
    private MemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
}
