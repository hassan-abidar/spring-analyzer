package com.springanalyzer.controller;

import com.springanalyzer.dto.DataFlowResponse;
import com.springanalyzer.dto.response.ApiResponse;
import com.springanalyzer.service.DataFlowAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/dataflow")
@RequiredArgsConstructor
@Slf4j
public class DataFlowController {

    private final DataFlowAnalyzerService dataFlowAnalyzerService;

    @GetMapping
    public ResponseEntity<ApiResponse<DataFlowResponse>> getDataFlow(@PathVariable Long projectId) {
        log.info("Fetching data flow analysis for project: {}", projectId);
        
        try {
            DataFlowResponse response = dataFlowAnalyzerService.analyzeDataFlow(projectId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error analyzing data flow for project: {}", projectId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to analyze data flow: " + e.getMessage()));
        }
    }
}
