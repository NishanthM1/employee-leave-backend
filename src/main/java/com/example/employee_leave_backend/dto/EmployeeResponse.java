package com.example.employee_leave_backend.dto;

import java.time.LocalDate;

public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String name;
    private String email;
    private String phone;
    private LocalDate joiningDate;
    private Long departmentId;
    private String status;

    public EmployeeResponse() {
    }

    public EmployeeResponse(Long id, String employeeCode, String name,
                            String email, String phone, LocalDate joiningDate,
                            Long departmentId, String status) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joiningDate = joiningDate;
        this.departmentId = departmentId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}