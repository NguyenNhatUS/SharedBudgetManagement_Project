package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BudgetMemberService {

    BudgetMember inviteMember(Long budgetId, String email, BudgetRole role);

    BudgetMember acceptInvite(String inviteToken);

    BudgetMember declineInvite(String inviteToken);

    BudgetMember changeMemberRole(Long budgetId, Long userId, BudgetRole newRole);

    void removeMember(Long budgetId, Long userId);

    void leaveBudget(Long budgetId, Long userId);

    List<BudgetMember> getMembersByBudgetId(Long budgetId);

    Page<BudgetMember> getMembersByBudgetId(Long budgetId, Pageable pageable);

    BudgetRole getMemberRole(Long budgetId, Long userId);
}
