package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetMemberRepository extends JpaRepository<BudgetMember, Long> {

    Optional<BudgetMember> findByUserIdAndBudgetId(Long userId, Long budgetId);

    List<BudgetMember> findAllByBudgetId(Long budgetId);

    Page<BudgetMember> findAllByBudgetId(Long budgetId, Pageable pageable);

    List<BudgetMember> findAllByUserId(Long userId);

    List<BudgetMember> findAllByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByUserIdAndBudgetId(Long userId, Long budgetId);

    Optional<BudgetMember> findByInviteToken(String inviteToken);


    long countByBudgetIdAndStatus(Long budgetId, MemberStatus status);
}
