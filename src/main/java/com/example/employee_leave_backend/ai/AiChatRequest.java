package com.example.employee_leave_backend.ai;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {

    @NotBlank
    private String message;

    public AiChatRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
