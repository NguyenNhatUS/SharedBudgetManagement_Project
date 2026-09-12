package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Transaction;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TransactionService {

    Transaction createTransaction(Long budgetId, Long userId, TransactionType type,
                                  BigDecimal amount, String description, String note,
                                  LocalDate transactionDate, List<Long> tagIds);

    Transaction updateTransaction(Long transactionId, TransactionType type,
                                  BigDecimal amount, String description, String note,
                                  LocalDate transactionDate, List<Long> tagIds);

    void deleteTransaction(Long transactionId);

    Transaction getTransactionById(Long transactionId);

    Page<Transaction> getTransactionsByBudget(Long budgetId, Pageable pageable);

    Page<Transaction> getTransactionsByBudgetAndType(Long budgetId, TransactionType type, Pageable pageable);

    Page<Transaction> getTransactionsByDateRange(Long budgetId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Thống kê tổng thu/chi theo Budget.
     * @return Map với key là TransactionType, value là tổng số tiền
     */
    Map<TransactionType, BigDecimal> getSummaryByBudget(Long budgetId);

    /**
     * Thống kê chi tiêu theo thành viên.
     * @return Danh sách [userId, fullName, totalExpense]
     */
    List<Object[]> getExpenseByMember(Long budgetId);
}
