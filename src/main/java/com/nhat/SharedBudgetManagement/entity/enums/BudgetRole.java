package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Vai trò của thành viên trong một Budget cụ thể (Resource-based Authorization).
 * <ul>
 *   <li>OWNER  — toàn quyền: quản lý thành viên, xoá Budget, CRUD giao dịch</li>
 *   <li>EDITOR — thêm/sửa/xoá giao dịch, không quản lý thành viên</li>
 *   <li>VIEWER — chỉ xem giao dịch và thống kê</li>
 * </ul>
 */
public enum BudgetRole {
    OWNER,
    EDITOR,
    VIEWER
}
