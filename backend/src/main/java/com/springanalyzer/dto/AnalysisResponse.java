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
    private boolean isMultiModule;
    private List<String> modules;
    private AnalysisSummary summary;
    private List<ClassInfo> classes;
    private List<EndpointInfo> endpoints;
    private List<DependencyInfo> dependencies;
    private List<RelationshipInfo> relationships;
    private Map<String, ModuleSummary> moduleSummaries;

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
        private long relationships;
        private int moduleCount;
        private Map<String, Long> classTypeBreakdown;
        private Map<String, Long> httpMethodBreakdown;
        private Map<String, Long> relationshipTypeBreakdown;
    }

    @Data
    @Builder
    public static class ModuleSummary {
        private String moduleName;
        private long totalClasses;
        private long controllers;
        private long services;
        private long repositories;
        private long entities;
        private long endpoints;
        private long dependencies;
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
        private String moduleName;
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
        private String moduleName;
    }

    @Data
    @Builder
    public static class DependencyInfo {
        private Long id;
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
        private String moduleName;
    }

    @Data
    @Builder
    public static class RelationshipInfo {
        private Long id;
        private String sourceClass;
        private String targetClass;
        private String type;
        private String fieldName;
    }
}
