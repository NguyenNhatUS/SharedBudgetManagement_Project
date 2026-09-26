package com.nhat.SharedBudgetManagement.security;

import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BudgetSecurity Resource-Based Authorization")
class BudgetSecurityTest {

        @Mock
        private BudgetMemberRepository budgetMemberRepository;

        @InjectMocks
        private BudgetSecurity budgetSecurity;

        private User testUser;
        private User adminUser;
        private Budget testBudget;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .email("user@example.com")
                                .fullName("Regular User")
                                .role(UserRole.ROLE_USER)
                                .build();

                adminUser = User.builder()
                                .id(99L)
                                .email("admin@example.com")
                                .fullName("Admin User")
                                .role(UserRole.ROLE_ADMIN)
                                .build();

                testBudget = Budget.builder()
                                .id(10L)
                                .name("Family Budget")
                                .currency("VND")
                                .build();
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private void authenticateUser(User user) {
                UserPrincipal principal = UserPrincipal.create(user);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
                                principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        @DisplayName("Should return true for isOwner when user is OWNER with status ACCEPTED")
        void isOwner_trueWhenOwnerAndAccepted() {
                authenticateUser(testUser);

                BudgetMember member = BudgetMember.builder()
                                .id(100L)
                                .user(testUser)
                                .budget(testBudget)
                                .role(BudgetRole.OWNER)
                                .status(MemberStatus.ACCEPTED)
                                .build();

                when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L))
                                .thenReturn(Optional.of(member));

                assertTrue(budgetSecurity.isOwner(10L));
                assertTrue(budgetSecurity.canEdit(10L));
                assertTrue(budgetSecurity.canView(10L));
        }

        @Test
        @DisplayName("Should return false for isOwner when user is EDITOR, but true for canEdit and canView")
        void isOwner_falseWhenEditor() {
                authenticateUser(testUser);

                BudgetMember member = BudgetMember.builder()
                                .id(101L)
                                .user(testUser)
                                .budget(testBudget)
                                .role(BudgetRole.EDITOR)
                                .status(MemberStatus.ACCEPTED)
                                .build();

                when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L))
                                .thenReturn(Optional.of(member));

                assertFalse(budgetSecurity.isOwner(10L));
                assertTrue(budgetSecurity.canEdit(10L));
                assertTrue(budgetSecurity.canView(10L));
        }

        @Test
        @DisplayName("Should return true for canView but false for canEdit when user is VIEWER")
        void canView_trueWhenViewer() {
                authenticateUser(testUser);

                BudgetMember member = BudgetMember.builder()
                                .id(102L)
                                .user(testUser)
                                .budget(testBudget)
                                .role(BudgetRole.VIEWER)
                                .status(MemberStatus.ACCEPTED)
                                .build();

                when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L))
                                .thenReturn(Optional.of(member));

                assertFalse(budgetSecurity.isOwner(10L));
                assertFalse(budgetSecurity.canEdit(10L));
                assertTrue(budgetSecurity.canView(10L));
        }

        @Test
        @DisplayName("Should return false when membership status is PENDING")
        void statusPending_returnsFalse() {
                authenticateUser(testUser);

                BudgetMember member = BudgetMember.builder()
                                .id(103L)
                                .user(testUser)
                                .budget(testBudget)
                                .role(BudgetRole.OWNER)
                                .status(MemberStatus.PENDING)
                                .build();

                when(budgetMemberRepository.findByUserIdAndBudgetId(1L, 10L))
                                .thenReturn(Optional.of(member));

                assertFalse(budgetSecurity.isOwner(10L));
                assertFalse(budgetSecurity.canEdit(10L));
                assertFalse(budgetSecurity.canView(10L));
        }

        @Test
        @DisplayName("Should return true for all checks when user has ROLE_ADMIN")
        void admin_bypassAllChecks() {
                authenticateUser(adminUser);

                assertTrue(budgetSecurity.isOwner(10L));
                assertTrue(budgetSecurity.canEdit(10L));
                assertTrue(budgetSecurity.canView(10L));
                assertTrue(budgetSecurity.hasRole(10L, "OWNER"));
        }

        @Test
        @DisplayName("Should return false when user is not authenticated")
        void unauthenticated_returnsFalse() {
                assertFalse(budgetSecurity.isOwner(10L));
                assertFalse(budgetSecurity.canEdit(10L));
                assertFalse(budgetSecurity.canView(10L));
        }
}
