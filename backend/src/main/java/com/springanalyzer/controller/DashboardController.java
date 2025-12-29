package com.springanalyzer.controller;

import com.springanalyzer.dto.DashboardResponse;
import com.springanalyzer.dto.response.ApiResponse;
import com.springanalyzer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(@PathVariable Long projectId) {
        DashboardResponse dashboard = dashboardService.getDashboard(projectId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
