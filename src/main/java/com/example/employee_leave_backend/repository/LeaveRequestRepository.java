package com.example.employee_leave_backend.repository;

import com.example.employee_leave_backend.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate,
            List<String> statuses
    );

    List<LeaveRequest> findByEmployeeDepartmentId(Long departmentId);

    List<LeaveRequest> findByEmployeeId(Long employeeId);
}