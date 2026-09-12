package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.TransactionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTagRepository extends JpaRepository<TransactionTag, Long> {

    List<TransactionTag> findAllByTransactionId(Long transactionId);

    void deleteAllByTransactionId(Long transactionId);

    boolean existsByTransactionIdAndTagId(Long transactionId, Long tagId);
}
