package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetMemberRepository extends JpaRepository<BudgetMember, Long> {

    Optional<BudgetMember> findByUserIdAndBudgetId(Long userId, Long budgetId);

    List<BudgetMember> findAllByBudgetId(Long budgetId);

    List<BudgetMember> findAllByUserId(Long userId);

    List<BudgetMember> findAllByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByUserIdAndBudgetId(Long userId, Long budgetId);

    Optional<BudgetMember> findByInviteToken(String inviteToken);

    /**
     * Đếm số thành viên ACCEPTED trong 1 Budget.
     */
    long countByBudgetIdAndStatus(Long budgetId, MemberStatus status);
}
