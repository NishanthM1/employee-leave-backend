package com.example.employee_leave_backend.service;

import com.example.employee_leave_backend.dto.LeaveTypeRequest;
import com.example.employee_leave_backend.dto.LeaveTypeResponse;
import com.example.employee_leave_backend.entity.LeaveBalance;
import com.example.employee_leave_backend.entity.LeaveType;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
        private final EmployeeRepository employeeRepository;
        private final LeaveBalanceRepository leaveBalanceRepository;

        public LeaveTypeService(LeaveTypeRepository leaveTypeRepository,
                                                        EmployeeRepository employeeRepository,
                                                        LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
                this.employeeRepository = employeeRepository;
                this.leaveBalanceRepository = leaveBalanceRepository;
    }

        @Transactional
    public LeaveTypeResponse createLeaveType(
            LeaveTypeRequest request) {

        LeaveType leaveType = new LeaveType();

        leaveType.setName(request.getName());
        leaveType.setYearlyDays(request.getYearlyDays());
        leaveType.setStatus("ACTIVE");

        LeaveType savedLeaveType =
                leaveTypeRepository.save(leaveType);

        employeeRepository.findAll().stream()
                .filter(employee -> "ACTIVE".equals(employee.getStatus()))
                .forEach(employee -> {
                    LeaveBalance balance = new LeaveBalance();
                    balance.setEmployee(employee);
                    balance.setLeaveType(savedLeaveType);
                    balance.setYear(java.time.LocalDate.now().getYear());
                    balance.setTotalDays(savedLeaveType.getYearlyDays());
                    balance.setUsedDays(0);
                    leaveBalanceRepository.save(balance);
                });

        return new LeaveTypeResponse(
                savedLeaveType.getId(),
                savedLeaveType.getName(),
                savedLeaveType.getYearlyDays(),
                savedLeaveType.getStatus()
        );
    }
    public List<LeaveTypeResponse> getAllLeaveTypes() {

        return leaveTypeRepository.findAll()
                .stream()
                .map(leaveType -> new LeaveTypeResponse(
                        leaveType.getId(),
                        leaveType.getName(),
                        leaveType.getYearlyDays(),
                        leaveType.getStatus()
                ))
                .toList();
    }

    public LeaveTypeResponse getLeaveTypeById(Long id) {

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave type not found"));

        return new LeaveTypeResponse(
                leaveType.getId(),
                leaveType.getName(),
                leaveType.getYearlyDays(),
                leaveType.getStatus()
        );
    }

    public LeaveTypeResponse updateLeaveType(
            Long id,
            LeaveTypeRequest request) {

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave type not found"));

        leaveType.setName(request.getName());
        leaveType.setYearlyDays(request.getYearlyDays());

        LeaveType updatedLeaveType =
                leaveTypeRepository.save(leaveType);

        return new LeaveTypeResponse(
                updatedLeaveType.getId(),
                updatedLeaveType.getName(),
                updatedLeaveType.getYearlyDays(),
                updatedLeaveType.getStatus()
        );
    }

    public LeaveTypeResponse deactivateLeaveType(Long id) {

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave type not found"));

        leaveType.setStatus("INACTIVE");

        LeaveType updatedLeaveType =
                leaveTypeRepository.save(leaveType);

        return new LeaveTypeResponse(
                updatedLeaveType.getId(),
                updatedLeaveType.getName(),
                updatedLeaveType.getYearlyDays(),
                updatedLeaveType.getStatus()
        );
    }
}