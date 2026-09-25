package com.example.employee_leave_backend;

import com.example.employee_leave_backend.exception.ApiError;
import com.example.employee_leave_backend.exception.BadRequestException;
import com.example.employee_leave_backend.exception.ConflictException;
import com.example.employee_leave_backend.exception.ForbiddenException;
import com.example.employee_leave_backend.exception.GlobalExceptionHandler;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsResourceNotFoundTo404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("Employee not found"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Employee not found", response.getBody().message());
    }

    @Test
    void mapsBadRequestTo400() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new BadRequestException("Start date cannot be after end date"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Start date cannot be after end date", response.getBody().message());
    }

    @Test
    void mapsForbiddenTo403() {
        ResponseEntity<ApiError> response = handler.handleForbidden(
                new ForbiddenException("You can only apply leave for yourself"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("You can only apply leave for yourself", response.getBody().message());
    }

    @Test
    void mapsConflictTo409() {
        ResponseEntity<ApiError> response = handler.handleConflict(
                new ConflictException("Leave request overlaps with an existing leave"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Leave request overlaps with an existing leave", response.getBody().message());
    }

    @Test
    void mapsValidationFailureTo400() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "startDate", "must not be null"));

        ResponseEntity<ApiError> response = handler.handleValidation(
                new MethodArgumentNotValidException(null, bindingResult));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("startDate: must not be null", response.getBody().message());
    }

    @Test
    void mapsDatabaseAndOptimisticLockConflictsTo409WithoutLeakingDetails() {
        ResponseEntity<ApiError> databaseResponse = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("SQL password and constraint details"));
        ResponseEntity<ApiError> lockResponse = handler.handleOptimisticLock(
                new OptimisticLockException("internal entity details"));

        assertEquals(409, databaseResponse.getStatusCode().value());
        assertEquals("Resource conflict", databaseResponse.getBody().message());
        assertEquals(409, lockResponse.getStatusCode().value());
        assertEquals("Resource was modified by another request", lockResponse.getBody().message());
    }

    @Test
    void mapsUnexpectedErrorsTo500WithoutLeakingDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new RuntimeException("SQL password and stack trace details"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().message());
        assertNotEquals("SQL password and stack trace details", response.getBody().message());
    }
}
