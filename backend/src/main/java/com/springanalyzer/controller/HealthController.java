package com.springanalyzer.controller;

import com.springanalyzer.dto.response.ApiResponse;
import com.springanalyzer.dto.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> healthCheck() {
        HealthResponse health = new HealthResponse(
            "UP",
            "Spring Analyzer",
            "1.0.0",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(health, "Application is healthy"));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "Service is reachable"));
    }
}
