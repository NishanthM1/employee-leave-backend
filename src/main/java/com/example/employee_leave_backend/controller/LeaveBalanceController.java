package com.example.employee_leave_backend.controller;

import com.example.employee_leave_backend.dto.LeaveBalanceResponse;
import com.example.employee_leave_backend.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(
            LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @PostMapping
    public ResponseEntity<LeaveBalanceResponse> createBalance(
            @RequestParam Long employeeId,
            @RequestParam Long leaveTypeId,
            @RequestParam Integer year,
            @RequestParam Integer totalDays) {

        return ResponseEntity.ok(
                leaveBalanceService.createBalance(
                        employeeId,
                        leaveTypeId,
                        year,
                        totalDays
                )
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<LeaveBalanceResponse> getBalanceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveBalanceService.getBalanceById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<LeaveBalanceResponse>> getAllBalances() {

        return ResponseEntity.ok(
                leaveBalanceService.getAllBalances()
        );
    }

        @GetMapping("/me")
        public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(
                        org.springframework.security.core.Authentication authentication) {
                return ResponseEntity.ok(leaveBalanceService.getMyBalances(authentication.getName()));
        }
}