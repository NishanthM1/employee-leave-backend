package com.example.employee_leave_backend.dto;

public class DepartmentResponse {

    private Long id;
    private String name;
    private String status;
    private Long managerId;

    public DepartmentResponse() {
    }

    public DepartmentResponse(Long id, String name, String status, Long managerId) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.managerId = managerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
}