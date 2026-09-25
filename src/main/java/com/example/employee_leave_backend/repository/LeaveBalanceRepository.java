package com.example.employee_leave_backend.repository;

import com.example.employee_leave_backend.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

	Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(
			Long employeeId, Long leaveTypeId, Integer year);

	List<LeaveBalance> findByEmployeeId(Long employeeId);
}