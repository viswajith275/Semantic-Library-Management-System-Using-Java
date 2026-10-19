package com.library.backend.controller;

import com.library.backend.dto.DashboardDto.DashboardResponse;
import com.library.backend.security.SecurityUtils;
import com.library.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Library-wide stats plus the caller's own loans, pins and recommendations.
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard(SecurityUtils.getCurrentUser()));
    }
}
