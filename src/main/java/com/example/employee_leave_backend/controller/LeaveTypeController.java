package com.example.employee_leave_backend.controller;

import com.example.employee_leave_backend.dto.LeaveTypeRequest;
import com.example.employee_leave_backend.dto.LeaveTypeResponse;
import com.example.employee_leave_backend.service.LeaveTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @PostMapping
    public ResponseEntity<LeaveTypeResponse> createLeaveType(
            @Valid @RequestBody LeaveTypeRequest request) {

        return ResponseEntity.ok(
                leaveTypeService.createLeaveType(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<LeaveTypeResponse>> getAllLeaveTypes() {

        return ResponseEntity.ok(
                leaveTypeService.getAllLeaveTypes()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> getLeaveTypeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveTypeService.getLeaveTypeById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> updateLeaveType(
            @PathVariable Long id,
            @Valid @RequestBody LeaveTypeRequest request) {

        return ResponseEntity.ok(
                leaveTypeService.updateLeaveType(id, request)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<LeaveTypeResponse> deactivateLeaveType(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveTypeService.deactivateLeaveType(id)
        );
    }
}