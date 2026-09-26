package com.nhat.SharedBudgetManagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UserPrincipal testPrincipal;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 900000; // 15 mins
    private static final long REFRESH_EXPIRATION = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationMs", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationMs", REFRESH_EXPIRATION);

        testPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .fullName("Test User")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should successfully generate a valid JWT access token")
    void generateAccessToken_success() {
        String token = tokenProvider.generateAccessToken(testPrincipal);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should extract correct email subject from token")
    void extractEmail_success() {
        String token = tokenProvider.generateAccessToken(testPrincipal);
        String email = tokenProvider.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("Should extract correct userId from token claims")
    void extractUserId_success() {
        String token = tokenProvider.generateAccessToken(testPrincipal);
        Long userId = tokenProvider.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("Should return false when validating a malformed token")
    void validateToken_malformed() {
        assertFalse(tokenProvider.validateToken("invalid.token.structure"));
    }

    @Test
    @DisplayName("Should return false when validating an empty token")
    void validateToken_empty() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("Should return false when validating a token signed with a different key")
    void validateToken_wrongSignature() {
        JwtTokenProvider anotherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(anotherProvider, "jwtSecret", "different_secret_key_that_is_long_enough_for_hmac_sha256_12345678");
        ReflectionTestUtils.setField(anotherProvider, "accessTokenExpirationMs", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(anotherProvider, "refreshTokenExpirationMs", REFRESH_EXPIRATION);

        String foreignToken = anotherProvider.generateAccessToken(testPrincipal);
        assertFalse(tokenProvider.validateToken(foreignToken));
    }

    @Test
    @DisplayName("Should generate a secure non-empty refresh token string")
    void generateRefreshToken_success() {
        String refreshToken1 = tokenProvider.generateRefreshToken();
        String refreshToken2 = tokenProvider.generateRefreshToken();

        assertNotNull(refreshToken1);
        assertNotNull(refreshToken2);
        assertNotEquals(refreshToken1, refreshToken2);
        assertTrue(refreshToken1.length() >= 32);
    }
}
