package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Triển khai dịch vụ gửi Email bất đồng bộ chuẩn Spring Mail (Giai đoạn 8).
 * Tự động đóng gói giao diện HTML chuyên nghiệp, responsive.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:noreply@sharedbudget.com}")
    private String fromEmail;

    @Value("${app.mail.invitation-url:http://localhost:3000/invitations}")
    private String invitationBaseUrl;

    @Async("mailTaskExecutor")
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Email successfully sent to: {} with subject: {}", to, subject);
        } catch (MessagingException ex) {
            log.error("Failed to compose MIME email for {}: {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    @Override
    public void sendBudgetInvitationEmail(String toEmail, String budgetName, String inviterName, String inviteToken) {
        String subject = "\uD83D\uDCE8 Lời mời tham gia ngân sách: " + budgetName;
        String acceptUrl = invitationBaseUrl + "/accept?token=" + inviteToken;
        String declineUrl = invitationBaseUrl + "/decline?token=" + inviteToken;

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                    .card { max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); padding: 30px 20px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                    .content { padding: 30px 25px; line-height: 1.6; }
                    .budget-badge { background: #e0e7ff; color: #4338ca; padding: 6px 14px; border-radius: 20px; font-weight: 600; display: inline-block; margin: 10px 0; }
                    .btn-group { margin: 30px 0 20px; text-align: center; }
                    .btn-accept { background-color: #4f46e5; color: #ffffff !important; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; display: inline-block; margin-right: 12px; }
                    .btn-decline { background-color: #f3f4f6; color: #4b5563 !important; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: 600; display: inline-block; }
                    .token-info { background: #f8fafc; border: 1px dashed #cbd5e1; padding: 12px; border-radius: 6px; font-family: monospace; font-size: 13px; color: #64748b; word-break: break-all; margin-top: 20px; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1>SharedBudget Management</h1>
                    </div>
                    <div class="content">
                        <p>Xin chào,</p>
                        <p><strong>%s</strong> vừa gửi lời mời bạn tham gia quản lý chung ngân sách:</p>
                        <div style="text-align: center;">
                            <div class="budget-badge">\uD83D\uDCB0 %s</div>
                        </div>
                        <p>Sau khi chấp nhận, bạn có thể xem các giao dịch, thêm chi tiêu và theo dõi tiến độ tài chính cùng nhóm.</p>
                        <div class="btn-group">
                            <a href="%s" class="btn-accept">Chấp nhận tham gia</a>
                            <a href="%s" class="btn-decline">Từ chối</a>
                        </div>
                        <div class="token-info">
                            Mã lời mời dự phòng: %s
                        </div>
                    </div>
                    <div class="footer">
                        Email này được gửi tự động từ hệ thống SharedBudgetManagement. Vui lòng không trả lời.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(inviterName, budgetName, acceptUrl, declineUrl, inviteToken);

        sendHtmlEmail(toEmail, subject, html);
    }

    @Async("mailTaskExecutor")
    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        String subject = "\uD83D\uDD11 Mã OTP Đặt lại mật khẩu - SharedBudgetManagement";

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                    .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, #059669 0%%, #10b981 100%%); padding: 25px 20px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                    .content { padding: 30px 25px; line-height: 1.6; text-align: center; }
                    .otp-box { background: #f0fdf4; border: 2px dashed #059669; border-radius: 10px; padding: 18px 24px; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #047857; margin: 25px auto; display: inline-block; }
                    .warning { background: #fef2f2; border-left: 4px solid #ef4444; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #991b1b; text-align: left; margin-top: 20px; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1>Yêu cầu Đặt lại Mật khẩu</h1>
                    </div>
                    <div class="content">
                        <p style="text-align: left;">Xin chào,</p>
                        <p style="text-align: left;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>%s</strong>. Vui lòng sử dụng mã OTP bên dưới để xác nhận:</p>
                        <div>
                            <div class="otp-box">%s</div>
                        </div>
                        <div class="warning">
                            ⏰ <strong>Lưu ý bảo mật:</strong> Mã OTP có hiệu lực trong <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai, kể cả nhân viên hỗ trợ.
                        </div>
                    </div>
                    <div class="footer">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này hoặc liên hệ hỗ trợ ngay lập tức.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(toEmail, otp);

        sendHtmlEmail(toEmail, subject, html);
    }
}
