package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import com.nhat.SharedBudgetManagement.repository.BudgetRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.BudgetService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMemberRepository budgetMemberRepository;
    private final UserRepository userRepository;

    /**
     * Tạo Budget + tự tạo BudgetMember OWNER cho user (2 bước trong 1 transaction).
     * Nếu bất kỳ bước nào fail → rollback toàn bộ.
     */
    @Override
    @Transactional
    public Budget createBudget(Long userId, String name, String description, String currency, BigDecimal spendingLimit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Bước 1: Tạo Budget
        Budget budget = Budget.builder()
                .name(name)
                .description(description)
                .currency(currency != null ? currency : "VND")
                .spendingLimit(spendingLimit)
                .createdBy(user)
                .build();
        budgetRepository.save(budget);

        // Bước 2: Tạo BudgetMember OWNER (cùng transaction — rollback nếu fail)
        BudgetMember owner = BudgetMember.builder()
                .user(user)
                .budget(budget)
                .role(BudgetRole.OWNER)
                .status(MemberStatus.ACCEPTED) // OWNER không cần mời
                .joinedAt(LocalDateTime.now())
                .build();
        budgetMemberRepository.save(owner);

        return budget;
    }

    @Override
    @Transactional(readOnly = true)
    public Budget getBudgetById(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
    }

    /**
     * Load Budget kèm members (fix N+1 bằng @EntityGraph).
     */
    @Override
    @Transactional(readOnly = true)
    public Budget getBudgetWithMembers(Long budgetId) {
        return budgetRepository.findWithMembersById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> getAllBudgetsByUserId(Long userId) {
        return budgetRepository.findAllByMemberUserId(userId);
    }

    @Override
    @Transactional
    public Budget updateBudget(Long budgetId, String name, String description, String currency, BigDecimal spendingLimit) {
        Budget budget = getBudgetById(budgetId);
        budget.setName(name);
        budget.setDescription(description);
        if (currency != null) {
            budget.setCurrency(currency);
        }
        budget.setSpendingLimit(spendingLimit);
        return budgetRepository.save(budget);
    }

    /**
     * Xoá Budget (cascade xoá members + transactions).
     */
    @Override
    @Transactional
    public void deleteBudget(Long budgetId) {
        Budget budget = getBudgetById(budgetId);
        budgetRepository.delete(budget);
    }
}
