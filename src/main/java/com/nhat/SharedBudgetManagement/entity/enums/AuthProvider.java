package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Phương thức xác thực của User.
 * <ul>
 *   <li>LOCAL  — đăng ký bằng email/password trực tiếp</li>
 *   <li>GOOGLE — đăng nhập qua OAuth2 Google</li>
 * </ul>
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
