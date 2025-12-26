package com.springanalyzer.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResponse {
    private Long projectId;
    private String projectName;
    private String status;
    private AnalysisSummary summary;
    private List<ClassInfo> classes;
    private List<EndpointInfo> endpoints;
    private List<DependencyInfo> dependencies;

    @Data
    @Builder
    public static class AnalysisSummary {
        private long totalClasses;
        private long controllers;
        private long services;
        private long repositories;
        private long entities;
        private long endpoints;
        private long dependencies;
        private Map<String, Long> classTypeBreakdown;
        private Map<String, Long> httpMethodBreakdown;
    }

    @Data
    @Builder
    public static class ClassInfo {
        private Long id;
        private String name;
        private String packageName;
        private String type;
        private List<String> annotations;
        private String extendsClass;
        private List<String> implementsInterfaces;
        private int fieldCount;
        private int methodCount;
    }

    @Data
    @Builder
    public static class EndpointInfo {
        private Long id;
        private String httpMethod;
        private String path;
        private String methodName;
        private String returnType;
        private String className;
    }

    @Data
    @Builder
    public static class DependencyInfo {
        private Long id;
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
    }
}
