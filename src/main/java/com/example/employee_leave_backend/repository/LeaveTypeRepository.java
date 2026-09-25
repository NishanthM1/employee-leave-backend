package com.example.employee_leave_backend.repository;

import com.example.employee_leave_backend.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
}