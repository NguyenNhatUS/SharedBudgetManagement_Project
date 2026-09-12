package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Vai trò toàn cục của User trong hệ thống (RBAC toàn cục).
 * Khác với {BudgetRole} — role này áp dụng cho toàn bộ hệ thống,
 * không phụ thuộc vào từng Budget cụ thể.
 *   ROLE_ADMIN — quản trị hệ thống
 *   ROLE_USER  — người dùng thông thường
 */
public enum UserRole {
    ROLE_ADMIN,
    ROLE_USER
}
