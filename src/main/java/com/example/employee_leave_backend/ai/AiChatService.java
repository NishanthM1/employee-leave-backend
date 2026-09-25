package com.example.employee_leave_backend.ai;

import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.entity.LeaveRequest;
import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.exception.AiServiceException;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveRequestRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class AiChatService {

    private static final int MAX_REQUESTS_IN_CONTEXT = 12;

    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public AiChatService(
            GeminiService geminiService,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveRequestRepository leaveRequestRepository) {
        this.geminiService = geminiService;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public AiChatResponse chat(AiChatRequest request, String authenticatedUsername) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new AiServiceException("Authenticated user is required");
        }

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new AiServiceException("Message is required");
        }

        EmployeeAiContext employeeContext = resolveEmployeeContext(authenticatedUsername);
        String prompt = buildPrompt(request.getMessage(), employeeContext);
        return new AiChatResponse(geminiService.generateReply(prompt));
    }

    private EmployeeAiContext resolveEmployeeContext(String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee record found for the authenticated user"));

        List<LeaveBalanceSummary> balances = leaveBalanceRepository.findByEmployeeId(employee.getId())
                .stream()
                .sorted(Comparator.comparing(balance -> balance.getLeaveType().getName()))
                .map(balance -> new LeaveBalanceSummary(
                        balance.getLeaveType().getName(),
                        balance.getYear(),
                        balance.getTotalDays(),
                        balance.getUsedDays(),
                        balance.getTotalDays() - balance.getUsedDays()))
                .toList();

        List<LeaveRequestSummary> requests = leaveRequestRepository.findByEmployeeId(employee.getId())
                .stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate, Comparator.reverseOrder())
                        .thenComparing(LeaveRequest::getCreatedAt, Comparator.reverseOrder()))
                .limit(MAX_REQUESTS_IN_CONTEXT)
                .map(request -> new LeaveRequestSummary(
                        request.getLeaveType() != null ? request.getLeaveType().getName() : "Unknown",
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getDays(),
                        request.getStatus(),
                        request.getRejectionReason()))
                .toList();

        return new EmployeeAiContext(
                employee.getName(),
                employee.getEmployeeCode(),
                employee.getDepartment() != null ? employee.getDepartment().getName() : "Unknown",
                balances,
                requests);
    }

    private String buildPrompt(String userQuestion, EmployeeAiContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("SYSTEM INSTRUCTION\n")
                .append(GeminiService.SYSTEM_INSTRUCTION)
                .append("\n\nEMPLOYEE CONTEXT\n")
                .append("Name: ").append(context.name())
                .append("\nEmployee Code: ").append(context.employeeCode())
                .append("\nDepartment: ").append(context.departmentName())
                .append("\n\nLEAVE BALANCES\n");

        if (context.leaveBalances().isEmpty()) {
            prompt.append("No leave balance information is available.\n");
        } else {
            for (LeaveBalanceSummary balance : context.leaveBalances()) {
                prompt.append(balance.leaveTypeName())
                        .append(":\n")
                        .append("Total: ").append(balance.totalDays())
                        .append("\nUsed: ").append(balance.usedDays())
                        .append("\nAvailable: ").append(balance.availableDays())
                        .append("\n");
            }
        }

        prompt.append("\nLEAVE REQUESTS\n");
        if (context.leaveRequests().isEmpty()) {
            prompt.append("No leave request history is available.\n");
        } else {
            int index = 1;
            for (LeaveRequestSummary request : context.leaveRequests()) {
                prompt.append(index)
                        .append(".\n")
                        .append("Leave Type: ").append(request.leaveTypeName())
                        .append("\nStart: ").append(request.startDate())
                        .append("\nEnd: ").append(request.endDate())
                        .append("\nDays: ").append(request.days())
                        .append("\nStatus: ").append(request.status())
                        .append("\n");
                if (request.rejectionReason() != null && !request.rejectionReason().isBlank()) {
                    prompt.append("Rejection Reason: ").append(request.rejectionReason()).append("\n");
                }
                index++;
            }
        }

        prompt.append("\nEND EMPLOYEE CONTEXT\n\nUSER QUESTION:\n")
                .append(userQuestion)
                .append("\n");

        return prompt.toString();
    }

    private record EmployeeAiContext(
            String name,
            String employeeCode,
            String departmentName,
            List<LeaveBalanceSummary> leaveBalances,
            List<LeaveRequestSummary> leaveRequests) {
    }

    private record LeaveBalanceSummary(
            String leaveTypeName,
            int year,
            int totalDays,
            int usedDays,
            int availableDays) {
    }

    private record LeaveRequestSummary(
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            int days,
            String status,
            String rejectionReason) {
    }
}
