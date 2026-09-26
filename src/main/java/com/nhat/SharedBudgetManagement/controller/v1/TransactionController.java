package com.nhat.SharedBudgetManagement.controller.v1;

import com.nhat.SharedBudgetManagement.common.ApiResponse;
import com.nhat.SharedBudgetManagement.common.PageResponse;
import com.nhat.SharedBudgetManagement.dto.request.CreateTransactionRequest;
import com.nhat.SharedBudgetManagement.dto.request.UpdateTransactionRequest;
import com.nhat.SharedBudgetManagement.dto.response.TransactionResponse;
import com.nhat.SharedBudgetManagement.entity.Transaction;
import com.nhat.SharedBudgetManagement.entity.enums.TransactionType;
import com.nhat.SharedBudgetManagement.mapper.TransactionMapper;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import com.nhat.SharedBudgetManagement.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/api/v1/budgets/{budgetId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    @PreAuthorize("@budgetSecurity.canEdit(#budgetId)")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @PathVariable Long budgetId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateTransactionRequest request) {

        Transaction transaction = transactionService.createTransaction(
                budgetId,
                currentUser.getId(),
                request.getType(),
                request.getAmount(),
                request.getDescription(),
                request.getNote(),
                request.getTransactionDate(),
                request.getTagIds());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transaction created successfully", transactionMapper.toResponse(transaction)));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("@budgetSecurity.canEdit(#budgetId)")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long budgetId,
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {

        Transaction transaction = transactionService.updateTransaction(
                transactionId,
                request.getType(),
                request.getAmount(),
                request.getDescription(),
                request.getNote(),
                request.getTransactionDate(),
                request.getTagIds());

        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", transactionMapper.toResponse(transaction)));
    }

    @DeleteMapping("/{transactionId}")
    @PreAuthorize("@budgetSecurity.canEdit(#budgetId)")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long budgetId,
            @PathVariable Long transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.noContent("Transaction deleted successfully"));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable Long budgetId,
            @PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transactionMapper.toResponse(transaction)));
    }

    /**
     * Danh sách giao dịch với phân trang & sắp xếp.
     * Ví dụ: GET /api/v1/budgets/1/transactions?page=0&size=20&sort=transactionDate,desc
     * Lọc tuỳ chọn: ?type=EXPENSE hoặc ?startDate=2026-01-01&endDate=2026-12-31
     */
    @GetMapping
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @PathVariable Long budgetId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Transaction> page;

        if (type != null) {
            page = transactionService.getTransactionsByBudgetAndType(budgetId, type, pageable);
        } else if (startDate != null && endDate != null) {
            page = transactionService.getTransactionsByDateRange(budgetId, startDate, endDate, pageable);
        } else {
            page = transactionService.getTransactionsByBudget(budgetId, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page, transactionMapper::toResponse)));
    }

    // ====================== Thống kê ======================

    /**
     * Tổng thu/chi theo Budget.
     * GET /api/v1/budgets/1/transactions/summary
     */
    @GetMapping("/summary")
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<Map<TransactionType, BigDecimal>>> getSummary(
            @PathVariable Long budgetId) {
        Map<TransactionType, BigDecimal> summary = transactionService.getSummaryByBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Chi tiêu theo thành viên.
     * GET /api/v1/budgets/1/transactions/expense-by-member
     */
    @GetMapping("/expense-by-member")
    @PreAuthorize("@budgetSecurity.canView(#budgetId)")
    public ResponseEntity<ApiResponse<List<Object[]>>> getExpenseByMember(
            @PathVariable Long budgetId) {
        List<Object[]> data = transactionService.getExpenseByMember(budgetId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}