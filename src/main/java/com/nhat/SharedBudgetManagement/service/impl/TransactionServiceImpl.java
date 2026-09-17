package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.config.RedisConfig;
import com.nhat.SharedBudgetManagement.entity.*;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import com.nhat.SharedBudgetManagement.exception.ForbiddenException;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.*;
import com.nhat.SharedBudgetManagement.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetMemberRepository budgetMemberRepository;
    private final TagRepository tagRepository;
    private final TransactionTagRepository transactionTagRepository;

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_BUDGET_SUMMARY, RedisConfig.CACHE_BUDGET_MEMBER_EXPENSE}, key = "#budgetId")
    public Transaction createTransaction(Long budgetId, Long userId, TransactionType type,
            BigDecimal amount, String description, String note,
            LocalDate transactionDate, List<Long> tagIds) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUDGET_NOT_FOUND,
                        "Budget not found with id: " + budgetId));

        BudgetMember member = budgetMemberRepository.findByUserIdAndBudgetId(userId, budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
                        "User is not a member of this budget"));

        if (member.getStatus() != MemberStatus.ACCEPTED) {
            throw new ForbiddenException(ErrorCode.MEMBERSHIP_NOT_ACCEPTED, "User's membership is not ACCEPTED");
        }

        Transaction transaction = Transaction.builder()
                .type(type)
                .amount(amount)
                .description(description)
                .note(note)
                .transactionDate(transactionDate)
                .budget(budget)
                .createdBy(member)
                .build();
        transactionRepository.save(transaction);

        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TAG_NOT_FOUND,
                                "Tag not found with id: " + tagId));

                TransactionTag transactionTag = TransactionTag.builder()
                        .transaction(transaction)
                        .tag(tag)
                        .build();
                transactionTagRepository.save(transactionTag);
            }
        }

        return transaction;
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_BUDGET_SUMMARY, RedisConfig.CACHE_BUDGET_MEMBER_EXPENSE}, key = "#result.budget.id")
    public Transaction updateTransaction(Long transactionId, TransactionType type,
            BigDecimal amount, String description, String note,
            LocalDate transactionDate, List<Long> tagIds) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction not found with id: " + transactionId));

        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setNote(note);
        transaction.setTransactionDate(transactionDate);

        transactionTagRepository.deleteAllByTransactionId(transactionId);

        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TAG_NOT_FOUND,
                                "Tag not found with id: " + tagId));

                TransactionTag transactionTag = TransactionTag.builder()
                        .transaction(transaction)
                        .tag(tag)
                        .build();
                transactionTagRepository.save(transactionTag);
            }
        }

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_BUDGET_SUMMARY, RedisConfig.CACHE_BUDGET_MEMBER_EXPENSE}, allEntries = true)
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction not found with id: " + transactionId));
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction not found with id: " + transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByBudget(Long budgetId, Pageable pageable) {
        return transactionRepository.findAllByBudgetId(budgetId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByBudgetAndType(Long budgetId, TransactionType type, Pageable pageable) {
        return transactionRepository.findAllByBudgetIdAndType(budgetId, type, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByDateRange(Long budgetId, LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return transactionRepository.findAllByBudgetIdAndTransactionDateBetween(budgetId, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_BUDGET_SUMMARY, key = "#budgetId")
    public Map<TransactionType, BigDecimal> getSummaryByBudget(Long budgetId) {
        List<Object[]> results = transactionRepository.sumAmountByBudgetIdGroupByType(budgetId);
        Map<TransactionType, BigDecimal> summary = new HashMap<>();
        summary.put(TransactionType.INCOME, BigDecimal.ZERO);
        summary.put(TransactionType.EXPENSE, BigDecimal.ZERO);

        for (Object[] row : results) {
            TransactionType type = (TransactionType) row[0];
            BigDecimal total = (BigDecimal) row[1];
            summary.put(type, total);
        }
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_BUDGET_MEMBER_EXPENSE, key = "#budgetId")
    public List<Object[]> getExpenseByMember(Long budgetId) {
        return transactionRepository.sumExpenseByBudgetIdGroupByMember(budgetId);
    }
}
