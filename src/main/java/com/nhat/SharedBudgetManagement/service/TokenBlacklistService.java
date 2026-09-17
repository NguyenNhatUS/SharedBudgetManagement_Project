package com.nhat.SharedBudgetManagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service quản lý Token Blacklist trên Redis phục vụ Giai đoạn 7.
 * Thu hồi tức thì Access Token khi người dùng Logout hoặc khi Token bị Revoke.
 * TTL được thiết lập đúng bằng thời gian sống còn lại của JWT, tự giải phóng RAM khi hết hạn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    public static final String BLACKLIST_PREFIX = "sbm:jwt:blacklist:";

    /**
     * Đưa Token vào Blacklist trên Redis với thời hạn TTL còn lại.
     * @param token JWT token cần thu hồi
     * @param remainingTimeMs Thời gian sống còn lại tính bằng mili-giây
     */
    public void blacklistToken(String token, long remainingTimeMs) {
        if (token == null || token.isBlank()) {
            return;
        }

        if (remainingTimeMs > 0) {
            String redisKey = BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue().set(redisKey, "revoked", Duration.ofMillis(remainingTimeMs));
            log.info("Token successfully blacklisted in Redis with TTL: {} ms", remainingTimeMs);
        } else {
            log.debug("Token is already expired or has non-positive remaining time, skipping blacklist");
        }
    }

    /**
     * Kiểm tra xem Token có đang nằm trong Blacklist hay không.
     * @param token JWT token cần kiểm tra
     * @return true nếu token đã bị thu hồi
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String redisKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey));
    }
}
