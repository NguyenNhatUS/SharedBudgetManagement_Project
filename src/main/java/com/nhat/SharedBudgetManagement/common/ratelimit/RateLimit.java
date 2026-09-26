package com.nhat.SharedBudgetManagement.common.ratelimit;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu các API Endpoint cần giới hạn tần suất gọi (Rate Limiting) phục vụ Giai đoạn 7.
 * Chống tấn công Brute-Force mật khẩu và spam DoS.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Số lượng requests tối đa được phép trong một cửa sổ thời gian.
     */
    int maxRequests() default 10;

    /**
     * Độ dài cửa sổ thời gian tính theo giây (ví dụ: 60 giây).
     */
    int windowSeconds() default 60;
}
