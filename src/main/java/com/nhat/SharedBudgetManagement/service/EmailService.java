package com.nhat.SharedBudgetManagement.service;

/**
 * Service giao tiếp gửi Email cho hệ thống SharedBudgetManagement (Giai đoạn 8).
 * Toàn bộ tác vụ gửi email chạy bất đồng bộ (@Async) trên background thread pool.
 */
public interface EmailService {

    /**
     * Gửi email HTML tổng quát.
     * @param to Địa chỉ email người nhận
     * @param subject Tiêu đề thư
     * @param htmlContent Nội dung HTML
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * Gửi email mời tham gia ngân sách kèm liên kết xác thực inviteToken.
     * @param toEmail Email thành viên được mời
     * @param budgetName Tên ngân sách
     * @param inviterName Tên người mời (Owner)
     * @param inviteToken Token xác thực lời mời
     */
    void sendBudgetInvitationEmail(String toEmail, String budgetName, String inviterName, String inviteToken);

    /**
     * Gửi email chứa mã OTP đặt lại mật khẩu (thời hạn 5 phút).
     * @param toEmail Email người dùng yêu cầu khôi phục mật khẩu
     * @param otp Mã OTP 6 chữ số
     */
    void sendPasswordResetOtpEmail(String toEmail, String otp);
}