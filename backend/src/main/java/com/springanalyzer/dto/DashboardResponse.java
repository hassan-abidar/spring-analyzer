package com.springanalyzer.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    private Long projectId;
    private String projectName;
    private String status;
    private String analyzedAt;

    private MetricsInfo metrics;
    private SecuritySummary security;
    private ChartData charts;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MetricsInfo {
        private int totalFiles;
        private int totalLines;
        private int codeLines;
        private int commentLines;
        private int blankLines;
        private int totalPackages;
        private int maxPackageDepth;
        private double avgMethodsPerClass;
        private double avgFieldsPerClass;
        private int maxMethodsInClass;
        private int maxFieldsInClass;
        private String packageStructure;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SecuritySummary {
        private int totalIssues;
        private int critical;
        private int high;
        private int medium;
        private int low;
        private int info;
        private Map<String, Integer> byCategory;
        private List<SecurityIssueInfo> topIssues;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SecurityIssueInfo {
        private Long id;
        private String severity;
        private String category;
        private String title;
        private String description;
        private String fileName;
        private Integer lineNumber;
        private String recommendation;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartData {
        private Map<String, Integer> classTypeDistribution;
        private Map<String, Integer> httpMethodDistribution;
        private Map<String, Integer> dependencyByScope;
        private Map<String, Integer> relationshipTypes;
        private Map<String, Integer> securityBySeverity;
        private List<PackageInfo> topPackages;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PackageInfo {
        private String name;
        private int classCount;
    }
}
