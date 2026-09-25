package com.example.employee_leave_backend.service;

import com.example.employee_leave_backend.dto.LeaveRequestRequest;
import com.example.employee_leave_backend.dto.LeaveRequestResponse;
import com.example.employee_leave_backend.entity.*;
import com.example.employee_leave_backend.exception.BadRequestException;
import com.example.employee_leave_backend.exception.ConflictException;
import com.example.employee_leave_backend.exception.ForbiddenException;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveRequestService {
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final DepartmentRepository departmentRepository;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository, LeaveTypeRepository leaveTypeRepository, LeaveBalanceRepository leaveBalanceRepository, UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.departmentRepository = departmentRepository;
    }

    // =========================
    // APPLY LEAVE
    // =========================
        @Transactional
        public LeaveRequestResponse applyLeave(LeaveRequestRequest request, String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Employee employee = employeeRepository.findByUserId(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

                if (!"ACTIVE".equals(employee.getStatus())) {
                        throw new BadRequestException("Inactive employees cannot apply for leave");
                }

        if (!request.getEmployeeId().equals(employee.getId())) {
            throw new ForbiddenException(
                    "You can only apply leave for yourself"
            );
        }
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId()).orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

                if (!"ACTIVE".equals(leaveType.getStatus())) {
                        throw new BadRequestException("Inactive leave types cannot be used");
                }


        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // Validate dates
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

                if (startDate.getYear() != endDate.getYear()) {
                        throw new BadRequestException("Leave requests cannot cross calendar years");
                }

        // Calculate leave days
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;

        // Find balance for requested year
        int year = startDate.getYear();

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), leaveType.getId(), year)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave balance not found"));

        // Check available balance
        int remainingDays =
                balance.getTotalDays()
                        - balance.getUsedDays();

        if (days > remainingDays) {
            throw new BadRequestException(
                    "Insufficient leave balance");
        }

        // Check overlapping leave
        boolean overlappingLeave =
                leaveRequestRepository
                        .existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                                employee.getId(),
                                endDate,
                                startDate,
                                List.of("PENDING", "APPROVED"));

        if (overlappingLeave) {
            throw new ConflictException(
                    "Leave request overlaps with an existing leave");
        }

        // Create leave request
        LeaveRequest leaveRequest =
                new LeaveRequest();

        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setDays(days);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus("PENDING");
        leaveRequest.setCreatedAt(LocalDateTime.now());

        LeaveRequest savedRequest =
                leaveRequestRepository.save(leaveRequest);

        return new LeaveRequestResponse(
                savedRequest.getId(),
                savedRequest.getEmployee().getId(),
                savedRequest.getLeaveType().getId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                savedRequest.getDays(),
                savedRequest.getReason(),
                savedRequest.getStatus(),
                null,
                null,
                null,
                savedRequest.getCreatedAt()
        );
    }

    // =========================
    // GET LEAVE REQUEST BY ID
    // =========================

    public LeaveRequestResponse getLeaveRequestById(
            Long id,
            String username) {

        LeaveRequest request = leaveRequestRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave request not found"));

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));

        // EMPLOYEE can only view their own request
        if (user.getRole().equals("EMPLOYEE")) {

            Employee employee = employeeRepository
                    .findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Employee not found"));

            if (!request.getEmployee().getId()
                    .equals(employee.getId())) {

                throw new ForbiddenException(
                        "You can only view your own leave requests");
            }
        }

                if (user.getRole().equals("MANAGER")) {
                        Department department = departmentRepository.findByManagerId(user.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
                        if (!request.getEmployee().getDepartment().getId().equals(department.getId())) {
                                throw new ForbiddenException("You can only view requests in your department");
                        }
                }

        return toResponse(request);
    }

    // =========================
    // GET ALL LEAVE REQUESTS
    // =========================

    public List<LeaveRequestResponse> getLeaveRequests(
            String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // =========================
        // EMPLOYEE
        // =========================

        if (user.getRole().equals("EMPLOYEE")) {

            Employee employee = employeeRepository
                    .findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Employee not found"));

            return leaveRequestRepository
                    .findAll()
                    .stream()
                    .filter(request ->
                            request.getEmployee()
                                    .getId()
                                    .equals(employee.getId()))
                    .map(this::toResponse)
                    .toList();
        }

        // =========================
        // MANAGER
        // =========================

        if (user.getRole().equals("MANAGER")) {

            Department department = departmentRepository
                    .findAll()
                    .stream()
                    .filter(d ->
                            d.getManager() != null
                                    && d.getManager()
                                    .getId()
                                    .equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Department not found"));

            return leaveRequestRepository
                    .findByEmployeeDepartmentId(department.getId())
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        // =========================
        // ADMIN
        // =========================

        if (user.getRole().equals("ADMIN")) {

            return leaveRequestRepository
                    .findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new ForbiddenException("Invalid user role");
    }

    // =========================
    // CANCEL LEAVE REQUEST
    // =========================

        @Transactional
        public LeaveRequestResponse cancelLeaveRequest(Long id, String username) {

        LeaveRequest request = leaveRequestRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new BadRequestException(
                    "Only pending leave requests can be cancelled");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (!request.getEmployee().getId().equals(employee.getId())) {
            throw new ForbiddenException("You can only cancel your own leave requests");
        }

        request.setStatus("CANCELLED");

        LeaveRequest updatedRequest =
                leaveRequestRepository.save(request);

        return toResponse(updatedRequest);
    }

    // =========================
    // CONVERT ENTITY TO RESPONSE
    // =========================

    private LeaveRequestResponse toResponse(
            LeaveRequest request) {

        Long reviewedBy =
                request.getReviewedBy() != null
                        ? request.getReviewedBy().getId()
                        : null;

        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getLeaveType().getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getDays(),
                request.getReason(),
                request.getStatus(),
                reviewedBy,
                request.getReviewedAt(),
                request.getRejectionReason(),
                request.getCreatedAt()
        );
    }

    // =========================
    // APPROVE LEAVE REQUEST
    // =========================

    @Transactional
    public LeaveRequestResponse approveLeave(
            Long leaveRequestId,
            String reviewerUsername) {

        // Find leave request
        LeaveRequest request = leaveRequestRepository
                .findById(leaveRequestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave request not found"));

        // Only PENDING can be approved
        if (!request.getStatus().equals("PENDING")) {
            throw new BadRequestException(
                    "Only pending leave requests can be approved");
        }

        // Find reviewer
        User reviewer = userRepository
                .findByUsername(reviewerUsername)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reviewer not found"));

                if (!"ADMIN".equals(reviewer.getRole()) && !"MANAGER".equals(reviewer.getRole())) {
                        throw new ForbiddenException("Only managers and admins can approve leave");
                }

        // =========================
        // CHECK MANAGER DEPARTMENT
        // =========================

        if (reviewer.getRole().equals("MANAGER")) {

            Department department = departmentRepository
                    .findAll()
                    .stream()
                    .filter(d ->
                            d.getManager() != null
                                    && d.getManager()
                                    .getId()
                                    .equals(reviewer.getId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new ForbiddenException(
                                    "Manager is not assigned to any department"));

            Long employeeDepartmentId =
                    request.getEmployee()
                            .getDepartment()
                            .getId();

            if (!employeeDepartmentId
                    .equals(department.getId())) {

                throw new ForbiddenException(
                        "Manager cannot approve leave for another department");
            }
        }

        // =========================
        // FIND LEAVE BALANCE
        // =========================

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        request.getEmployee().getId(),
                        request.getLeaveType().getId(),
                        request.getStartDate().getYear())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave balance not found"));

        // Check remaining balance
        int remainingDays =
                balance.getTotalDays()
                        - balance.getUsedDays();

        if (request.getDays() > remainingDays) {
            throw new BadRequestException(
                    "Insufficient leave balance");
        }

        // =========================
        // DEDUCT LEAVE BALANCE
        // =========================

        balance.setUsedDays(
                balance.getUsedDays()
                        + request.getDays());

        leaveBalanceRepository.save(balance);

        // =========================
        // APPROVE REQUEST
        // =========================

        request.setStatus("APPROVED");
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        LeaveRequest approvedRequest =
                leaveRequestRepository.save(request);

        return toResponse(approvedRequest);
    }

    // =========================
    // REJECT LEAVE REQUEST
    // =========================

    @Transactional
    public LeaveRequestResponse rejectLeave(
            Long leaveRequestId,
            String reviewerUsername,
            String rejectionReason) {

        // Find leave request
        LeaveRequest request = leaveRequestRepository
                .findById(leaveRequestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave request not found"));

        // Only PENDING can be rejected
        if (!request.getStatus().equals("PENDING")) {
            throw new BadRequestException(
                    "Only pending leave requests can be rejected");
        }

        // Find reviewer
        User reviewer = userRepository
                .findByUsername(reviewerUsername)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reviewer not found"));

                if (!"ADMIN".equals(reviewer.getRole()) && !"MANAGER".equals(reviewer.getRole())) {
                        throw new ForbiddenException("Only managers and admins can reject leave");
                }

        // =========================
        // CHECK MANAGER DEPARTMENT
        // =========================

        if (reviewer.getRole().equals("MANAGER")) {

            Department department = departmentRepository
                    .findAll()
                    .stream()
                    .filter(d ->
                            d.getManager() != null
                                    && d.getManager()
                                    .getId()
                                    .equals(reviewer.getId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new ForbiddenException(
                                    "Manager is not assigned to any department"));

            Long employeeDepartmentId =
                    request.getEmployee()
                            .getDepartment()
                            .getId();

            if (!employeeDepartmentId
                    .equals(department.getId())) {

                throw new ForbiddenException(
                        "Manager cannot reject leave for another department");
            }
        }

        // =========================
        // REJECT REQUEST
        // =========================

        request.setStatus("REJECTED");
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);

        LeaveRequest rejectedRequest =
                leaveRequestRepository.save(request);

        return toResponse(rejectedRequest);
    }
}