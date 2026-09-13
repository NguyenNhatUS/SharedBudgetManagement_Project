package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.BudgetRole;
import com.nhat.SharedBudgetManagement.entity.enums.MemberStatus;
import com.nhat.SharedBudgetManagement.repository.BudgetMemberRepository;
import com.nhat.SharedBudgetManagement.repository.BudgetRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.BudgetMemberService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetMemberServiceImpl implements BudgetMemberService {

    private final BudgetMemberRepository budgetMemberRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional
    public BudgetMember inviteMember(Long budgetId, String email, BudgetRole role) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));


        if (budgetMemberRepository.existsByUserIdAndBudgetId(user.getId(), budgetId)) {
            throw new IllegalStateException("User is already a member of this budget");
        }

        if (role == BudgetRole.OWNER) {
            throw new IllegalArgumentException("Cannot invite a member with OWNER role");
        }

        BudgetMember member = BudgetMember.builder()
                .user(user)
                .budget(budget)
                .role(role)
                .status(MemberStatus.PENDING)
                .inviteToken(UUID.randomUUID().toString())
                .build();

        return budgetMemberRepository.save(member);
    }

    @Override
    @Transactional
    public BudgetMember acceptInvite(String inviteToken) {
        BudgetMember member = budgetMemberRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite token"));

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalStateException("Invitation has already been processed");
        }

        member.setStatus(MemberStatus.ACCEPTED);
        member.setJoinedAt(LocalDateTime.now());
        member.setInviteToken(null);
        return budgetMemberRepository.save(member);
    }

    @Override
    @Transactional
    public BudgetMember declineInvite(String inviteToken) {
        BudgetMember member = budgetMemberRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invite token"));

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalStateException("Invitation has already been processed");
        }

        member.setStatus(MemberStatus.DECLINED);
        member.setInviteToken(null);
        return budgetMemberRepository.save(member);
    }

    @Override
    @Transactional
    public BudgetMember changeMemberRole(Long budgetId, Long userId, BudgetRole newRole) {
        BudgetMember member = budgetMemberRepository.findByUserIdAndBudgetId(userId, budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found in this budget"));


        if (member.getRole() == BudgetRole.OWNER) {
            throw new IllegalStateException("Cannot change the role of the OWNER");
        }


        if (newRole == BudgetRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role to a member");
        }

        member.setRole(newRole);
        return budgetMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Long budgetId, Long userId) {
        BudgetMember member = budgetMemberRepository.findByUserIdAndBudgetId(userId, budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found in this budget"));


        if (member.getRole() == BudgetRole.OWNER) {
            throw new IllegalStateException("Cannot remove the OWNER from the budget");
        }

        budgetMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void leaveBudget(Long budgetId, Long userId) {
        BudgetMember member = budgetMemberRepository.findByUserIdAndBudgetId(userId, budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found in this budget"));


        if (member.getRole() == BudgetRole.OWNER) {
            throw new IllegalStateException("OWNER cannot leave the budget. Transfer ownership or delete the budget instead.");
        }

        budgetMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetMember> getMembersByBudgetId(Long budgetId) {
        return budgetMemberRepository.findAllByBudgetId(budgetId);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetRole getMemberRole(Long budgetId, Long userId) {
        BudgetMember member = budgetMemberRepository.findByUserIdAndBudgetId(userId, budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found in this budget"));
        return member.getRole();
    }
}
