package com.example.employee_leave_backend.service;

import com.example.employee_leave_backend.dto.LeaveBalanceResponse;
import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.entity.LeaveBalance;
import com.example.employee_leave_backend.entity.LeaveType;
import com.example.employee_leave_backend.exception.BadRequestException;
import com.example.employee_leave_backend.exception.ConflictException;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveTypeRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
        private final UserRepository userRepository;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            UserRepository userRepository) {

        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.userRepository = userRepository;
    }

    public LeaveBalanceResponse createBalance(
            Long employeeId,
            Long leaveTypeId,
            Integer year,
            Integer totalDays) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave type not found"));

                if (year == null || year < 1 || totalDays == null || totalDays < 0) {
                        throw new BadRequestException("Year and total days must be valid");
                }
                if (leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year).isPresent()) {
                        throw new ConflictException("Leave balance already exists");
                }

        LeaveBalance balance = new LeaveBalance();

        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(0);

        LeaveBalance savedBalance =
                leaveBalanceRepository.save(balance);

        return new LeaveBalanceResponse(
                savedBalance.getId(),
                savedBalance.getEmployee().getId(),
                savedBalance.getLeaveType().getId(),
                savedBalance.getYear(),
                savedBalance.getTotalDays(),
                savedBalance.getUsedDays(),
                savedBalance.getTotalDays()
                        - savedBalance.getUsedDays()
        );
    }
    public LeaveBalanceResponse getBalanceById(Long id) {

        LeaveBalance balance = leaveBalanceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave balance not found"));

        return new LeaveBalanceResponse(
                balance.getId(),
                balance.getEmployee().getId(),
                balance.getLeaveType().getId(),
                balance.getYear(),
                balance.getTotalDays(),
                balance.getUsedDays(),
                balance.getTotalDays() - balance.getUsedDays()
        );
    }

    public List<LeaveBalanceResponse> getAllBalances() {

        return leaveBalanceRepository.findAll()
                .stream()
                .map(balance -> new LeaveBalanceResponse(
                        balance.getId(),
                        balance.getEmployee().getId(),
                        balance.getLeaveType().getId(),
                        balance.getYear(),
                        balance.getTotalDays(),
                        balance.getUsedDays(),
                        balance.getTotalDays() - balance.getUsedDays()
                ))
                .toList();
    }

        public List<LeaveBalanceResponse> getMyBalances(String username) {
                Long userId = userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")).getId();
                Employee employee = employeeRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
                return leaveBalanceRepository.findByEmployeeId(employee.getId()).stream()
                        .map(balance -> new LeaveBalanceResponse(balance.getId(), balance.getEmployee().getId(),
                                balance.getLeaveType().getId(), balance.getYear(), balance.getTotalDays(),
                                balance.getUsedDays(), balance.getTotalDays() - balance.getUsedDays()))
                        .toList();
        }
}