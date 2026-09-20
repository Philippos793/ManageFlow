package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.response.AdminDashboardSummaryResponse;
import com.philippos.employeemanagement.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin-summary")
    public AdminDashboardSummaryResponse getAdminSummary() {
        return dashboardService.getAdminSummary();
    }
}