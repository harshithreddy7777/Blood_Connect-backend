package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminDashboardResponse;
import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /**
     * Fetch comprehensive summary statistics and triage metrics for the administrative dashboard.
     * Accessible strictly by system administrators.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardSummary() {
        AdminDashboardResponse summary = adminDashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard summary statistics retrieved successfully", summary));
    }
}
