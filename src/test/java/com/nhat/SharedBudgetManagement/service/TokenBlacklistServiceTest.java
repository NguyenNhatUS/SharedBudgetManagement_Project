package com.nhat.SharedBudgetManagement.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for TokenBlacklistService")
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Should successfully store token in Redis blacklist with TTL")
    void blacklistToken_validToken_setsInRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = "valid_jwt_token_sample";
        long remainingMs = 300000L; // 5 minutes

        tokenBlacklistService.blacklistToken(token, remainingMs);

        verify(valueOperations).set(
                eq(TokenBlacklistService.BLACKLIST_PREFIX + token),
                eq("revoked"),
                eq(Duration.ofMillis(remainingMs))
        );
    }

    @Test
    @DisplayName("Should skip blacklisting when token is null or blank")
    void blacklistToken_nullOrBlank_doesNothing() {
        tokenBlacklistService.blacklistToken(null, 300000L);
        tokenBlacklistService.blacklistToken("", 300000L);
        tokenBlacklistService.blacklistToken("   ", 300000L);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("Should skip blacklisting when remaining TTL is zero or negative")
    void blacklistToken_nonPositiveTtl_doesNothing() {
        tokenBlacklistService.blacklistToken("sample_token", 0L);
        tokenBlacklistService.blacklistToken("sample_token", -5000L);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("Should return true when token exists in Redis blacklist")
    void isBlacklisted_tokenExists_returnsTrue() {
        String token = "blacklisted_token";
        String expectedKey = TokenBlacklistService.BLACKLIST_PREFIX + token;

        when(stringRedisTemplate.hasKey(expectedKey)).thenReturn(Boolean.TRUE);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertTrue(result);
        verify(stringRedisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should return false when token does not exist in Redis blacklist")
    void isBlacklisted_tokenNotExists_returnsFalse() {
        String token = "clean_token";
        String expectedKey = TokenBlacklistService.BLACKLIST_PREFIX + token;

        when(stringRedisTemplate.hasKey(expectedKey)).thenReturn(Boolean.FALSE);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertFalse(result);
        verify(stringRedisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should return false for null or blank token without querying Redis")
    void isBlacklisted_nullOrBlankToken_returnsFalse() {
        assertFalse(tokenBlacklistService.isBlacklisted(null));
        assertFalse(tokenBlacklistService.isBlacklisted(""));
        assertFalse(tokenBlacklistService.isBlacklisted("   "));

        verifyNoInteractions(stringRedisTemplate);
    }
}
