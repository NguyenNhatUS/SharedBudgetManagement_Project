package com.nhat.SharedBudgetManagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 500 Internal Server Error
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized server error", HttpStatus.INTERNAL_SERVER_ERROR),

    // 400 Bad Request
    VALIDATION_FAILED(1001, "Validation failed", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1002, "Bad request", HttpStatus.BAD_REQUEST),
    INVALID_INVITE_TOKEN(1003, "Invalid or expired invite token", HttpStatus.BAD_REQUEST),
    CANNOT_INVITE_OWNER(1004, "Cannot invite a member with OWNER role", HttpStatus.BAD_REQUEST),
    CANNOT_ASSIGN_OWNER_ROLE(1005, "Cannot assign OWNER role to a member", HttpStatus.BAD_REQUEST),
    INVITATION_ALREADY_PROCESSED(1006, "Invitation has already been processed", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    UNAUTHORIZED(1007, "Unauthorized", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1021, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1022, "Invalid or revoked refresh token", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(1023, "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1024, "Access token has expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1025, "Invalid token signature or format", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    FORBIDDEN(1008, "Access denied", HttpStatus.FORBIDDEN),
    CANNOT_MODIFY_OWNER(1009, "Cannot modify or remove the OWNER of the budget", HttpStatus.FORBIDDEN),
    OWNER_CANNOT_LEAVE(1010, "OWNER cannot leave the budget without transferring ownership", HttpStatus.FORBIDDEN),
    MEMBERSHIP_NOT_ACCEPTED(1011, "User's membership is not active or accepted", HttpStatus.FORBIDDEN),

    // 404 Not Found
    RESOURCE_NOT_FOUND(1012, "Resource not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1013, "User not found", HttpStatus.NOT_FOUND),
    BUDGET_NOT_FOUND(1014, "Budget not found", HttpStatus.NOT_FOUND),
    MEMBER_NOT_FOUND(1015, "Member not found in budget", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND(1016, "Transaction not found", HttpStatus.NOT_FOUND),
    TAG_NOT_FOUND(1017, "Tag not found", HttpStatus.NOT_FOUND),

    // 409 Conflict
    RESOURCE_ALREADY_EXISTS(1018, "Resource already exists", HttpStatus.CONFLICT),
    TAG_ALREADY_EXISTS(1019, "Tag name already exists", HttpStatus.CONFLICT),
    MEMBER_ALREADY_EXISTS(1020, "User is already a member of this budget", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1026, "Email is already registered", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
