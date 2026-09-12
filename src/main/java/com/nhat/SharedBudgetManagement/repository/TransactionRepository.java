package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.Transaction;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ====================== Pagination & Sorting ======================

    /**
     * Danh sách giao dịch theo Budget, có phân trang & sắp xếp.
     * FE gửi: ?page=0&size=20&sort=transactionDate,desc
     */
    Page<Transaction> findAllByBudgetId(Long budgetId, Pageable pageable);

    /**
     * Lọc theo loại giao dịch (INCOME/EXPENSE) + pagination.
     */
    Page<Transaction> findAllByBudgetIdAndType(Long budgetId, TransactionType type, Pageable pageable);

    /**
     * Lọc theo khoảng thời gian + pagination.
     */
    Page<Transaction> findAllByBudgetIdAndTransactionDateBetween(
            Long budgetId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // ====================== JOIN FETCH (fix N+1) ======================

    /**
     * Load danh sách giao dịch kèm tags (fix N+1 khi hiển thị danh sách).
     */
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN FETCH t.transactionTags tt " +
           "LEFT JOIN FETCH tt.tag " +
           "WHERE t.budget.id = :budgetId")
    List<Transaction> findAllByBudgetIdFetchTags(@Param("budgetId") Long budgetId);

    // ====================== Thống kê ======================

    /**
     * Tổng thu/chi theo Budget, group by type.
     */
    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
           "WHERE t.budget.id = :budgetId " +
           "GROUP BY t.type")
    List<Object[]> sumAmountByBudgetIdGroupByType(@Param("budgetId") Long budgetId);

    /**
     * Tổng chi theo thành viên trong Budget.
     */
    @Query("SELECT m.user.id, m.user.fullName, SUM(t.amount) FROM Transaction t " +
           "JOIN t.createdBy m " +
           "WHERE t.budget.id = :budgetId AND t.type = 'EXPENSE' " +
           "GROUP BY m.user.id, m.user.fullName")
    List<Object[]> sumExpenseByBudgetIdGroupByMember(@Param("budgetId") Long budgetId);

    /**
     * Tổng chi trong khoảng thời gian (dùng cho cảnh báo vượt ngưỡng spending limit).
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.budget.id = :budgetId AND t.type = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseByBudgetIdAndDateRange(
            @Param("budgetId") Long budgetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
