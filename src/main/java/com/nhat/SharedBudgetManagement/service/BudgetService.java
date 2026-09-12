package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Budget;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService {

    Budget createBudget(Long userId, String name, String description, String currency, BigDecimal spendingLimit);

    Budget getBudgetById(Long budgetId);

    Budget getBudgetWithMembers(Long budgetId);

    List<Budget> getAllBudgetsByUserId(Long userId);

    Budget updateBudget(Long budgetId, String name, String description, String currency, BigDecimal spendingLimit);

    void deleteBudget(Long budgetId);
}
