package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.dto.request.LoginRequest;
import com.nhat.SharedBudgetManagement.dto.request.RefreshTokenRequest;
import com.nhat.SharedBudgetManagement.dto.request.RegisterRequest;
import com.nhat.SharedBudgetManagement.dto.response.AuthResponse;
import com.nhat.SharedBudgetManagement.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken, Long currentUserId);

    String createRefreshTokenForUser(User user);
}
