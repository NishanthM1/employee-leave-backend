package com.example.employee_leave_backend.controller;

import com.example.employee_leave_backend.dto.DepartmentRequest;
import com.example.employee_leave_backend.dto.DepartmentResponse;
import com.example.employee_leave_backend.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {

        return ResponseEntity.ok(
                departmentService.createDepartment(request)
        );
    }

    @GetMapping
    public ResponseEntity<java.util.List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<DepartmentResponse> deactivateDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.deactivateDepartment(id));
    }

    @GetMapping("/me")
    public ResponseEntity<DepartmentResponse> getMyDepartment(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(departmentService.getManagerDepartment(authentication.getName()));
    }
}