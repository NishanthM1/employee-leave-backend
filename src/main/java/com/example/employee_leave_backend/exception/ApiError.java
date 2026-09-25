package com.example.employee_leave_backend.exception;

import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime timestamp) {
}
