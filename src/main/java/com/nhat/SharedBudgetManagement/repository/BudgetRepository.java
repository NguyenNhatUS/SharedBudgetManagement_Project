package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Load Budget kèm danh sách members và user của từng member (fix N+1).
     */
    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<Budget> findWithMembersById(Long id);

    /**
     * Load Budget kèm danh sách transactions và người tạo (fix N+1).
     */
    @EntityGraph(attributePaths = {"transactions", "transactions.createdBy"})
    Optional<Budget> findWithTransactionsById(Long id);

    /**
     * Alternative: JOIN FETCH load Budget + members + user.
     */
    @Query("SELECT b FROM Budget b " +
           "JOIN FETCH b.members m " +
           "JOIN FETCH m.user " +
           "WHERE b.id = :id")
    Optional<Budget> findByIdFetchMembers(@Param("id") Long id);

    /**
     * Tìm tất cả Budget mà user đang tham gia (status = ACCEPTED).
     */
    @Query("SELECT b FROM Budget b " +
           "JOIN b.members m " +
           "WHERE m.user.id = :userId AND m.status = 'ACCEPTED'")
    List<Budget> findAllByMemberUserId(@Param("userId") Long userId);
}
