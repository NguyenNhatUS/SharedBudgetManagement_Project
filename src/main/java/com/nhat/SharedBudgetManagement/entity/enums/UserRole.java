package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Vai trò toàn cục của User trong hệ thống (RBAC toàn cục).
 * Khác với {@link BudgetRole} — role này áp dụng cho toàn bộ hệ thống,
 * không phụ thuộc vào từng Budget cụ thể.
 * <ul>
 *   <li>ROLE_ADMIN — quản trị hệ thống</li>
 *   <li>ROLE_USER  — người dùng thông thường</li>
 * </ul>
 */
public enum UserRole {
    ROLE_ADMIN,
    ROLE_USER
}
