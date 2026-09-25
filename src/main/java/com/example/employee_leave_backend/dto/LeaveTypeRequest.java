package com.example.employee_leave_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LeaveTypeRequest {

    @NotBlank
    private String name;
    @NotNull
    @Positive
    private Integer yearlyDays;

    public LeaveTypeRequest() {
    }

    public LeaveTypeRequest(String name, Integer yearlyDays) {
        this.name = name;
        this.yearlyDays = yearlyDays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYearlyDays() {
        return yearlyDays;
    }

    public void setYearlyDays(Integer yearlyDays) {
        this.yearlyDays = yearlyDays;
    }
}