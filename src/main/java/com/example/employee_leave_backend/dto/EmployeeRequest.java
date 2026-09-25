package com.example.employee_leave_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class EmployeeRequest {

    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
    @NotBlank
    private String employeeCode;
    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String email;
    private String phone;
    @NotNull
    private LocalDate joiningDate;
    @NotNull
    private Long departmentId;

    public EmployeeRequest() {
    }

    public EmployeeRequest(String username, String password, String employeeCode,
                           String name, String email, String phone,
                           LocalDate joiningDate, Long departmentId) {
        this.username = username;
        this.password = password;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joiningDate = joiningDate;
        this.departmentId = departmentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}