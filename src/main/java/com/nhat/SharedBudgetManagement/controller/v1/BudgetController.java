package com.nhat.SharedBudgetManagement.controller.v1;

import com.nhat.SharedBudgetManagement.common.ApiResponse;
import com.nhat.SharedBudgetManagement.common.PageResponse;
import com.nhat.SharedBudgetManagement.dto.request.*;
import com.nhat.SharedBudgetManagement.dto.response.BudgetMemberResponse;
import com.nhat.SharedBudgetManagement.dto.response.BudgetResponse;
import com.nhat.SharedBudgetManagement.entity.Budget;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import com.nhat.SharedBudgetManagement.mapper.BudgetMapper;
import com.nhat.SharedBudgetManagement.mapper.BudgetMemberMapper;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import com.nhat.SharedBudgetManagement.service.BudgetMemberService;
import com.nhat.SharedBudgetManagement.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetMemberService budgetMemberService;
    private final BudgetMapper budgetMapper;
    private final BudgetMemberMapper budgetMemberMapper;


    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateBudgetRequest request) {

        Budget budget = budgetService.createBudget(
                currentUser.getId(),
                request.getName(),
                request.getDescription(),
                request.getCurrency());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Budget created successfully", budgetMapper.toResponse(budget)));
    }

    @GetMapping("/{budgetId}")
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudget(@PathVariable Long budgetId) {
        Budget budget = budgetService.getBudgetWithMembers(budgetId);
        return ResponseEntity.ok(ApiResponse.success(budgetMapper.toResponse(budget)));
    }


    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BudgetResponse>>> getBudgetsByUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Budget> budgetPage = budgetService.getAllBudgetsByUserId(currentUser.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(budgetPage, budgetMapper::toResponse)));
    }

    @PutMapping("/{budgetId}")
    @PreAuthorize("@budgetSecurity.isOwner(#budgetId)")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) {
        Budget budget = budgetService.updateBudget(
                budgetId, request.getName(), request.getDescription(),
                request.getCurrency());
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", budgetMapper.toResponse(budget)));
    }

    @DeleteMapping("/{budgetId}")
    @PreAuthorize("@budgetSecurity.isOwner(#budgetId)")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.noContent("Budget deleted successfully"));
    }

    // ====================== Member Management ======================

    @PostMapping("/{budgetId}/members/invite")
    @PreAuthorize("@budgetSecurity.isOwner(#budgetId)")
    public ResponseEntity<ApiResponse<BudgetMemberResponse>> inviteMember(
            @PathVariable Long budgetId,
            @Valid @RequestBody InviteMemberRequest request) {
        BudgetMember member = budgetMemberService.inviteMember(
                budgetId, request.getEmail(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Invitation sent", budgetMemberMapper.toResponse(member)));
    }

    @PostMapping("/members/accept")
    public ResponseEntity<ApiResponse<BudgetMemberResponse>> acceptInvite(
            @RequestParam String token) {
        BudgetMember member = budgetMemberService.acceptInvite(token);
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted", budgetMemberMapper.toResponse(member)));
    }

    @PostMapping("/members/decline")
    public ResponseEntity<ApiResponse<BudgetMemberResponse>> declineInvite(
            @RequestParam String token) {
        BudgetMember member = budgetMemberService.declineInvite(token);
        return ResponseEntity.ok(ApiResponse.success("Invitation declined", budgetMemberMapper.toResponse(member)));
    }

    /**
     * Danh sách thành viên trong ngân sách có phân trang & sắp xếp.
     * Ví dụ: GET /api/v1/budgets/1/members?page=0&size=20&sort=joinedAt,asc
     */
    @GetMapping("/{budgetId}/members")
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<PageResponse<BudgetMemberResponse>>> getMembers(
            @PathVariable Long budgetId,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BudgetMember> memberPage = budgetMemberService.getMembersByBudgetId(budgetId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(memberPage, budgetMemberMapper::toResponse)));
    }

    @PatchMapping("/{budgetId}/members/{userId}/role")
    @PreAuthorize("@budgetSecurity.isOwner(#budgetId)")
    public ResponseEntity<ApiResponse<BudgetMemberResponse>> changeMemberRole(
            @PathVariable Long budgetId,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeMemberRoleRequest request) {
        BudgetMember member = budgetMemberService.changeMemberRole(budgetId, userId, request.getRole());
        return ResponseEntity
                .ok(ApiResponse.success("Role changed successfully", budgetMemberMapper.toResponse(member)));
    }

    @DeleteMapping("/{budgetId}/members/{userId}")
    @PreAuthorize("@budgetSecurity.isOwner(#budgetId)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long budgetId,
            @PathVariable Long userId) {
        budgetMemberService.removeMember(budgetId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("Member removed successfully"));
    }

    @PostMapping("/{budgetId}/members/leave")
    public ResponseEntity<ApiResponse<Void>> leaveBudget(
            @PathVariable Long budgetId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        budgetMemberService.leaveBudget(budgetId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.noContent("Left budget successfully"));
    }
}