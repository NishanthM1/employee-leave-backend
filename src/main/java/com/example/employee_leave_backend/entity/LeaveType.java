package com.example.employee_leave_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer yearlyDays;

    @Column(nullable = false)
    private String status;

    public LeaveType() {
    }

    public LeaveType(Long id, String name, Integer yearlyDays, String status) {
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