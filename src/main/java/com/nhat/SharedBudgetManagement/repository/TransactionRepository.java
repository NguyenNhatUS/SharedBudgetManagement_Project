package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.Transaction;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findAllByBudgetId(Long budgetId, Pageable pageable);


    Page<Transaction> findAllByBudgetIdAndType(Long budgetId, TransactionType type, Pageable pageable);


    Page<Transaction> findAllByBudgetIdAndTransactionDateBetween(
            Long budgetId, LocalDate startDate, LocalDate endDate, Pageable pageable);



    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN FETCH t.transactionTags tt " +
           "LEFT JOIN FETCH tt.tag " +
           "WHERE t.budget.id = :budgetId")
    List<Transaction> findAllByBudgetId(Long budgetId);

    // ====================== Thống kê ======================

    /**
     * Tổng thu/chi theo Budget, group by type.
     */
    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
           "WHERE t.budget.id = :budgetId " +
           "GROUP BY t.type")
    List<Object[]> sumAmountByBudgetIdGroupByType(Long budgetId);

    /**
     * Tổng chi theo thành viên trong Budget.
     */
    @Query("SELECT m.user.id, m.user.fullName, SUM(t.amount) FROM Transaction t " +
           "JOIN t.createdBy m " +
           "WHERE t.budget.id = :budgetId AND t.type = 'EXPENSE' " +
           "GROUP BY m.user.id, m.user.fullName")
    List<Object[]> sumExpenseByBudgetIdGroupByMember(Long budgetId);
}