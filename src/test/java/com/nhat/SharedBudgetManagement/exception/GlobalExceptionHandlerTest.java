package com.nhat.SharedBudgetManagement.exception;

import com.nhat.SharedBudgetManagement.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404")
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found with id: 99");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAppException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("User not found with id: 99", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle BadRequestException and return 400")
    void testHandleBadRequestException() {
        BadRequestException ex = new BadRequestException(ErrorCode.CANNOT_INVITE_OWNER, "Cannot invite a member with OWNER role");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAppException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Cannot invite a member with OWNER role", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ConflictException and return 409")
    void testHandleConflictException() {
        ConflictException ex = new ConflictException(ErrorCode.TAG_ALREADY_EXISTS, "Tag with name 'Food' already exists");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAppException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Tag with name 'Food' already exists", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ForbiddenException and return 403")
    void testHandleForbiddenException() {
        ForbiddenException ex = new ForbiddenException(ErrorCode.CANNOT_MODIFY_OWNER, "Cannot change the role of the OWNER");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAppException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Cannot change the role of the OWNER", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400 with detailed field errors")
    void testHandleValidationException() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "createBudgetRequest");
        bindingResult.addError(new FieldError("createBudgetRequest", "name", "Budget name cannot be blank"));
        bindingResult.addError(new FieldError("createBudgetRequest", "currency", "Currency must be 3 characters"));

        MethodParameter parameter = new MethodParameter(this.getClass().getDeclaredMethod("setUp"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidationExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Validation failed: 2 error(s)"));

        Map<String, String> errors = response.getBody().getData();
        assertNotNull(errors);
        assertEquals("Budget name cannot be blank", errors.get("name"));
        assertEquals("Currency must be 3 characters", errors.get("currency"));
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException and return 404")
    void testHandleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found in DB");

        ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Entity not found in DB", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid argument provided", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500 without leaking stacktrace")
    void testHandleGeneralException() {
        Exception ex = new RuntimeException("Unexpected database crash");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneralException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(), response.getBody().getMessage());
    }
}
