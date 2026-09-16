package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BudgetService {

    Budget createBudget(Long userId, String name, String description, String currency);

    Budget getBudgetById(Long budgetId);

    Budget getBudgetWithMembers(Long budgetId);

    List<Budget> getAllBudgetsByUserId(Long userId);

    Page<Budget> getAllBudgetsByUserId(Long userId, Pageable pageable);

    Budget updateBudget(Long budgetId, String name, String description, String currency);

    void deleteBudget(Long budgetId);
}
