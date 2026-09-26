package com.nhat.SharedBudgetManagement.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for RateLimitInterceptor")
class RateLimitInterceptorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    // Dummy method for reflection
    @RateLimit(maxRequests = 3, windowSeconds = 60)
    public void dummyRateLimitedMethod() {}

    public void dummyUnrestrictedMethod() {}

    @Test
    @DisplayName("Should allow request when handler is not a HandlerMethod")
    void preHandle_nonHandlerMethod_returnsTrue() throws Exception {
        Object nonHandler = new Object();
        boolean result = rateLimitInterceptor.preHandle(request, response, nonHandler);
        assertTrue(result);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("Should allow request when method has no @RateLimit annotation")
    void preHandle_noAnnotation_returnsTrue() throws Exception {
        RateLimit annotation = null;
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(annotation);

        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);
        assertTrue(result);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("Should allow request and set TTL on first request within limit")
    void preHandle_withinLimit_firstRequest_returnsTrue() throws Exception {
        RateLimit annotation = getClass().getMethod("dummyRateLimitedMethod").getAnnotation(RateLimit.class);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(annotation);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(stringRedisTemplate).expire(eq("sbm:rate_limit:192.168.1.100:/api/v1/auth/login"), eq(Duration.ofSeconds(60)));
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should block request and return HTTP 429 when request count exceeds limit")
    void preHandle_exceedsLimit_returnsFalseAnd429() throws Exception {
        RateLimit annotation = getClass().getMethod("dummyRateLimitedMethod").getAnnotation(RateLimit.class);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(annotation);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // Exceeds maxRequests = 3
        when(valueOperations.increment(anyString())).thenReturn(4L);
        when(stringRedisTemplate.getExpire(anyString())).thenReturn(45L);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "45");
        assertTrue(stringWriter.toString().contains("Quá nhiều yêu cầu"));
    }

    @Test
    @DisplayName("Should gracefully allow request when Redis encounters an exception (Graceful Degradation)")
    void preHandle_redisException_gracefulDegradationReturnsTrue() throws Exception {
        RateLimit annotation = getClass().getMethod("dummyRateLimitedMethod").getAnnotation(RateLimit.class);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(annotation);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
