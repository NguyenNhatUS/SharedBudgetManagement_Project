package com.nhat.SharedBudgetManagement.entity.enums;

/**
 * Trạng thái lời mời tham gia Budget.
 * <ul>
 *   <li>PENDING  — đã gửi lời mời, chưa phản hồi</li>
 *   <li>ACCEPTED — đã chấp nhận lời mời</li>
 *   <li>DECLINED — đã từ chối lời mời</li>
 * </ul>
 */
public enum MemberStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
