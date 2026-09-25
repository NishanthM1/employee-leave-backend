package com.example.employee_leave_backend.service;

import com.example.employee_leave_backend.dto.EmployeeRequest;
import com.example.employee_leave_backend.dto.EmployeeResponse;
import com.example.employee_leave_backend.dto.EmployeeUpdateRequest;
import com.example.employee_leave_backend.entity.Department;
import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.entity.LeaveBalance;
import com.example.employee_leave_backend.entity.LeaveType;
import com.example.employee_leave_backend.exception.ConflictException;
import com.example.employee_leave_backend.exception.ForbiddenException;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.repository.DepartmentRepository;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveTypeRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

        @Transactional
        public EmployeeResponse createEmployee(EmployeeRequest request) {

                if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                        throw new ConflictException("Username already exists");
                }

        // Find department
        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        // Create login user
        User user = new User();

        user.setUsername(request.getUsername());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole("EMPLOYEE");

        userRepository.save(user);

        // Create employee
        Employee employee = new Employee();

        employee.setUser(user);
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setDepartment(department);
        employee.setStatus("ACTIVE");

        Employee savedEmployee =
                employeeRepository.save(employee);

        List<LeaveType> activeLeaveTypes = leaveTypeRepository
                .findAll()
                .stream()
                .filter(leaveType ->
                        leaveType.getStatus().equals("ACTIVE"))
                .toList();

        for (LeaveType leaveType : activeLeaveTypes) {
            LeaveBalance balance = new LeaveBalance();

            balance.setEmployee(savedEmployee);
            balance.setLeaveType(leaveType);
            balance.setYear(LocalDate.now().getYear());
            balance.setTotalDays(leaveType.getYearlyDays());
            balance.setUsedDays(0);

            leaveBalanceRepository.save(balance);
        }

        // Return response
        return new EmployeeResponse(
                savedEmployee.getId(),
                savedEmployee.getEmployeeCode(),
                savedEmployee.getName(),
                savedEmployee.getEmail(),
                savedEmployee.getPhone(),
                savedEmployee.getJoiningDate(),
                savedEmployee.getDepartment().getId(),
                savedEmployee.getStatus()
        );
    }

        public EmployeeResponse getEmployeeById(Long id, String username) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if ("EMPLOYEE".equals(user.getRole())) {
                        Employee current = employeeRepository.findByUserId(user.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
                        if (!current.getId().equals(employee.getId())) {
                                throw new ForbiddenException("You can only view your own profile");
                        }
                }
                if ("MANAGER".equals(user.getRole())) {
                        Department department = departmentRepository.findByManagerId(user.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
                        if (!department.getId().equals(employee.getDepartment().getId())) {
                                throw new ForbiddenException("You can only view employees in your department");
                        }
                }
                return toResponse(employee);
    }

    public EmployeeResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
    }

    public List<EmployeeResponse> getMyTeam(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Department department = departmentRepository.findByManagerId(manager.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        return employeeRepository.findByDepartmentId(department.getId()).stream()
                .map(this::toResponse).toList();
    }
        public List<EmployeeResponse> getAllEmployees(String username) {

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if ("MANAGER".equals(user.getRole())) {
                        return getMyTeam(username);
                }
                if (!"ADMIN".equals(user.getRole())) {
                        throw new ForbiddenException("Only admins can view all employees");
                }

        return employeeRepository.findAll()
                .stream()
                .map(employee -> new EmployeeResponse(
                        employee.getId(),
                        employee.getEmployeeCode(),
                        employee.getName(),
                        employee.getEmail(),
                        employee.getPhone(),
                        employee.getJoiningDate(),
                        employee.getDepartment().getId(),
                        employee.getStatus()
                ))
                .toList();
    }

    public EmployeeResponse updateEmployee(
            Long id,
            EmployeeUpdateRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setDepartment(department);

        Employee updatedEmployee =
                employeeRepository.save(employee);

        return new EmployeeResponse(
                updatedEmployee.getId(),
                updatedEmployee.getEmployeeCode(),
                updatedEmployee.getName(),
                updatedEmployee.getEmail(),
                updatedEmployee.getPhone(),
                updatedEmployee.getJoiningDate(),
                updatedEmployee.getDepartment().getId(),
                updatedEmployee.getStatus()
        );
    }

    public EmployeeResponse deactivateEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        employee.setStatus("INACTIVE");

        Employee updatedEmployee =
                employeeRepository.save(employee);

        return toResponse(updatedEmployee);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getEmployeeCode(), employee.getName(),
                employee.getEmail(), employee.getPhone(), employee.getJoiningDate(),
                employee.getDepartment().getId(), employee.getStatus());
    }
}