package com.nhat.SharedBudgetManagement.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Interceptor kiểm tra và thực thi Rate Limiting dựa trên Redis Atomic Counter.
 * Tự động chặn và trả về HTTP Status 429 Too Many Requests khi vượt ngưỡng.
 * Tích hợp Graceful Degradation: Nếu Redis gặp sự cố, hệ thống cho phép request
 * đi tiếp để tránh gián đoạn dịch vụ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    public static final String RATE_LIMIT_PREFIX = "sbm:rate_limit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String redisKey = RATE_LIMIT_PREFIX + clientIp + ":" + endpoint;

        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();

        try {
            Long currentRequests = stringRedisTemplate.opsForValue().increment(redisKey);

            if (currentRequests != null && currentRequests == 1) {
                stringRedisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }

            if (currentRequests != null && currentRequests > maxRequests) {
                Long ttl = stringRedisTemplate.getExpire(redisKey);
                long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;

                log.warn("Rate limit exceeded for IP: {} on endpoint: {}. Requests: {}/{}, Retry-After: {}s",
                        clientIp, endpoint, currentRequests, maxRequests, retryAfter);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(retryAfter));

                String jsonResponse = String.format(
                        "{\"success\":false,\"message\":\"Too many requests have been sent from your IP address. Please try again in %d seconds.\",\"data\":null,\"timestamp\":\"%s\"}",
                        retryAfter, java.time.LocalDateTime.now());
                response.getWriter().write(jsonResponse);
                return false;
            }
        } catch (Exception ex) {
            // Graceful Degradation: Nếu Redis không khả dụng, ghi log và cho phép request
            // tiếp tục
            log.error("Redis rate limit check encountered an error, bypassing rate limit: {}", ex.getMessage());
            return true;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}