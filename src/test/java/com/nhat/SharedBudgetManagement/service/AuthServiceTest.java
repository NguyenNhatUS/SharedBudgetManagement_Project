package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.dto.request.ForgotPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.request.LoginRequest;
import com.nhat.SharedBudgetManagement.dto.request.RefreshTokenRequest;
import com.nhat.SharedBudgetManagement.dto.request.RegisterRequest;
import com.nhat.SharedBudgetManagement.dto.request.ResetPasswordRequest;
import com.nhat.SharedBudgetManagement.dto.response.AuthResponse;
import com.nhat.SharedBudgetManagement.dto.response.UserResponse;
import com.nhat.SharedBudgetManagement.entity.RefreshToken;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.BadRequestException;
import com.nhat.SharedBudgetManagement.exception.ConflictException;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.exception.UnauthorizedException;
import com.nhat.SharedBudgetManagement.mapper.UserMapper;
import com.nhat.SharedBudgetManagement.repository.RefreshTokenRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.security.JwtTokenProvider;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import com.nhat.SharedBudgetManagement.service.EmailService;
import com.nhat.SharedBudgetManagement.service.TokenBlacklistService;
import com.nhat.SharedBudgetManagement.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuthService & Token Rotation")
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private UserMapper userMapper;

        @Mock
        private TokenBlacklistService tokenBlacklistService;

        @Mock
        private StringRedisTemplate stringRedisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @Mock
        private EmailService emailService;

        @InjectMocks
        private AuthServiceImpl authService;

        private User testUser;
        private UserResponse testUserResponse;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .password("encoded_pass")
                                .fullName("Test User")
                                .role(UserRole.ROLE_USER)
                                .build();

                testUserResponse = UserResponse.builder()
                                .id(1L)
                                .email("test@example.com")
                                .fullName("Test User")
                                .role(UserRole.ROLE_USER)
                                .build();
        }

        @Test
        @DisplayName("Should successfully register a new user and return AuthResponse")
        void register_success() {
                RegisterRequest request = RegisterRequest.builder()
                                .email("test@example.com")
                                .password("raw_password")
                                .fullName("Test User")
                                .build();

                when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
                when(passwordEncoder.encode("raw_password")).thenReturn("encoded_pass");
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock_access_token");
                when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");
                when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
                when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

                AuthResponse response = authService.register(request);

                assertNotNull(response);
                assertEquals("mock_access_token", response.getAccessToken());
                assertEquals("mock_refresh_token", response.getRefreshToken());
                assertEquals("Bearer", response.getTokenType());
                assertEquals("test@example.com", response.getUser().getEmail());

                verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when registering with duplicate email")
        void register_duplicateEmail() {
                RegisterRequest request = RegisterRequest.builder()
                                .email("test@example.com")
                                .password("raw_password")
                                .fullName("Test User")
                                .build();

                when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

                assertThrows(ConflictException.class, () -> authService.register(request));
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should successfully login and return AuthResponse")
        void login_success() {
                LoginRequest request = LoginRequest.builder()
                                .email("test@example.com")
                                .password("raw_password")
                                .build();

                UserPrincipal principal = UserPrincipal.create(testUser);
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                principal.getAuthorities());

                when(authenticationManager.authenticate(any())).thenReturn(auth);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock_access_token");
                when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");
                when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
                when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

                AuthResponse response = authService.login(request);

                assertNotNull(response);
                assertEquals("mock_access_token", response.getAccessToken());
                assertEquals("mock_refresh_token", response.getRefreshToken());
                verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when login authentication fails")
        void login_invalidCredentials() {
                LoginRequest request = LoginRequest.builder()
                                .email("test@example.com")
                                .password("wrong_password")
                                .build();

                when(authenticationManager.authenticate(any()))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                assertThrows(BadCredentialsException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("Should rotate tokens when refresh token is valid")
        void refreshToken_success() {
                RefreshToken storedToken = RefreshToken.builder()
                                .id(10L)
                                .token("valid_refresh_token")
                                .user(testUser)
                                .expiryDate(LocalDateTime.now().plusDays(5))
                                .revoked(false)
                                .build();

                RefreshTokenRequest request = RefreshTokenRequest.builder()
                                .refreshToken("valid_refresh_token")
                                .build();

                when(refreshTokenRepository.findByToken("valid_refresh_token")).thenReturn(Optional.of(storedToken));
                when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access_token");
                when(jwtTokenProvider.generateRefreshToken()).thenReturn("new_refresh_token");
                when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
                when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

                AuthResponse response = authService.refreshToken(request);

                assertNotNull(response);
                assertEquals("new_access_token", response.getAccessToken());
                assertEquals("new_refresh_token", response.getRefreshToken());
                assertTrue(storedToken.getRevoked(), "Old refresh token should be revoked");
                verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should revoke all user tokens defensively when a revoked token is reused")
        void refreshToken_revokedToken_defensiveRevoke() {
                RefreshToken revokedToken = RefreshToken.builder()
                                .id(10L)
                                .token("stolen_revoked_token")
                                .user(testUser)
                                .expiryDate(LocalDateTime.now().plusDays(5))
                                .revoked(true)
                                .build();

                RefreshTokenRequest request = RefreshTokenRequest.builder()
                                .refreshToken("stolen_revoked_token")
                                .build();

                when(refreshTokenRepository.findByToken("stolen_revoked_token")).thenReturn(Optional.of(revokedToken));

                assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
                verify(refreshTokenRepository).revokeAllByUserId(1L);
        }

        @Test
        @DisplayName("Should successfully revoke token on logout")
        void logout_withRefreshToken() {
                RefreshToken storedToken = RefreshToken.builder()
                                .id(10L)
                                .token("active_token")
                                .user(testUser)
                                .revoked(false)
                                .build();

                when(refreshTokenRepository.findByToken("active_token")).thenReturn(Optional.of(storedToken));

                authService.logout("active_token", null);

                assertTrue(storedToken.getRevoked());
                verify(refreshTokenRepository).save(storedToken);
        }

        @Test
        @DisplayName("Should successfully revoke refresh token and blacklist access token in Redis")
        void logout_withRefreshTokenAndAccessToken() {
                RefreshToken storedToken = RefreshToken.builder()
                                .id(10L)
                                .token("active_token")
                                .user(testUser)
                                .revoked(false)
                                .build();

                when(refreshTokenRepository.findByToken("active_token")).thenReturn(Optional.of(storedToken));
                when(jwtTokenProvider.getRemainingExpirationMs("jwt_access_token")).thenReturn(600000L);

                authService.logout("active_token", 1L, "jwt_access_token");

                assertTrue(storedToken.getRevoked());
                verify(refreshTokenRepository).save(storedToken);
                verify(tokenBlacklistService).blacklistToken("jwt_access_token", 600000L);
        }

        @Test
        @DisplayName("forgotPassword - Success: should generate OTP, save to Redis and send email")
        void forgotPassword_success() {
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .email("test@example.com")
                                .build();

                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

                authService.forgotPassword(request);

                verify(valueOperations).set(eq("sbm:otp:reset:test@example.com"), any(String.class), eq(Duration.ofMinutes(5)));
                verify(emailService).sendPasswordResetOtpEmail(eq("test@example.com"), any(String.class));
        }

        @Test
        @DisplayName("forgotPassword - User not found: should throw ResourceNotFoundException")
        void forgotPassword_userNotFound() {
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .email("notfound@example.com")
                                .build();

                when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
                verify(emailService, never()).sendPasswordResetOtpEmail(any(), any());
        }

        @Test
        @DisplayName("resetPassword - Success: should update password, delete OTP and revoke tokens")
        void resetPassword_success() {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("test@example.com")
                                .otp("123456")
                                .newPassword("newSecretPassword123")
                                .build();

                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("sbm:otp:reset:test@example.com")).thenReturn("123456");
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                when(passwordEncoder.encode("newSecretPassword123")).thenReturn("new_encoded_password");

                authService.resetPassword(request);

                assertEquals("new_encoded_password", testUser.getPassword());
                verify(userRepository).save(testUser);
                verify(stringRedisTemplate).delete("sbm:otp:reset:test@example.com");
                verify(refreshTokenRepository).revokeAllByUserId(1L);
        }

        @Test
        @DisplayName("resetPassword - Invalid OTP: should throw BadRequestException")
        void resetPassword_invalidOtp() {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("test@example.com")
                                .otp("999999")
                                .newPassword("newSecretPassword123")
                                .build();

                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("sbm:otp:reset:test@example.com")).thenReturn("123456");

                assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("resetPassword - Expired OTP (null): should throw BadRequestException")
        void resetPassword_expiredOtp() {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("test@example.com")
                                .otp("123456")
                                .newPassword("newSecretPassword123")
                                .build();

                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("sbm:otp:reset:test@example.com")).thenReturn(null);

                assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("resetPassword - User not found: should throw ResourceNotFoundException")
        void resetPassword_userNotFound() {
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .email("test@example.com")
                                .otp("123456")
                                .newPassword("newSecretPassword123")
                                .build();

                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("sbm:otp:reset:test@example.com")).thenReturn("123456");
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> authService.resetPassword(request));
                verify(userRepository, never()).save(any());
        }
}