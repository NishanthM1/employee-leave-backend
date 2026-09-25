package com.example.employee_leave_backend;

import com.example.employee_leave_backend.entity.Department;
import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.entity.LeaveBalance;
import com.example.employee_leave_backend.entity.LeaveRequest;
import com.example.employee_leave_backend.entity.LeaveType;
import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.repository.DepartmentRepository;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.LeaveBalanceRepository;
import com.example.employee_leave_backend.repository.LeaveRequestRepository;
import com.example.employee_leave_backend.repository.LeaveTypeRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import com.example.employee_leave_backend.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static io.jsonwebtoken.Jwts.builder;
import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAndConcurrencyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private PasswordEncoder passwordEncoder;

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
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    private User employeeUser;
    private Employee employee;
    private LeaveBalance balance;
    private LeaveRequest leaveRequest;
    private int year;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());

        Department department = new Department();
        department.setName("Security Department " + suffix);
        department.setStatus("ACTIVE");
        department = departmentRepository.save(department);

        employeeUser = new User();
        employeeUser.setUsername("security-employee-" + suffix);
        employeeUser.setPassword(passwordEncoder.encode("employee-password"));
        employeeUser.setRole("EMPLOYEE");
        employeeUser = userRepository.save(employeeUser);

        employee = new Employee();
        employee.setUser(employeeUser);
        employee.setEmployeeCode("SEC" + suffix.substring(suffix.length() - 8));
        employee.setName("Security Employee");
        employee.setEmail("security-" + suffix + "@example.com");
        employee.setJoiningDate(LocalDate.now().minusYears(1));
        employee.setDepartment(department);
        employee.setStatus("ACTIVE");
        employee = employeeRepository.save(employee);

        LeaveType leaveType = new LeaveType();
        leaveType.setName("Security Leave " + suffix);
        leaveType.setYearlyDays(10);
        leaveType.setStatus("ACTIVE");
        leaveType = leaveTypeRepository.save(leaveType);

        year = LocalDate.now().getYear();
        balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setTotalDays(10);
        balance.setUsedDays(0);
        balance = leaveBalanceRepository.save(balance);

        leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(LocalDate.of(year, 2, 5));
        leaveRequest.setEndDate(LocalDate.of(year, 2, 5));
        leaveRequest.setDays(1);
        leaveRequest.setStatus("PENDING");
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest = leaveRequestRepository.saveAndFlush(leaveRequest);
    }

    @Test
    void validJwtAuthenticatesAgainstDatabaseUser() throws Exception {
        String token = jwtService.generateToken(employeeUser.getUsername(), "ADMIN");

        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void missingMalformedInvalidSignatureExpiredAndUnknownJwtAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/employees/" + employee.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer malformed"))
                .andExpect(status().isUnauthorized());

        SecretKey wrongKey = hmacShaKeyFor(
                "a-different-signing-key-that-is-long-enough-123456".getBytes(StandardCharsets.UTF_8));
        String invalidSignature = builder()
                .subject(employeeUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();
        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignature))
                .andExpect(status().isUnauthorized());

        SecretKey signingKey = hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String expired = builder()
                .subject(employeeUser.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(signingKey)
                .compact();
        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        String unknownSubject = builder()
                .subject("unknown-user-" + System.nanoTime())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();
        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + unknownSubject))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modifiedRoleClaimCannotEscalateDatabaseEmployee() throws Exception {
        String token = jwtService.generateToken(employeeUser.getUsername(), "ADMIN");

        mockMvc.perform(get("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

        @Test
    void activeEmployeeCanLogin() {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                employeeUser.getUsername(), "employee-password"));
    }

    @Test
    void inactiveEmployeeLoginIsRejectedAndExistingJwtNoLongerAuthenticates() throws Exception {
        String token = jwtService.generateToken(employeeUser.getUsername(), "EMPLOYEE");
        employee.setStatus("INACTIVE");
        employeeRepository.save(employee);

        assertThrows(DisabledException.class,
                () -> authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        employeeUser.getUsername(), "employee-password")));

        mockMvc.perform(get("/api/employees/" + employee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCanLogin() {
        User manager = createNonEmployeeUser("MANAGER");

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                manager.getUsername(), "manager-password"));
    }

    @Test
    void adminCanLogin() {
        User admin = createNonEmployeeUser("ADMIN");

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                admin.getUsername(), "admin-password"));
        }

    @Test
    void leaveRequestVersionPreventsStaleApprovalAndRejectionOverwrite() {
        entityManager.clear();
        LeaveRequest first = leaveRequestRepository.findById(leaveRequest.getId()).orElseThrow();
        entityManager.detach(first);
        entityManager.clear();
        LeaveRequest second = leaveRequestRepository.findById(leaveRequest.getId()).orElseThrow();
        entityManager.detach(second);

        first.setStatus("APPROVED");
        leaveRequestRepository.saveAndFlush(first);
        entityManager.clear();

        second.setStatus("REJECTED");
        assertThrows(OptimisticLockingFailureException.class,
                () -> leaveRequestRepository.saveAndFlush(second));
    }

    @Test
    void leaveBalanceVersionPreventsStaleUpdates() {
        entityManager.clear();
        LeaveBalance first = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        entityManager.detach(first);
        entityManager.clear();
        LeaveBalance second = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        entityManager.detach(second);

        first.setUsedDays(1);
        leaveBalanceRepository.saveAndFlush(first);
        entityManager.clear();

        second.setUsedDays(2);
        assertThrows(OptimisticLockingFailureException.class,
                () -> leaveBalanceRepository.saveAndFlush(second));
    }

    @Test
    void liveSchemaContainsNonNullVersionColumns() {
        assertEquals(0L, ((Number) entityManager.createNativeQuery(
                "select count(*) from leave_balances where version is null").getSingleResult()).longValue());
        assertEquals(1L, ((Number) entityManager.createNativeQuery(
                "select count(*) from information_schema.columns where table_name='leave_balances' and column_name='version' and is_nullable='NO'").getSingleResult()).longValue());
        assertEquals(1L, ((Number) entityManager.createNativeQuery(
                "select count(*) from information_schema.columns where table_name='leave_requests' and column_name='version' and is_nullable='NO'").getSingleResult()).longValue());
    }

        private User createNonEmployeeUser(String role) {
                User user = new User();
                user.setUsername("security-" + role.toLowerCase() + "-" + System.nanoTime());
                user.setPassword(passwordEncoder.encode(role.toLowerCase() + "-password"));
                user.setRole(role);
                return userRepository.saveAndFlush(user);
        }
}
