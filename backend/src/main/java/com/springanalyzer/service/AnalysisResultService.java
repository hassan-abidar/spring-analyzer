package com.springanalyzer.service;

import com.springanalyzer.dto.AnalysisResponse;
import com.springanalyzer.dto.AnalysisResponse.*;
import com.springanalyzer.entity.*;
import com.springanalyzer.exception.ResourceNotFoundException;
import com.springanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisResultService {

    private final ProjectRepository projectRepository;
    private final AnalyzedClassRepository classRepository;
    private final EndpointRepository endpointRepository;
    private final DependencyRepository dependencyRepository;

    public AnalysisResponse getAnalysisResult(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<AnalyzedClass> classes = classRepository.findByProjectId(projectId);
        List<Endpoint> endpoints = endpointRepository.findByProjectId(projectId);
        List<Dependency> dependencies = dependencyRepository.findByProjectId(projectId);

        return AnalysisResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .status(project.getStatus().name())
                .summary(buildSummary(classes, endpoints, dependencies))
                .classes(classes.stream().map(this::toClassInfo).toList())
                .endpoints(endpoints.stream().map(this::toEndpointInfo).toList())
                .dependencies(dependencies.stream().map(this::toDependencyInfo).toList())
                .build();
    }

    private AnalysisSummary buildSummary(List<AnalyzedClass> classes, List<Endpoint> endpoints, List<Dependency> dependencies) {
        Map<String, Long> classTypeBreakdown = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getType().name(), Collectors.counting()));

        Map<String, Long> httpMethodBreakdown = endpoints.stream()
                .collect(Collectors.groupingBy(e -> e.getHttpMethod().name(), Collectors.counting()));

        return AnalysisSummary.builder()
                .totalClasses(classes.size())
                .controllers(countByType(classes, ClassType.CONTROLLER) + countByType(classes, ClassType.REST_CONTROLLER))
                .services(countByType(classes, ClassType.SERVICE))
                .repositories(countByType(classes, ClassType.REPOSITORY))
                .entities(countByType(classes, ClassType.ENTITY))
                .endpoints(endpoints.size())
                .dependencies(dependencies.size())
                .classTypeBreakdown(classTypeBreakdown)
                .httpMethodBreakdown(httpMethodBreakdown)
                .build();
    }

    private long countByType(List<AnalyzedClass> classes, ClassType type) {
        return classes.stream().filter(c -> c.getType() == type).count();
    }

    private ClassInfo toClassInfo(AnalyzedClass c) {
        return ClassInfo.builder()
                .id(c.getId())
                .name(c.getName())
                .packageName(c.getPackageName())
                .type(c.getType().name())
                .annotations(c.getAnnotations() != null ? Arrays.asList(c.getAnnotations().split(",")) : List.of())
                .extendsClass(c.getExtendsClass())
                .implementsInterfaces(c.getImplementsInterfaces() != null ? Arrays.asList(c.getImplementsInterfaces().split(",")) : List.of())
                .fieldCount(c.getFieldCount() != null ? c.getFieldCount() : 0)
                .methodCount(c.getMethodCount() != null ? c.getMethodCount() : 0)
                .build();
    }

    private EndpointInfo toEndpointInfo(Endpoint e) {
        return EndpointInfo.builder()
                .id(e.getId())
                .httpMethod(e.getHttpMethod().name())
                .path(e.getPath())
                .methodName(e.getMethodName())
                .returnType(e.getReturnType())
                .className(e.getAnalyzedClass() != null ? e.getAnalyzedClass().getName() : null)
                .build();
    }

    private DependencyInfo toDependencyInfo(Dependency d) {
        return DependencyInfo.builder()
                .id(d.getId())
                .groupId(d.getGroupId())
                .artifactId(d.getArtifactId())
                .version(d.getVersion())
                .scope(d.getScope())
                .build();
    }
}
