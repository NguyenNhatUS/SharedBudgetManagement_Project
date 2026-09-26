package com.nhat.SharedBudgetManagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhat.SharedBudgetManagement.controller.v1.AuthController;
import com.nhat.SharedBudgetManagement.dto.request.ForgotPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.request.LoginRequest;
import com.nhat.SharedBudgetManagement.dto.request.RefreshTokenRequest;
import com.nhat.SharedBudgetManagement.dto.request.RegisterRequest;
import com.nhat.SharedBudgetManagement.dto.request.ResetPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.response.AuthResponse;
import com.nhat.SharedBudgetManagement.dto.response.UserResponse;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.GlobalExceptionHandler;
import com.nhat.SharedBudgetManagement.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuthController")
class AuthControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        private AuthResponse mockAuthResponse;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(authController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();

                objectMapper = new ObjectMapper();

                mockAuthResponse = AuthResponse.builder()
                                .accessToken("mock_access_token")
                                .refreshToken("mock_refresh_token")
                                .tokenType("Bearer")
                                .user(UserResponse.builder()
                                                .id(1L)
                                                .email("user@example.com")
                                                .fullName("Test User")
                                                .role(UserRole.ROLE_USER)
                                                .build())
                                .build();
        }

        @Test
        @DisplayName("POST /api/v1/auth/register should return 201 Created on valid input")
        void register_success() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .email("user@example.com")
                                .password("password123")
                                .fullName("Test User")
                                .build();

                when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value(201))
                                .andExpect(jsonPath("$.message").value("User registered successfully"))
                                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))
                                .andExpect(jsonPath("$.data.refreshToken").value("mock_refresh_token"))
                                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register should return 400 Bad Request when validation fails")
        void register_validationFails() throws Exception {
                RegisterRequest invalidRequest = RegisterRequest.builder()
                                .email("invalid-email-format")
                                .password("123") // too short (< 6)
                                .fullName("")
                                .build();

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.data.email").exists())
                                .andExpect(jsonPath("$.data.password").exists())
                                .andExpect(jsonPath("$.data.fullName").exists());
        }

        @Test
        @DisplayName("POST /api/v1/auth/login should return 200 OK on valid credentials")
        void login_success() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .email("user@example.com")
                                .password("password123")
                                .build();

                when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message").value("Login successful"))
                                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh should return 200 OK with new tokens")
        void refresh_success() throws Exception {
                RefreshTokenRequest request = RefreshTokenRequest.builder()
                                .refreshToken("mock_refresh_token")
                                .build();

                when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(mockAuthResponse);

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/logout should return 200 OK")
        void logout_success() throws Exception {
                RefreshTokenRequest request = RefreshTokenRequest.builder()
                                .refreshToken("mock_refresh_token")
                                .build();

                mockMvc.perform(post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/forgot-password should return 200 OK")
        void forgotPassword_success() throws Exception {
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .email("user@example.com")
                                .build();

                doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

                mockMvc.perform(post("/api/v1/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message").value("The password reset OTP has been sent to your email"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/forgot-password should return 400 Bad Request when email is invalid")
        void forgotPassword_invalidEmail() throws Exception {
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .email("not-an-email")
                                .build();

                mockMvc.perform(post("/api/v1/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("POST /api/v1/auth/reset-password should return 200 OK")
        void resetPassword_success() throws Exception {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("user@example.com")
                                .otp("123456")
                                .newPassword("newPassword123")
                                .build();

                doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

                mockMvc.perform(post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message").value("Password reset successfully. Please log in again."));
        }

        @Test
        @DisplayName("POST /api/v1/auth/reset-password should return 400 Bad Request when OTP is invalid length")
        void resetPassword_invalidOtpLength() throws Exception {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("user@example.com")
                                .otp("12")
                                .newPassword("newPassword123")
                                .build();

                mockMvc.perform(post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }
}
