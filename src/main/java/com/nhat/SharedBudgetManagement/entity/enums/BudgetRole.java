package com.nhat.SharedBudgetManagement.entity.enums;

/**
Vai trò của thành viên trong một Budget cụ thể (Resource-based Authorization).
    OWNER  — toàn quyền: quản lý thành viên, xoá Budget, CRUD giao dịch
    EDITOR — thêm/sửa/xoá giao dịch, không quản lý thành viên
    VIEWER — chỉ xem giao dịch và thống kê
 */
public enum BudgetRole {
    OWNER,
    EDITOR,
    VIEWER
}
