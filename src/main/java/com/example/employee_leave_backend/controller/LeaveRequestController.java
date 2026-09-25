package com.example.employee_leave_backend.controller;

import com.example.employee_leave_backend.dto.LeaveRequestRequest;
import com.example.employee_leave_backend.dto.LeaveRequestResponse;
import com.example.employee_leave_backend.service.LeaveRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(
            LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    // =========================
    // APPLY LEAVE
    // =========================

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> applyLeave(
            @Valid @RequestBody LeaveRequestRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                leaveRequestService.applyLeave(
                        request,
                        username
                )
        );
    }

    // =========================
    // GET ALL
    // =========================

    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>>
    getLeaveRequests(Authentication authentication) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                leaveRequestService.getLeaveRequests(username));
    }


    // =========================
    // GET REQUEST BY ID
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse>
    getLeaveRequestById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                leaveRequestService.getLeaveRequestById(
                        id,
                        username
                )
        );
    }

    // =========================
    // CANCEL
    // =========================

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse>
    cancelLeaveRequest(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveRequestService.cancelLeaveRequest(id, authentication.getName())
        );
    }

    // =========================
    // APPROVE
    // =========================

    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse>
    approveLeave(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveRequestService.approveLeave(
                        id,
                        authentication.getName()
                )
        );
    }

    // =========================
    // REJECT
    // =========================

    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse>
    rejectLeave(
            @PathVariable Long id,
            @RequestParam String rejectionReason,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveRequestService.rejectLeave(
                        id,
                        authentication.getName(),
                        rejectionReason
                )
        );
    }
}