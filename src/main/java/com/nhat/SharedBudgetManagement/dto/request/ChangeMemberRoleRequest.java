package com.nhat.SharedBudgetManagement.dto.request;

import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeMemberRoleRequest {

    @NotNull(message = "Role is required")
    private BudgetRole role;
}
