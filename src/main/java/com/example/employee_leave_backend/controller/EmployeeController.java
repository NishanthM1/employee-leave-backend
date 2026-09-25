package com.example.employee_leave_backend.controller;

import com.example.employee_leave_backend.dto.EmployeeRequest;
import com.example.employee_leave_backend.dto.EmployeeResponse;
import com.example.employee_leave_backend.dto.EmployeeUpdateRequest;
import com.example.employee_leave_backend.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
            employeeService.getEmployeeById(id, authentication.getName())
        );
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(employeeService.getMyProfile(authentication.getName()));
    }

    @GetMapping("/my-team")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<EmployeeResponse>> getMyTeam(Authentication authentication) {
        return ResponseEntity.ok(employeeService.getMyTeam(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(Authentication authentication) {

        return ResponseEntity.ok(
                employeeService.getAllEmployees(authentication.getName())
        );
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        return ResponseEntity.ok(
                employeeService.createEmployee(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {

        return ResponseEntity.ok(
                employeeService.updateEmployee(id, request)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employeeService.deactivateEmployee(id)
        );
    }
}