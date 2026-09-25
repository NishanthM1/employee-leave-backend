package com.example.employee_leave_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DepartmentRequest {

    @NotBlank
    private String name;
    private Long managerId;

    public DepartmentRequest() {
    }

    public DepartmentRequest(String name, Long managerId) {
        this.name = name;
        this.managerId = managerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
}