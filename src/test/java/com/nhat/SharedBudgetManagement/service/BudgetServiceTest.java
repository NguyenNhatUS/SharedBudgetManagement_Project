package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import com.nhat.SharedBudgetManagement.repository.BudgetRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.impl.BudgetServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BudgetService")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMemberRepository budgetMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User testUser;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .fullName("Budget Owner")
                .build();

        testBudget = Budget.builder()
                .id(10L)
                .name("Family Monthly Budget")
                .description("Household expenses")
                .currency("VND")
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createBudget - Success: should create budget and register creator as OWNER")
    void createBudget_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        Budget result = budgetService.createBudget(1L, "Family Monthly Budget", "Household expenses", "VND");

        assertNotNull(result);
        assertEquals("Family Monthly Budget", result.getName());
        assertEquals("VND", result.getCurrency());
        verify(budgetRepository).save(any(Budget.class));
        verify(budgetMemberRepository).save(argThat(member ->
                member.getRole() == BudgetRole.OWNER &&
                member.getStatus() == MemberStatus.ACCEPTED &&
                member.getUser().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("createBudget - User Not Found: should throw ResourceNotFoundException")
    void createBudget_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.createBudget(99L, "Test Budget", "Desc", "USD"));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("getBudgetById - Success: should return budget when ID exists")
    void getBudgetById_success() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));

        Budget result = budgetService.getBudgetById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Family Monthly Budget", result.getName());
    }

    @Test
    @DisplayName("getBudgetById - Not Found: should throw ResourceNotFoundException")
    void getBudgetById_notFound() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.getBudgetById(99L));
    }

    @Test
    @DisplayName("getBudgetWithMembers - Success: should return budget with loaded members")
    void getBudgetWithMembers_success() {
        when(budgetRepository.findWithMembersById(10L)).thenReturn(Optional.of(testBudget));

        Budget result = budgetService.getBudgetWithMembers(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(budgetRepository).findWithMembersById(10L);
    }

    @Test
    @DisplayName("getAllBudgetsByUserId - Success: should return user budgets list")
    void getAllBudgetsByUserId_list_success() {
        when(budgetRepository.findAllByMemberUserId(1L)).thenReturn(List.of(testBudget));

        List<Budget> result = budgetService.getAllBudgetsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    @DisplayName("getAllBudgetsByUserId (Pageable) - Success: should return paged user budgets")
    void getAllBudgetsByUserId_pageable_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Budget> page = new PageImpl<>(List.of(testBudget), pageable, 1);
        when(budgetRepository.findAllByMemberUserId(1L, pageable)).thenReturn(page);

        Page<Budget> result = budgetService.getAllBudgetsByUserId(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("updateBudget - Success: should update budget fields")
    void updateBudget_success() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        Budget result = budgetService.updateBudget(10L, "New Name", "New Desc", "USD");

        assertNotNull(result);
        assertEquals("New Name", testBudget.getName());
        assertEquals("New Desc", testBudget.getDescription());
        assertEquals("USD", testBudget.getCurrency());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    @DisplayName("deleteBudget - Success: should delete existing budget")
    void deleteBudget_success() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));

        budgetService.deleteBudget(10L);

        verify(budgetRepository).delete(testBudget);
    }
}
