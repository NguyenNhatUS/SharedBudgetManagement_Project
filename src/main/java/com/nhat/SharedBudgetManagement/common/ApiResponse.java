package com.nhat.SharedBudgetManagement.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder()
                .status(204)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> error(int status, String message) {
        return ApiResponse.<Void>builder()
                .status(status)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .build();
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String customMessage) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getHttpStatus().value())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage, T data) {
        return ApiResponse.<T>builder()
                .status(errorCode.getHttpStatus().value())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .data(data)
                .build();
    }
}