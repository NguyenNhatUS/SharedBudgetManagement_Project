package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.dto.request.LoginRequest;
import com.nhat.SharedBudgetManagement.dto.request.RefreshTokenRequest;
import com.nhat.SharedBudgetManagement.dto.request.RegisterRequest;
import com.nhat.SharedBudgetManagement.dto.response.AuthResponse;
import com.nhat.SharedBudgetManagement.dto.response.UserResponse;
import com.nhat.SharedBudgetManagement.entity.RefreshToken;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.ConflictException;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.exception.UnauthorizedException;
import com.nhat.SharedBudgetManagement.mapper.UserMapper;
import com.nhat.SharedBudgetManagement.repository.RefreshTokenRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.security.JwtTokenProvider;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import com.nhat.SharedBudgetManagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered successfully with email: {}", savedUser.getEmail());

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createAndSaveRefreshToken(savedUser);

        UserResponse userResponse = userMapper.toResponse(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createAndSaveRefreshToken(user);

        UserResponse userResponse = userMapper.toResponse(user);
        log.info("User {} logged in successfully", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));

        if (!storedToken.isValid()) {
            // Nếu phát hiện token đã bị thu hồi hoặc hết hạn, có thể là hành vi tấn công lặp lại
            if (Boolean.TRUE.equals(storedToken.getRevoked())) {
                log.warn("Attempted reuse of revoked refresh token for user: {}", storedToken.getUser().getId());
                // Thu hồi tất cả refresh token của user để phòng thủ
                refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            }
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();

        // Áp dụng cơ chế Token Rotation: Thu hồi token cũ và tạo token mới
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = createAndSaveRefreshToken(user);

        UserResponse userResponse = userMapper.toResponse(user);
        log.info("Refreshed tokens successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken, Long currentUserId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Revoked specific refresh token for user: {}", token.getUser().getId());
            });
        } else if (currentUserId != null) {
            refreshTokenRepository.revokeAllByUserId(currentUserId);
            log.info("Revoked all refresh tokens for user: {}", currentUserId);
        }
    }

    @Override
    @Transactional
    public String createRefreshTokenForUser(User user) {
        return createAndSaveRefreshToken(user);
    }

    private String createAndSaveRefreshToken(User user) {
        String tokenStr = jwtTokenProvider.generateRefreshToken();
        long expirationSeconds = jwtTokenProvider.getRefreshTokenExpirationMs() / 1000;

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(expirationSeconds))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }
}
