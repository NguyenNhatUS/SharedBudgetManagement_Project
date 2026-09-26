package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.exception.BadRequestException;
import com.nhat.SharedBudgetManagement.exception.ConflictException;
import com.nhat.SharedBudgetManagement.exception.ForbiddenException;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import com.nhat.SharedBudgetManagement.repository.BudgetRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.EmailService;
import com.nhat.SharedBudgetManagement.service.impl.BudgetMemberServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BudgetMemberService")
class BudgetMemberServiceTest {

    @Mock
    private BudgetMemberRepository budgetMemberRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BudgetMemberServiceImpl budgetMemberService;

    private Budget testBudget;
    private User testUser;
    private BudgetMember testMember;

    @BeforeEach
    void setUp() {
        testBudget = Budget.builder().id(10L).name("Vacation Budget").build();
        testUser = User.builder().id(2L).email("member@example.com").fullName("Member").build();
        testMember = BudgetMember.builder()
                .id(100L)
                .budget(testBudget)
                .user(testUser)
                .role(BudgetRole.EDITOR)
                .status(MemberStatus.PENDING)
                .inviteToken("token-123")
                .build();
    }

    @Test
    @DisplayName("inviteMember - Success: should create invitation with PENDING status and token")
    void inviteMember_success() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(testUser));
        when(budgetMemberRepository.existsByUserIdAndBudgetId(2L, 10L)).thenReturn(false);
        when(budgetMemberRepository.save(any(BudgetMember.class))).thenAnswer(i -> i.getArgument(0));

        BudgetMember result = budgetMemberService.inviteMember(10L, "member@example.com", BudgetRole.EDITOR);

        assertNotNull(result);
        assertEquals(BudgetRole.EDITOR, result.getRole());
        assertEquals(MemberStatus.PENDING, result.getStatus());
        assertNotNull(result.getInviteToken());
        verify(budgetMemberRepository).save(any(BudgetMember.class));
        verify(emailService).sendBudgetInvitationEmail(eq("member@example.com"), eq("Vacation Budget"), any(), any(String.class));
    }

    @Test
    @DisplayName("inviteMember - Cannot invite as OWNER: should throw BadRequestException")
    void inviteMember_cannotInviteOwner() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(testUser));
        when(budgetMemberRepository.existsByUserIdAndBudgetId(2L, 10L)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> budgetMemberService.inviteMember(10L, "member@example.com", BudgetRole.OWNER));
    }

    @Test
    @DisplayName("inviteMember - Member Already Exists: should throw ConflictException")
    void inviteMember_alreadyExists() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(testUser));
        when(budgetMemberRepository.existsByUserIdAndBudgetId(2L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> budgetMemberService.inviteMember(10L, "member@example.com", BudgetRole.EDITOR));
    }

    @Test
    @DisplayName("acceptInvite - Success: should set status to ACCEPTED and clear inviteToken")
    void acceptInvite_success() {
        when(budgetMemberRepository.findByInviteToken("token-123")).thenReturn(Optional.of(testMember));
        when(budgetMemberRepository.save(any(BudgetMember.class))).thenReturn(testMember);

        BudgetMember result = budgetMemberService.acceptInvite("token-123");

        assertNotNull(result);
        assertEquals(MemberStatus.ACCEPTED, testMember.getStatus());
        assertNull(testMember.getInviteToken());
        assertNotNull(testMember.getJoinedAt());
        verify(budgetMemberRepository).save(testMember);
    }

    @Test
    @DisplayName("acceptInvite - Invalid Token: should throw BadRequestException")
    void acceptInvite_invalidToken() {
        when(budgetMemberRepository.findByInviteToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> budgetMemberService.acceptInvite("invalid-token"));
    }

    @Test
    @DisplayName("acceptInvite - Already Processed: should throw BadRequestException")
    void acceptInvite_alreadyProcessed() {
        testMember.setStatus(MemberStatus.ACCEPTED);
        when(budgetMemberRepository.findByInviteToken("token-123")).thenReturn(Optional.of(testMember));

        assertThrows(BadRequestException.class, () -> budgetMemberService.acceptInvite("token-123"));
    }

    @Test
    @DisplayName("declineInvite - Success: should set status to DECLINED")
    void declineInvite_success() {
        when(budgetMemberRepository.findByInviteToken("token-123")).thenReturn(Optional.of(testMember));
        when(budgetMemberRepository.save(any(BudgetMember.class))).thenReturn(testMember);

        BudgetMember result = budgetMemberService.declineInvite("token-123");

        assertNotNull(result);
        assertEquals(MemberStatus.DECLINED, testMember.getStatus());
        assertNull(testMember.getInviteToken());
    }

    @Test
    @DisplayName("changeMemberRole - Success: should update role of non-owner member")
    void changeMemberRole_success() {
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));
        when(budgetMemberRepository.save(any(BudgetMember.class))).thenReturn(testMember);

        BudgetMember result = budgetMemberService.changeMemberRole(10L, 2L, BudgetRole.VIEWER);

        assertNotNull(result);
        assertEquals(BudgetRole.VIEWER, testMember.getRole());
    }

    @Test
    @DisplayName("changeMemberRole - Cannot modify OWNER role: should throw ForbiddenException")
    void changeMemberRole_cannotModifyOwner() {
        testMember.setRole(BudgetRole.OWNER);
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        assertThrows(ForbiddenException.class,
                () -> budgetMemberService.changeMemberRole(10L, 2L, BudgetRole.EDITOR));
    }

    @Test
    @DisplayName("changeMemberRole - Cannot assign OWNER role: should throw BadRequestException")
    void changeMemberRole_cannotAssignOwner() {
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        assertThrows(BadRequestException.class,
                () -> budgetMemberService.changeMemberRole(10L, 2L, BudgetRole.OWNER));
    }

    @Test
    @DisplayName("removeMember - Success: should remove member from budget")
    void removeMember_success() {
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        budgetMemberService.removeMember(10L, 2L);

        verify(budgetMemberRepository).delete(testMember);
    }

    @Test
    @DisplayName("removeMember - Cannot remove OWNER: should throw ForbiddenException")
    void removeMember_cannotRemoveOwner() {
        testMember.setRole(BudgetRole.OWNER);
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        assertThrows(ForbiddenException.class, () -> budgetMemberService.removeMember(10L, 2L));
        verify(budgetMemberRepository, never()).delete(any(BudgetMember.class));
    }

    @Test
    @DisplayName("leaveBudget - Success: non-owner can leave budget")
    void leaveBudget_success() {
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        budgetMemberService.leaveBudget(10L, 2L);

        verify(budgetMemberRepository).delete(testMember);
    }

    @Test
    @DisplayName("leaveBudget - OWNER cannot leave: should throw ForbiddenException")
    void leaveBudget_ownerCannotLeave() {
        testMember.setRole(BudgetRole.OWNER);
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        assertThrows(ForbiddenException.class, () -> budgetMemberService.leaveBudget(10L, 2L));
        verify(budgetMemberRepository, never()).delete(any(BudgetMember.class));
    }

    @Test
    @DisplayName("getMemberRole - Success: should return member role")
    void getMemberRole_success() {
        when(budgetMemberRepository.findByUserIdAndBudgetId(2L, 10L)).thenReturn(Optional.of(testMember));

        BudgetRole role = budgetMemberService.getMemberRole(10L, 2L);

        assertEquals(BudgetRole.EDITOR, role);
    }
}
