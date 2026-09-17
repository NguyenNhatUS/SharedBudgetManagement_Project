package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.dto.request.ForgotPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.request.LoginRequest;
import com.nhat.SharedBudgetManagement.dto.request.RefreshTokenRequest;
import com.nhat.SharedBudgetManagement.dto.request.RegisterRequest;
import com.nhat.SharedBudgetManagement.dto.request.ResetPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.response.AuthResponse;
import com.nhat.SharedBudgetManagement.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken, Long currentUserId);

    void logout(String refreshToken, Long currentUserId, String accessToken);

    String createRefreshTokenForUser(User user);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
