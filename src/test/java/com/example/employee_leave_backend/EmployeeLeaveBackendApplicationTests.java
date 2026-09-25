package com.example.employee_leave_backend;

import com.example.employee_leave_backend.dto.LeaveRequestRequest;
import com.example.employee_leave_backend.dto.LeaveRequestResponse;
import com.example.employee_leave_backend.dto.DepartmentRequest;
import com.example.employee_leave_backend.dto.EmployeeRequest;
import com.example.employee_leave_backend.dto.EmployeeUpdateRequest;
import com.example.employee_leave_backend.dto.LeaveTypeRequest;
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
import com.example.employee_leave_backend.service.DepartmentService;
import com.example.employee_leave_backend.service.EmployeeService;
import com.example.employee_leave_backend.service.LeaveRequestService;
import com.example.employee_leave_backend.service.LeaveTypeService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeLeaveBackendApplicationTests {

	@Autowired
	private LeaveRequestService leaveRequestService;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private DepartmentService departmentService;

	@Autowired
	private LeaveTypeService leaveTypeService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MockMvc mockMvc;

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

	private User employeeUser;
	private User managerUser;
	private User adminUser;
	private Employee employee;
	private Employee otherEmployee;
	private Employee outsideEmployee;
	private LeaveType leaveType;
	private LeaveBalance balance;
	private int testYear;

	@BeforeEach
	void setUp() {
		String suffix = String.valueOf(System.nanoTime());

		Department department = new Department();
		department.setName("Test Department " + suffix);
		department.setStatus("ACTIVE");
		department = departmentRepository.save(department);
		managerUser = createUser("manager-" + suffix, "MANAGER");
		adminUser = createUser("admin-" + suffix, "ADMIN");
		department.setManager(managerUser);
		department = departmentRepository.save(department);

		employee = createEmployee("employee-" + suffix, department);
		otherEmployee = createEmployee("other-" + suffix, department);
		employeeUser = employee.getUser();

		Department otherDepartment = new Department();
		otherDepartment.setName("Other Department " + suffix);
		otherDepartment.setStatus("ACTIVE");
		otherDepartment.setManager(createUser("outside-manager-" + suffix, "MANAGER"));
		otherDepartment = departmentRepository.save(otherDepartment);
		outsideEmployee = createEmployee("outside-" + suffix, otherDepartment);

		leaveType = new LeaveType();
		leaveType.setName("Test Leave " + suffix);
		leaveType.setYearlyDays(10);
		leaveType.setStatus("ACTIVE");
		leaveType = leaveTypeRepository.save(leaveType);

		testYear = LocalDate.now().getYear();
		balance = new LeaveBalance();
		balance.setEmployee(employee);
		balance.setLeaveType(leaveType);
		balance.setYear(testYear);
		balance.setTotalDays(10);
		balance.setUsedDays(0);
		balance = leaveBalanceRepository.save(balance);
	}

	@Test
	void employeeCanApplyLeaveForSelfAndDaysAreInclusive() {
		LeaveRequestResponse response = apply(employee.getId(), date(5), date(7));

		assertEquals(employee.getId(), response.getEmployeeId());
		assertEquals(3, response.getDays());
		assertEquals("PENDING", response.getStatus());
	}

	@Test
	void employeeCannotApplyLeaveForAnotherEmployee() {
		assertThrows(RuntimeException.class,
				() -> apply(otherEmployee.getId(), date(5), date(7)));
	}

	@Test
	void employeeCanViewOnlyOwnLeaveRequests() {
		LeaveRequest ownRequest = persistRequest("PENDING", employee, date(5), date(5));
		persistRequest("PENDING", otherEmployee, date(6), date(6));

		List<LeaveRequestResponse> requests =
				leaveRequestService.getLeaveRequests(employeeUser.getUsername());

		assertEquals(1, requests.size());
		assertEquals(ownRequest.getId(), requests.get(0).getId());
	}

	@Test
	void employeeCannotViewAnotherEmployeesRequestById() {
		LeaveRequest otherRequest = persistRequest("PENDING", otherEmployee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.getLeaveRequestById(
						otherRequest.getId(), employeeUser.getUsername()));
	}

	@Test
	void employeeCanCancelOwnPendingRequest() {
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(5));

		LeaveRequestResponse response = leaveRequestService.cancelLeaveRequest(
				request.getId(), employeeUser.getUsername());

		assertEquals("CANCELLED", response.getStatus());
	}

	@Test
	void employeeCannotCancelAnotherEmployeesRequest() {
		LeaveRequest request = persistRequest("PENDING", otherEmployee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.cancelLeaveRequest(
						request.getId(), employeeUser.getUsername()));
	}

	@Test
	void employeeCannotCancelApprovedRequest() {
		LeaveRequest request = persistRequest("APPROVED", employee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.cancelLeaveRequest(
						request.getId(), employeeUser.getUsername()));
	}

	@Test
	void employeeCannotCancelRejectedRequest() {
		LeaveRequest request = persistRequest("REJECTED", employee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.cancelLeaveRequest(
						request.getId(), employeeUser.getUsername()));
	}

	@Test
	void startDateCannotBeAfterEndDate() {
		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(7), date(5)));
	}

	@Test
	void requestedDaysCannotExceedRemainingBalance() {
		balance.setTotalDays(2);
		leaveBalanceRepository.save(balance);

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(5), date(7)));
	}

	@Test
	void pendingOverlappingLeaveIsRejected() {
		persistRequest("PENDING", employee, date(5), date(7));

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(7), date(9)));
	}

	@Test
	void approvedOverlappingLeaveIsRejected() {
		persistRequest("APPROVED", employee, date(5), date(7));

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(7), date(9)));
	}

	@Test
	void inactiveEmployeeCannotApplyLeave() {
		employee.setStatus("INACTIVE");
		employeeRepository.save(employee);

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(5), date(5)));
	}

	@Test
	void inactiveLeaveTypeCannotBeUsed() {
		leaveType.setStatus("INACTIVE");
		leaveTypeRepository.save(leaveType);

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(5), date(5)));
	}

	@Test
	void crossYearLeaveIsRejected() {
		LocalDate startDate = LocalDate.of(testYear, 12, 31);
		LocalDate endDate = LocalDate.of(testYear + 1, 1, 1);

		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), startDate, endDate));
	}

	@Test
	void managerCanViewRequestsFromOwnDepartmentOnly() {
		LeaveRequest ownRequest = persistRequest("PENDING", employee, date(5), date(5));
		persistRequest("PENDING", outsideEmployee, date(6), date(6));

		List<LeaveRequestResponse> requests =
				leaveRequestService.getLeaveRequests(managerUser.getUsername());

		assertEquals(1, requests.size());
		assertEquals(ownRequest.getId(), requests.get(0).getId());
	}

	@Test
	void managerCannotViewAnotherDepartmentsRequestById() {
		LeaveRequest outsideRequest =
				persistRequest("PENDING", outsideEmployee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.getLeaveRequestById(
						outsideRequest.getId(), managerUser.getUsername()));
	}

	@Test
	void managerDepartmentIsResolvedFromAuthenticatedManager() {
		assertEquals(
			employee.getDepartment().getId(),
			departmentService.getManagerDepartment(managerUser.getUsername()).getId());
	}

	@Test
	void managerCanApproveOwnDepartmentRequestAndReviewerComesFromAuthenticatedUser() {
		balance.setTotalDays(12);
		balance.setUsedDays(5);
		leaveBalanceRepository.save(balance);
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(7));

		LeaveRequestResponse response = leaveRequestService.approveLeave(
				request.getId(), managerUser.getUsername());

		assertEquals("APPROVED", response.getStatus());
		assertEquals(managerUser.getId(), response.getReviewedBy());
		assertEquals(8, leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays());
	}

	@Test
	void managerCannotApproveAnotherDepartmentsRequest() {
		LeaveRequest request = persistRequest("PENDING", outsideEmployee, date(5), date(7));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.approveLeave(request.getId(), managerUser.getUsername()));
	}

	@Test
	void managerCanRejectOwnDepartmentRequestWithoutDeductingBalance() {
		balance.setTotalDays(12);
		balance.setUsedDays(5);
		leaveBalanceRepository.save(balance);
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(7));

		LeaveRequestResponse response = leaveRequestService.rejectLeave(
				request.getId(), managerUser.getUsername(), "Rejected for testing");

		assertEquals("REJECTED", response.getStatus());
		assertEquals(managerUser.getId(), response.getReviewedBy());
		assertEquals(5, leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays());
	}

	@Test
	void managerCannotRejectAnotherDepartmentsRequest() {
		LeaveRequest request = persistRequest("PENDING", outsideEmployee, date(5), date(7));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.rejectLeave(
						request.getId(), managerUser.getUsername(), "Rejected for testing"));
	}

	@Test
	void approvedRequestCannotBeApprovedAgainOrRejected() {
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(5));
		leaveRequestService.approveLeave(request.getId(), managerUser.getUsername());

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.approveLeave(request.getId(), managerUser.getUsername()));
		assertThrows(RuntimeException.class,
				() -> leaveRequestService.rejectLeave(
						request.getId(), managerUser.getUsername(), "Rejected for testing"));
	}

	@Test
	void rejectedRequestCannotBeApprovedOrRejectedAgain() {
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(5));
		leaveRequestService.rejectLeave(request.getId(), managerUser.getUsername(), "Rejected for testing");

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.approveLeave(request.getId(), managerUser.getUsername()));
		assertThrows(RuntimeException.class,
				() -> leaveRequestService.rejectLeave(
						request.getId(), managerUser.getUsername(), "Rejected again"));
	}

	@Test
	void cancelledRequestCannotBeApprovedOrRejected() {
		LeaveRequest request = persistRequest("CANCELLED", employee, date(5), date(5));

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.approveLeave(request.getId(), managerUser.getUsername()));
		assertThrows(RuntimeException.class,
				() -> leaveRequestService.rejectLeave(
						request.getId(), managerUser.getUsername(), "Rejected for testing"));
	}

	@Test
	void approvalDoesNotDeductBalanceTwice() {
		balance.setTotalDays(12);
		balance.setUsedDays(5);
		leaveBalanceRepository.save(balance);
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(7));

		leaveRequestService.approveLeave(request.getId(), managerUser.getUsername());
		int usedDaysAfterApproval =
				leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays();

		assertThrows(RuntimeException.class,
				() -> leaveRequestService.approveLeave(request.getId(), managerUser.getUsername()));
		assertEquals(usedDaysAfterApproval,
				leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays());
	}

	@Test
	void employeeCannotAccessApprovalOrRejectionEndpoints() throws Exception {
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
						employeeUser.getUsername(),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

		mockMvc.perform(patch("/api/leave-requests/1/approve")
					.with(authentication(authentication)))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/leave-requests/1/reject")
						.param("rejectionReason", "Rejected for testing")
						.with(authentication(authentication)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanListAllEmployees() {
		List<?> employees = employeeService.getAllEmployees(adminUser.getUsername());

		assertEquals(true, employees.stream().anyMatch(employeeResponse ->
				((com.example.employee_leave_backend.dto.EmployeeResponse) employeeResponse)
						.getId().equals(employee.getId())));
		assertEquals(true, employees.stream().anyMatch(employeeResponse ->
				((com.example.employee_leave_backend.dto.EmployeeResponse) employeeResponse)
						.getId().equals(outsideEmployee.getId())));
	}

	@Test
	void adminCanCreateEmployeeWithUserAndHashedPassword() {
		String suffix = String.valueOf(System.nanoTime());
		EmployeeRequest request = new EmployeeRequest(
				"created-" + suffix,
				"PlainPassword123",
				"CREATED" + suffix.substring(suffix.length() - 8),
				"Created Employee",
				"created-" + suffix + "@example.com",
				null,
				LocalDate.now().minusMonths(1),
				employee.getDepartment().getId());

		Long createdEmployeeId = employeeService.createEmployee(request).getId();
		Employee createdEmployee = employeeRepository.findById(createdEmployeeId).orElseThrow();
		User createdUser = userRepository.findById(createdEmployee.getUser().getId()).orElseThrow();

		assertEquals("EMPLOYEE", createdUser.getRole());
		assertEquals(false, "PlainPassword123".equals(createdUser.getPassword()));
		assertEquals(true, passwordEncoder.matches("PlainPassword123", createdUser.getPassword()));
		assertEquals(true, leaveBalanceRepository
				.findByEmployeeIdAndLeaveTypeIdAndYear(
						createdEmployeeId, leaveType.getId(), testYear)
				.isPresent());
	}

	@Test
	void adminCanUpdateEmployee() {
		String suffix = String.valueOf(System.nanoTime());
		EmployeeUpdateRequest request = new EmployeeUpdateRequest();
		request.setEmployeeCode("UPDATED" + suffix.substring(suffix.length() - 8));
		request.setName("Updated Employee");
		request.setEmail("updated-" + suffix + "@example.com");
		request.setPhone("9999999999");
		request.setJoiningDate(LocalDate.now().minusMonths(2));
		request.setDepartmentId(employee.getDepartment().getId());

		assertEquals("Updated Employee",
				employeeService.updateEmployee(employee.getId(), request).getName());
		assertEquals("Updated Employee",
				employeeRepository.findById(employee.getId()).orElseThrow().getName());
	}

	@Test
	void adminCanDeactivateEmployeeAndLeaveApplicationIsRejected() {
		employeeService.deactivateEmployee(employee.getId());

		assertEquals("INACTIVE", employeeRepository.findById(employee.getId()).orElseThrow().getStatus());
		assertThrows(RuntimeException.class,
				() -> apply(employee.getId(), date(5), date(5)));
	}

	@Test
	void adminCanCreateUpdateAndDeactivateDepartment() {
		DepartmentRequest createRequest = new DepartmentRequest(
				"Created Department " + System.nanoTime(), managerUser.getId());
		Long departmentId = departmentService.createDepartment(createRequest).getId();

		DepartmentRequest updateRequest = new DepartmentRequest(
				"Updated Department " + System.nanoTime(), managerUser.getId());
		assertEquals("Updated Department " + updateRequest.getName().substring("Updated Department ".length()),
				departmentService.updateDepartment(departmentId, updateRequest).getName());
		assertEquals("INACTIVE", departmentService.deactivateDepartment(departmentId).getStatus());
	}

	@Test
	void adminCanCreateUpdateAndDeactivateLeaveType() {
		String suffix = String.valueOf(System.nanoTime());
		LeaveTypeRequest createRequest = new LeaveTypeRequest("Created Leave " + suffix, 15);
		Long leaveTypeId = leaveTypeService.createLeaveType(createRequest).getId();

		LeaveTypeRequest updateRequest = new LeaveTypeRequest("Updated Leave " + suffix, 18);
		assertEquals("Updated Leave " + suffix,
				leaveTypeService.updateLeaveType(leaveTypeId, updateRequest).getName());
		assertEquals("INACTIVE", leaveTypeService.deactivateLeaveType(leaveTypeId).getStatus());
	}

	@Test
	void adminCanViewAllLeaveRequests() {
		LeaveRequest ownRequest = persistRequest("PENDING", employee, date(5), date(5));
		LeaveRequest outsideRequest = persistRequest("PENDING", outsideEmployee, date(6), date(6));

		List<LeaveRequestResponse> requests =
				leaveRequestService.getLeaveRequests(adminUser.getUsername());

		assertEquals(true, requests.stream().anyMatch(request -> request.getId().equals(ownRequest.getId())));
		assertEquals(true, requests.stream().anyMatch(request -> request.getId().equals(outsideRequest.getId())));
	}

	@Test
	void adminCanApproveRequestAndReviewerAndBalanceAreCorrect() {
		balance.setTotalDays(12);
		balance.setUsedDays(5);
		leaveBalanceRepository.save(balance);
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(7));

		LeaveRequestResponse response = leaveRequestService.approveLeave(
				request.getId(), adminUser.getUsername());

		assertEquals("APPROVED", response.getStatus());
		assertEquals(adminUser.getId(), response.getReviewedBy());
		assertEquals(8, leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays());
	}

	@Test
	void adminCanRejectRequestWithoutDeductingBalance() {
		balance.setTotalDays(12);
		balance.setUsedDays(5);
		leaveBalanceRepository.save(balance);
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(7));

		LeaveRequestResponse response = leaveRequestService.rejectLeave(
				request.getId(), adminUser.getUsername(), "Rejected by admin");

		assertEquals("REJECTED", response.getStatus());
		assertEquals(adminUser.getId(), response.getReviewedBy());
		assertEquals(5, leaveBalanceRepository.findById(balance.getId()).orElseThrow().getUsedDays());
	}

	@Test
	void adminCanAccessApprovalEndpointThroughSpringSecurity() throws Exception {
		LeaveRequest request = persistRequest("PENDING", employee, date(5), date(5));

		mockMvc.perform(patch("/api/leave-requests/" + request.getId() + "/approve")
					.with(authenticatedAs(adminUser)))
				.andExpect(status().isOk());
	}

	@Test
	void managerAndEmployeeCannotAccessAdminManagementEndpoints() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		String employeeJson = "{"
				+ "\"username\":\"endpoint-" + suffix + "\","
				+ "\"password\":\"Password123\","
				+ "\"employeeCode\":\"END" + suffix.substring(suffix.length() - 8) + "\","
				+ "\"name\":\"Endpoint Employee\","
				+ "\"email\":\"endpoint-" + suffix + "@example.com\","
				+ "\"joiningDate\":\"2025-01-01\","
				+ "\"departmentId\":" + employee.getDepartment().getId()
				+ "}";
		String departmentJson = "{\"name\":\"Endpoint Department " + suffix + "\"}";
		String leaveTypeJson = "{\"name\":\"Endpoint Leave " + suffix + "\",\"yearlyDays\":10}";

		mockMvc.perform(post("/api/employees")
					.contentType(APPLICATION_JSON)
					.content(employeeJson)
					.with(authenticatedAs(adminUser)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/employees")
					.contentType(APPLICATION_JSON)
					.content(employeeJson)
					.with(authenticatedAs(managerUser)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/employees")
					.contentType(APPLICATION_JSON)
					.content(employeeJson)
					.with(authenticatedAs(employeeUser)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/departments")
					.contentType(APPLICATION_JSON)
					.content(departmentJson)
					.with(authenticatedAs(managerUser)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/leave-types")
					.contentType(APPLICATION_JSON)
					.content(leaveTypeJson)
					.with(authenticatedAs(employeeUser)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/leave-balances")
					.param("employeeId", outsideEmployee.getId().toString())
					.param("leaveTypeId", leaveType.getId().toString())
					.param("year", String.valueOf(testYear))
					.param("totalDays", "10")
					.with(authenticatedAs(managerUser)))
				.andExpect(status().isForbidden());
	}

	private RequestPostProcessor authenticatedAs(User user) {
		return authentication(new UsernamePasswordAuthenticationToken(
				user.getUsername(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
	}

	private LeaveRequestResponse apply(Long employeeId, LocalDate startDate, LocalDate endDate) {
		LeaveRequestRequest request = new LeaveRequestRequest(
				employeeId,
				leaveType.getId(),
				startDate,
				endDate,
				"Test reason");

		return leaveRequestService.applyLeave(request, employeeUser.getUsername());
	}

	private Employee createEmployee(String username, Department department) {
		User user = createUser(username, "EMPLOYEE");

		Employee createdEmployee = new Employee();
		createdEmployee.setUser(user);
		createdEmployee.setEmployeeCode(
			"CODE-" + username.substring(0, 3) + username.substring(username.length() - 8));
		createdEmployee.setName(username);
		createdEmployee.setEmail(username + "@example.com");
		createdEmployee.setJoiningDate(LocalDate.now().minusYears(1));
		createdEmployee.setDepartment(department);
		createdEmployee.setStatus("ACTIVE");
		return employeeRepository.save(createdEmployee);
	}

	private User createUser(String username, String role) {
		User user = new User();
		user.setUsername(username);
		user.setPassword("encoded-password");
		user.setRole(role);
		return userRepository.save(user);
	}

	private LeaveRequest persistRequest(
			String status,
			Employee requestEmployee,
			LocalDate startDate,
			LocalDate endDate) {
		LeaveRequest request = new LeaveRequest();
		request.setEmployee(requestEmployee);
		request.setLeaveType(leaveType);
		request.setStartDate(startDate);
		request.setEndDate(endDate);
		request.setDays((int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1);
		request.setStatus(status);
		request.setCreatedAt(LocalDateTime.now());
		return leaveRequestRepository.saveAndFlush(request);
	}

	private LocalDate date(int dayOfMonth) {
		return LocalDate.of(testYear, 1, dayOfMonth);
	}

}
