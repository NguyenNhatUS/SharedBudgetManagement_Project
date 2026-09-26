package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.*;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import com.nhat.SharedBudgetManagement.exception.ForbiddenException;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.*;
import com.nhat.SharedBudgetManagement.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for TransactionService")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMemberRepository budgetMemberRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TransactionTagRepository transactionTagRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Budget testBudget;
    private User testUser;
    private BudgetMember testMember;
    private Tag testTag;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testBudget = Budget.builder().id(10L).name("Test Budget").build();
        testUser = User.builder().id(1L).email("user@example.com").build();
        testMember = BudgetMember.builder()
                .id(100L)
                .budget(testBudget)
                .user(testUser)
                .role(BudgetRole.OWNER)
                .status(MemberStatus.ACCEPTED)
                .build();
        testTag = Tag.builder().id(5L).name("Food").build();
        testTransaction = Transaction.builder()
                .id(50L)
                .budget(testBudget)
                .createdBy(testMember)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("150000"))
                .description("Lunch")
                .transactionDate(LocalDate.now())
                .transactionTags(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("createTransaction - Success: should create transaction with tags")
    void createTransaction_success() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L)).thenReturn(Optional.of(testMember));
        when(tagRepository.findById(5L)).thenReturn(Optional.of(testTag));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(50L);
            return t;
        });

        Transaction result = transactionService.createTransaction(
                10L, 1L, TransactionType.EXPENSE,
                new BigDecimal("150000"), "Lunch", "Good food",
                LocalDate.now(), List.of(5L));

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals(TransactionType.EXPENSE, result.getType());
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionTagRepository).save(any(TransactionTag.class));
    }

    @Test
    @DisplayName("createTransaction - Budget Not Found: should throw ResourceNotFoundException")
    void createTransaction_budgetNotFound() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(
                99L, 1L, TransactionType.EXPENSE,
                BigDecimal.TEN, "Desc", null, LocalDate.now(), null));
    }

    @Test
    @DisplayName("createTransaction - Membership Not Accepted: should throw ForbiddenException")
    void createTransaction_membershipNotAccepted() {
        testMember.setStatus(MemberStatus.PENDING);
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L)).thenReturn(Optional.of(testMember));

        assertThrows(ForbiddenException.class, () -> transactionService.createTransaction(
                10L, 1L, TransactionType.EXPENSE,
                BigDecimal.TEN, "Desc", null, LocalDate.now(), null));
    }

    @Test
    @DisplayName("updateTransaction - Success: should update transaction and replace tags")
    void updateTransaction_success() {
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(testTransaction));
        when(tagRepository.findById(5L)).thenReturn(Optional.of(testTag));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        Transaction result = transactionService.updateTransaction(
                50L, TransactionType.INCOME, new BigDecimal("200000"),
                "Bonus", "Work bonus", LocalDate.now(), List.of(5L));

        assertNotNull(result);
        assertEquals(TransactionType.INCOME, testTransaction.getType());
        assertEquals(new BigDecimal("200000"), testTransaction.getAmount());
        verify(transactionTagRepository).deleteAllByTransactionId(50L);
        verify(transactionTagRepository).save(any(TransactionTag.class));
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    @DisplayName("deleteTransaction - Success: should delete transaction")
    void deleteTransaction_success() {
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(testTransaction));

        transactionService.deleteTransaction(50L);

        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    @DisplayName("deleteTransaction - Not Found: should throw ResourceNotFoundException")
    void deleteTransaction_notFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(99L));
    }

    @Test
    @DisplayName("getSummaryByBudget - Success: should return map of INCOME and EXPENSE totals")
    void getSummaryByBudget_success() {
        List<Object[]> queryResults = new ArrayList<>();
        queryResults.add(new Object[] { TransactionType.EXPENSE, new BigDecimal("350000") });
        queryResults.add(new Object[] { TransactionType.INCOME, new BigDecimal("1000000") });

        when(transactionRepository.sumAmountByBudgetIdGroupByType(10L)).thenReturn(queryResults);

        Map<TransactionType, BigDecimal> summary = transactionService.getSummaryByBudget(10L);

        assertNotNull(summary);
        assertEquals(new BigDecimal("1000000"), summary.get(TransactionType.INCOME));
        assertEquals(new BigDecimal("350000"), summary.get(TransactionType.EXPENSE));
    }

    @Test
    @DisplayName("getExpenseByMember - Success: should return expense grouped by member")
    void getExpenseByMember_success() {
        List<Object[]> mockList = new ArrayList<>();
        mockList.add(new Object[] { "John Doe", new BigDecimal("500000") });
        when(transactionRepository.sumExpenseByBudgetIdGroupByMember(10L)).thenReturn(mockList);

        List<Object[]> result = transactionService.getExpenseByMember(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getTransactionsByBudget - Success: should return paged transactions")
    void getTransactionsByBudget_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(testTransaction), pageable, 1);
        when(transactionRepository.findAllByBudgetId(10L, pageable)).thenReturn(page);

        Page<Transaction> result = transactionService.getTransactionsByBudget(10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}