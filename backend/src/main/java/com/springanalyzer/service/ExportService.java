package com.springanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.springanalyzer.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ObjectMapper objectMapper;

    public byte[] exportAsJson(AnalysisResponse result) {
        try {
            ObjectMapper prettyMapper = objectMapper.copy();
            prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return prettyMapper.writeValueAsBytes(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export as JSON", e);
        }
    }

    public byte[] exportAsMarkdown(AnalysisResponse result) {
        StringBuilder md = new StringBuilder();
        
        md.append("# Analysis Report: ").append(result.getProjectName()).append("\n\n");
        md.append("**Status:** ").append(result.getStatus()).append("\n\n");
        
        if (result.getSummary() != null) {
            var s = result.getSummary();
            md.append("## Summary\n\n");
            md.append("| Metric | Count |\n");
            md.append("|--------|-------|\n");
            md.append("| Total Classes | ").append(s.getTotalClasses()).append(" |\n");
            md.append("| Controllers | ").append(s.getControllers()).append(" |\n");
            md.append("| Services | ").append(s.getServices()).append(" |\n");
            md.append("| Repositories | ").append(s.getRepositories()).append(" |\n");
            md.append("| Entities | ").append(s.getEntities()).append(" |\n");
            md.append("| Endpoints | ").append(s.getEndpoints()).append(" |\n");
            md.append("| Dependencies | ").append(s.getDependencies()).append(" |\n");
            md.append("| Relationships | ").append(s.getRelationships()).append(" |\n\n");
        }
        
        if (result.getClasses() != null && !result.getClasses().isEmpty()) {
            md.append("## Classes\n\n");
            result.getClasses().forEach(c -> {
                md.append("### ").append(c.getName()).append("\n");
                md.append("- **Package:** ").append(c.getPackageName()).append("\n");
                md.append("- **Type:** ").append(c.getType()).append("\n");
                md.append("- **Fields:** ").append(c.getFieldCount()).append("\n");
                md.append("- **Methods:** ").append(c.getMethodCount()).append("\n");
                if (c.getAnnotations() != null && !c.getAnnotations().isEmpty()) {
                    md.append("- **Annotations:** ").append(String.join(", ", c.getAnnotations())).append("\n");
                }
                md.append("\n");
            });
        }
        
        if (result.getEndpoints() != null && !result.getEndpoints().isEmpty()) {
            md.append("## REST Endpoints\n\n");
            md.append("| Method | Path | Handler | Return |\n");
            md.append("|--------|------|---------|--------|\n");
            result.getEndpoints().forEach(e -> {
                md.append("| ").append(e.getHttpMethod());
                md.append(" | ").append(e.getPath());
                md.append(" | ").append(e.getClassName() != null ? e.getClassName() + "." : "").append(e.getMethodName()).append("()");
                md.append(" | ").append(e.getReturnType());
                md.append(" |\n");
            });
            md.append("\n");
        }
        
        if (result.getDependencies() != null && !result.getDependencies().isEmpty()) {
            md.append("## Dependencies\n\n");
            md.append("| Group | Artifact | Version | Scope |\n");
            md.append("|-------|----------|---------|-------|\n");
            result.getDependencies().forEach(d -> {
                md.append("| ").append(d.getGroupId());
                md.append(" | ").append(d.getArtifactId());
                md.append(" | ").append(d.getVersion() != null ? d.getVersion() : "-");
                md.append(" | ").append(d.getScope());
                md.append(" |\n");
            });
            md.append("\n");
        }
        
        if (result.getRelationships() != null && !result.getRelationships().isEmpty()) {
            md.append("## Class Relationships\n\n");
            md.append("| Source | Type | Target | Field |\n");
            md.append("|--------|------|--------|-------|\n");
            result.getRelationships().forEach(r -> {
                md.append("| ").append(r.getSourceClass());
                md.append(" | ").append(r.getType());
                md.append(" | ").append(r.getTargetClass());
                md.append(" | ").append(r.getFieldName() != null ? r.getFieldName() : "-");
                md.append(" |\n");
            });
        }
        
        return md.toString().getBytes(StandardCharsets.UTF_8);
    }
}
