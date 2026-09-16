package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Nguồn xác thực của tài khoản User.
 *   LOCAL  — đăng ký bằng Email/Password truyền thống
 *   GOOGLE — đăng nhập qua Google OAuth2
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
