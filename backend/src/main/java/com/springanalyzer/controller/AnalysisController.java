package com.springanalyzer.controller;

import com.springanalyzer.dto.AnalysisResponse;
import com.springanalyzer.dto.response.ApiResponse;
import com.springanalyzer.service.AnalysisService;
import com.springanalyzer.service.AnalysisResultService;
import com.springanalyzer.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisResultService analysisResultService;
    private final ExportService exportService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> startAnalysis(@PathVariable Long projectId) {
        analysisService.analyzeProjectAsync(projectId);
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Analysis started", "Project analysis has been queued"));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzeSync(@PathVariable Long projectId) {
        analysisService.analyzeProject(projectId);
        AnalysisResponse result = analysisResultService.getAnalysisResult(projectId);
        return ResponseEntity.ok(ApiResponse.success(result, "Analysis completed"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisResult(@PathVariable Long projectId) {
        AnalysisResponse result = analysisResultService.getAnalysisResult(projectId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(@PathVariable Long projectId) {
        AnalysisResponse result = analysisResultService.getAnalysisResult(projectId);
        byte[] json = exportService.exportAsJson(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getProjectName() + "-analysis.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long projectId) {
        AnalysisResponse result = analysisResultService.getAnalysisResult(projectId);
        byte[] markdown = exportService.exportAsMarkdown(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getProjectName() + "-analysis.md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(markdown);
    }
}
