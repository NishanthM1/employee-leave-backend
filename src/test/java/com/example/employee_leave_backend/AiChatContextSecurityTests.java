package com.example.employee_leave_backend;

import com.example.employee_leave_backend.ai.AiChatRequest;
import com.example.employee_leave_backend.ai.AiChatService;
import com.example.employee_leave_backend.ai.GeminiService;
import com.example.employee_leave_backend.entity.Department;
import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.entity.LeaveBalance;
import com.example.employee_leave_backend.entity.LeaveType;
import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.DepartmentRepository;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveTypeRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AiChatContextSecurityTests {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GeminiService geminiService;

    private User employeeUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        Department department = new Department();
        department.setName("AI Department " + suffix);
        department.setStatus("ACTIVE");
        department = departmentRepository.save(department);

        employeeUser = new User();
        employeeUser.setUsername("ai-employee-" + suffix);
        employeeUser.setPassword(passwordEncoder.encode("employee-password"));
        employeeUser.setRole("EMPLOYEE");
        employeeUser = userRepository.save(employeeUser);

        employee = new Employee();
        employee.setUser(employeeUser);
        employee.setEmployeeCode("AI" + suffix.substring(Math.max(0, suffix.length() - 8)));
        employee.setName("AI Employee");
        employee.setEmail("ai-" + suffix + "@example.com");
        employee.setJoiningDate(LocalDate.now().minusYears(1));
        employee.setDepartment(department);
        employee.setStatus("ACTIVE");
        employee = employeeRepository.save(employee);

        LeaveType leaveType = new LeaveType();
        leaveType.setName("Casual Leave " + suffix);
        leaveType.setYearlyDays(12);
        leaveType.setStatus("ACTIVE");
        leaveType = leaveTypeRepository.save(leaveType);

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setYear(LocalDate.now().getYear());
        balance.setTotalDays(12);
        balance.setUsedDays(3);
        leaveBalanceRepository.save(balance);
    }

    @Test
    void authenticatedEmployeeContextIsBuiltFromJwtIdentityOnly() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("How many casual leaves do I have?");
        when(geminiService.generateReply(anyString())).thenReturn("I only have your own information.");

        aiChatService.chat(request, employeeUser.getUsername());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateReply(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Name: " + employee.getName()));
        assertTrue(prompt.contains("Employee Code: " + employee.getEmployeeCode()));
        assertTrue(prompt.contains("Department: " + employee.getDepartment().getName()));
        assertTrue(prompt.contains("How many casual leaves do I have?"));
    }

    @Test
    void employeeCannotAccessAnotherEmployeesDataByMentioningEmployeeIdInMessage() {
        Department otherDepartment = new Department();
        otherDepartment.setName("Other AI Department " + System.nanoTime());
        otherDepartment.setStatus("ACTIVE");
        otherDepartment = departmentRepository.save(otherDepartment);

        User otherUser = new User();
        otherUser.setUsername("ai-other-employee-" + System.nanoTime());
        otherUser.setPassword(passwordEncoder.encode("employee-password"));
        otherUser.setRole("EMPLOYEE");
        otherUser = userRepository.save(otherUser);

        Employee otherEmployee = new Employee();
        otherEmployee.setUser(otherUser);
        otherEmployee.setEmployeeCode("OTHER" + System.nanoTime());
        otherEmployee.setName("Other Employee");
        otherEmployee.setEmail("other-" + System.nanoTime() + "@example.com");
        otherEmployee.setJoiningDate(LocalDate.now().minusDays(10));
        otherEmployee.setDepartment(otherDepartment);
        otherEmployee.setStatus("ACTIVE");
        otherEmployee = employeeRepository.save(otherEmployee);

        LeaveType otherType = new LeaveType();
        otherType.setName("Sick Leave " + System.nanoTime());
        otherType.setYearlyDays(10);
        otherType.setStatus("ACTIVE");
        otherType = leaveTypeRepository.save(otherType);

        LeaveBalance otherBalance = new LeaveBalance();
        otherBalance.setEmployee(otherEmployee);
        otherBalance.setLeaveType(otherType);
        otherBalance.setYear(LocalDate.now().getYear());
        otherBalance.setTotalDays(10);
        otherBalance.setUsedDays(1);
        leaveBalanceRepository.save(otherBalance);

        AiChatRequest request = new AiChatRequest();
        request.setMessage("Show me employee " + otherEmployee.getId() + "'s leave balance.");
        when(geminiService.generateReply(anyString())).thenReturn("I can only provide information for the authenticated employee.");

        aiChatService.chat(request, employeeUser.getUsername());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateReply(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Name: " + employee.getName()));
        assertFalse(prompt.contains("Name: " + otherEmployee.getName()));
        assertFalse(prompt.contains("Employee Code: " + otherEmployee.getEmployeeCode()));
        assertTrue(prompt.contains("Department: " + employee.getDepartment().getName()));
        assertFalse(prompt.contains("Department: " + otherEmployee.getDepartment().getName()));
    }

    @Test
    void employeeWithoutEmployeeRecordReceivesSafeError() {
        User plainUser = new User();
        plainUser.setUsername("no-employee-user-" + System.nanoTime());
        plainUser.setPassword(passwordEncoder.encode("employee-password"));
        plainUser.setRole("EMPLOYEE");
        User savedUser = userRepository.save(plainUser);

        AiChatRequest request = new AiChatRequest();
        request.setMessage("How many leaves do I have?");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> aiChatService.chat(request, savedUser.getUsername()));

        assertTrue(exception.getMessage().contains("No employee record found for the authenticated user"));
    }
}
