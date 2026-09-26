package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@sharedbudget.com");
        ReflectionTestUtils.setField(emailService, "invitationBaseUrl", "http://localhost:3000/invitations");

        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
        lenient().when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendHtmlEmail - Success: should create and send MIME message")
    void sendHtmlEmail_success() {
        emailService.sendHtmlEmail("user@example.com", "Test Subject", "<p>Hello World</p>");

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendHtmlEmail - Exception caught: should not bubble exception")
    void sendHtmlEmail_handlesExceptionGracefully() {
        doThrow(new MailSendException("SMTP error")).when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> 
            emailService.sendHtmlEmail("user@example.com", "Error Subject", "<p>Fails</p>")
        );

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendBudgetInvitationEmail - Success: should compose and send HTML invitation")
    void sendBudgetInvitationEmail_success() {
        emailService.sendBudgetInvitationEmail(
                "invitee@example.com",
                "Family Budget",
                "Alice",
                "token-abc-123"
        );

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetOtpEmail - Success: should compose and send OTP email")
    void sendPasswordResetOtpEmail_success() {
        emailService.sendPasswordResetOtpEmail(
                "user@example.com",
                "654321"
        );

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
