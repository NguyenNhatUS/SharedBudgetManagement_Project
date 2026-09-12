package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<Budget> findWithMembersById(Long id);


    @EntityGraph(attributePaths = {"transactions", "transactions.createdBy"})
    Optional<Budget> findWithTransactionsById(Long id);


    @Query("SELECT b FROM Budget b " +
           "JOIN FETCH b.members m " +
           "JOIN FETCH m.user " +
           "WHERE b.id = :id")
    Optional<Budget> findByIdFetchMembers(Long id);


     // Tìm tất cả Budget mà user đang tham gia (status = ACCEPTED).
    @Query("SELECT b FROM Budget b " +
           "JOIN b.members m " +
           "WHERE m.user.id = :userId AND m.status = 'ACCEPTED'")
    List<Budget> findAllByMemberUserId(Long userId);
}
