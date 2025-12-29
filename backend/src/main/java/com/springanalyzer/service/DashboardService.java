package com.springanalyzer.service;

import com.springanalyzer.dto.DashboardResponse;
import com.springanalyzer.dto.DashboardResponse.*;
import com.springanalyzer.entity.*;
import com.springanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final AnalyzedClassRepository classRepository;
    private final EndpointRepository endpointRepository;
    private final DependencyRepository dependencyRepository;
    private final ClassRelationshipRepository relationshipRepository;
    private final SecurityIssueRepository securityIssueRepository;
    private final CodeMetricsRepository codeMetricsRepository;

    public DashboardResponse getDashboard(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        return DashboardResponse.builder()
            .projectId(project.getId())
            .projectName(project.getName())
            .status(project.getStatus().name())
            .analyzedAt(project.getAnalyzedAt() != null 
                ? project.getAnalyzedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : null)
            .metrics(buildMetrics(projectId))
            .security(buildSecuritySummary(projectId))
            .charts(buildChartData(projectId))
            .build();
    }

    private MetricsInfo buildMetrics(Long projectId) {
        return codeMetricsRepository.findByProjectId(projectId)
            .map(m -> MetricsInfo.builder()
                .totalFiles(m.getTotalFiles())
                .totalLines(m.getTotalLines())
                .codeLines(m.getCodeLines())
                .commentLines(m.getCommentLines())
                .blankLines(m.getBlankLines())
                .totalPackages(m.getTotalPackages())
                .maxPackageDepth(m.getMaxPackageDepth())
                .avgMethodsPerClass(m.getAvgMethodsPerClass())
                .avgFieldsPerClass(m.getAvgFieldsPerClass())
                .maxMethodsInClass(m.getMaxMethodsInClass())
                .maxFieldsInClass(m.getMaxFieldsInClass())
                .packageStructure(m.getPackageStructure())
                .build())
            .orElse(null);
    }

    private SecuritySummary buildSecuritySummary(Long projectId) {
        List<SecurityIssue> issues = securityIssueRepository.findByProjectId(projectId);
        
        Map<IssueSeverity, Long> bySeverity = issues.stream()
            .collect(Collectors.groupingBy(SecurityIssue::getSeverity, Collectors.counting()));
        
        Map<String, Integer> byCategory = issues.stream()
            .collect(Collectors.groupingBy(i -> i.getCategory().name(), 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        List<SecurityIssueInfo> topIssues = issues.stream()
            .sorted(Comparator.comparing(SecurityIssue::getSeverity))
            .limit(10)
            .map(this::toIssueInfo)
            .collect(Collectors.toList());

        return SecuritySummary.builder()
            .totalIssues(issues.size())
            .critical(bySeverity.getOrDefault(IssueSeverity.CRITICAL, 0L).intValue())
            .high(bySeverity.getOrDefault(IssueSeverity.HIGH, 0L).intValue())
            .medium(bySeverity.getOrDefault(IssueSeverity.MEDIUM, 0L).intValue())
            .low(bySeverity.getOrDefault(IssueSeverity.LOW, 0L).intValue())
            .info(bySeverity.getOrDefault(IssueSeverity.INFO, 0L).intValue())
            .byCategory(byCategory)
            .topIssues(topIssues)
            .build();
    }

    private SecurityIssueInfo toIssueInfo(SecurityIssue i) {
        return SecurityIssueInfo.builder()
            .id(i.getId())
            .severity(i.getSeverity().name())
            .category(i.getCategory().name())
            .title(i.getTitle())
            .description(i.getDescription())
            .fileName(i.getFileName())
            .lineNumber(i.getLineNumber())
            .recommendation(i.getRecommendation())
            .build();
    }

    private ChartData buildChartData(Long projectId) {
        List<AnalyzedClass> classes = classRepository.findByProjectId(projectId);
        List<Endpoint> endpoints = endpointRepository.findByProjectId(projectId);
        List<Dependency> dependencies = dependencyRepository.findByProjectId(projectId);
        List<ClassRelationship> relationships = relationshipRepository.findByProject_Id(projectId);
        List<SecurityIssue> issues = securityIssueRepository.findByProjectId(projectId);

        Map<String, Integer> classTypeDistribution = classes.stream()
            .collect(Collectors.groupingBy(c -> c.getType().name(), 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> httpMethodDistribution = endpoints.stream()
            .collect(Collectors.groupingBy(e -> e.getHttpMethod().name(), 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> dependencyByScope = dependencies.stream()
            .collect(Collectors.groupingBy(d -> d.getScope() != null ? d.getScope() : "compile", 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> relationshipTypes = relationships.stream()
            .collect(Collectors.groupingBy(r -> r.getType().name(), 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> securityBySeverity = issues.stream()
            .collect(Collectors.groupingBy(i -> i.getSeverity().name(), 
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        List<PackageInfo> topPackages = classes.stream()
            .collect(Collectors.groupingBy(AnalyzedClass::getPackageName, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> PackageInfo.builder()
                .name(e.getKey())
                .classCount(e.getValue().intValue())
                .build())
            .collect(Collectors.toList());

        return ChartData.builder()
            .classTypeDistribution(classTypeDistribution)
            .httpMethodDistribution(httpMethodDistribution)
            .dependencyByScope(dependencyByScope)
            .relationshipTypes(relationshipTypes)
            .securityBySeverity(securityBySeverity)
            .topPackages(topPackages)
            .build();
    }
}
