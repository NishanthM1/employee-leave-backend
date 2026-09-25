package com.example.employee_leave_backend.dto;

public class LeaveTypeResponse {

    private Long id;
    private String name;
    private Integer yearlyDays;
    private String status;

    public LeaveTypeResponse() {
    }

    public LeaveTypeResponse(Long id, String name, Integer yearlyDays, String status) {
        this.id = id;
        this.name = name;
        this.yearlyDays = yearlyDays;
        this.status = status;
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

    public Integer getYearlyDays() {
        return yearlyDays;
    }

    public void setYearlyDays(Integer yearlyDays) {
        this.yearlyDays = yearlyDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}