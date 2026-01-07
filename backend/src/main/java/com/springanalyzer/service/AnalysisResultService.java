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
    private final ClassRelationshipRepository relationshipRepository;

    public AnalysisResponse getAnalysisResult(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<AnalyzedClass> classes = classRepository.findByProjectId(projectId);
        List<Endpoint> endpoints = endpointRepository.findByProjectId(projectId);
        List<Dependency> dependencies = dependencyRepository.findByProjectId(projectId);
        List<ClassRelationship> relationships = relationshipRepository.findByProject_Id(projectId);

        // Get distinct module names
        List<String> modules = classRepository.findDistinctModuleNamesByProjectId(projectId);
        if (modules.isEmpty() || (modules.size() == 1 && modules.get(0) == null)) {
            modules = List.of("main");
        }
        boolean isMultiModule = modules.size() > 1;

        // Build module summaries
        Map<String, ModuleSummary> moduleSummaries = new HashMap<>();
        for (String moduleName : modules) {
            if (moduleName != null) {
                moduleSummaries.put(moduleName, buildModuleSummary(moduleName, classes, endpoints, dependencies));
            }
        }

        return AnalysisResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .status(project.getStatus().name())
                .isMultiModule(isMultiModule)
                .modules(modules)
                .summary(buildSummary(classes, endpoints, dependencies, relationships, modules.size()))
                .moduleSummaries(moduleSummaries)
                .classes(classes.stream().map(this::toClassInfo).toList())
                .endpoints(endpoints.stream().map(this::toEndpointInfo).toList())
                .dependencies(dependencies.stream().map(this::toDependencyInfo).toList())
                .relationships(relationships.stream().map(this::toRelationshipInfo).toList())
                .build();
    }

    private ModuleSummary buildModuleSummary(String moduleName, List<AnalyzedClass> allClasses, 
            List<Endpoint> allEndpoints, List<Dependency> allDependencies) {
        List<AnalyzedClass> moduleClasses = allClasses.stream()
                .filter(c -> moduleName.equals(c.getModuleName()))
                .toList();
        List<Endpoint> moduleEndpoints = allEndpoints.stream()
                .filter(e -> moduleName.equals(e.getModuleName()))
                .toList();
        List<Dependency> moduleDeps = allDependencies.stream()
                .filter(d -> moduleName.equals(d.getModuleName()))
                .toList();

        return ModuleSummary.builder()
                .moduleName(moduleName)
                .totalClasses(moduleClasses.size())
                .controllers(countByType(moduleClasses, ClassType.CONTROLLER) + countByType(moduleClasses, ClassType.REST_CONTROLLER))
                .services(countByType(moduleClasses, ClassType.SERVICE))
                .repositories(countByType(moduleClasses, ClassType.REPOSITORY))
                .entities(countByType(moduleClasses, ClassType.ENTITY))
                .endpoints(moduleEndpoints.size())
                .dependencies(moduleDeps.size())
                .build();
    }

    private AnalysisSummary buildSummary(List<AnalyzedClass> classes, List<Endpoint> endpoints, 
            List<Dependency> dependencies, List<ClassRelationship> relationships, int moduleCount) {
        Map<String, Long> classTypeBreakdown = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getType().name(), Collectors.counting()));

        Map<String, Long> httpMethodBreakdown = endpoints.stream()
                .collect(Collectors.groupingBy(e -> e.getHttpMethod().name(), Collectors.counting()));

        Map<String, Long> relationshipTypeBreakdown = relationships.stream()
                .collect(Collectors.groupingBy(r -> r.getType().name(), Collectors.counting()));

        return AnalysisSummary.builder()
                .totalClasses(classes.size())
                .controllers(countByType(classes, ClassType.CONTROLLER) + countByType(classes, ClassType.REST_CONTROLLER))
                .services(countByType(classes, ClassType.SERVICE))
                .repositories(countByType(classes, ClassType.REPOSITORY))
                .entities(countByType(classes, ClassType.ENTITY))
                .endpoints(endpoints.size())
                .dependencies(dependencies.size())
                .relationships(relationships.size())
                .moduleCount(moduleCount)
                .classTypeBreakdown(classTypeBreakdown)
                .httpMethodBreakdown(httpMethodBreakdown)
                .relationshipTypeBreakdown(relationshipTypeBreakdown)
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
                .moduleName(c.getModuleName())
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
                .moduleName(e.getModuleName())
                .build();
    }

    private DependencyInfo toDependencyInfo(Dependency d) {
        return DependencyInfo.builder()
                .id(d.getId())
                .groupId(d.getGroupId())
                .artifactId(d.getArtifactId())
                .version(d.getVersion())
                .scope(d.getScope())
                .moduleName(d.getModuleName())
                .build();
    }

    private RelationshipInfo toRelationshipInfo(ClassRelationship r) {
        return RelationshipInfo.builder()
                .id(r.getId())
                .sourceClass(r.getSourceClass().getName())
                .targetClass(r.getTargetClass().getName())
                .type(r.getType().name())
                .fieldName(r.getFieldName())
                .build();
    }
}
