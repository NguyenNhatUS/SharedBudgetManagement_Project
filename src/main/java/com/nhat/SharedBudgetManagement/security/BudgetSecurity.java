package com.nhat.SharedBudgetManagement.security;

import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;


@Slf4j
@Component("budgetSecurity")
@RequiredArgsConstructor
public class BudgetSecurity {

    private final BudgetMemberRepository budgetMemberRepository;

    public boolean isOwner(Long budgetId) {
        if (budgetId == null) {
            return false;
        }

        if(SecurityUtils.isAdmin()) {
            return true;
        }

        return getMembership(budgetId)
                .map(m -> m.getRole() == BudgetRole.OWNER)
                .orElse(false);
    }

    // OWNER or EDITOR
    public boolean canEdit(Long budgetId) {
        if (budgetId == null) {
            return false;
        }
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return getMembership(budgetId)
                .map(m -> m.getRole() == BudgetRole.OWNER || m.getRole() == BudgetRole.EDITOR)
                .orElse(false);
    }

    // OWNER, EDITOR or VIEWER
    public boolean canView(Long budgetId) {
        if (budgetId == null) {
            return false;
        }
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return getMembership(budgetId).isPresent();
    }


    public boolean hasRole(Long budgetId, String roleName) {
        if (budgetId == null || roleName == null) {
            return false;
        }
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        try {
            BudgetRole expectedRole = BudgetRole.valueOf(roleName.toUpperCase());
            return getMembership(budgetId)
                    .map(m -> m.getRole() == expectedRole)
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid budget role check: {}", roleName);
            return false;
        }
    }

    /**
     * Lấy thông tin thành viên hợp lệ (trạng thái ACCEPTED) của user hiện tại trong budget.
     */
    private Optional<BudgetMember> getMembership(Long budgetId) {
        Optional<Long> currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return Optional.empty();
        }

        return budgetMemberRepository.findByUserIdAndBudgetId(currentUserId.get(), budgetId)
                .filter(m -> m.getStatus() == MemberStatus.ACCEPTED);
    }
}