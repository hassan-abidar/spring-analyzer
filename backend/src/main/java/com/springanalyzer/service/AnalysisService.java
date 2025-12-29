package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import com.springanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final ProjectRepository projectRepository;
    private final AnalyzedClassRepository classRepository;
    private final EndpointRepository endpointRepository;
    private final DependencyRepository dependencyRepository;
    private final ClassRelationshipRepository relationshipRepository;
    private final SecurityIssueRepository securityIssueRepository;
    private final CodeMetricsRepository codeMetricsRepository;
    private final FileStorageService fileStorageService;
    private final ZipExtractionService zipExtractionService;
    private final JavaParserService javaParserService;
    private final PomParserService pomParserService;
    private final RelationshipService relationshipService;
    private final SecurityScannerService securityScannerService;
    private final MetricsService metricsService;

    @Async
    public void analyzeProjectAsync(Long projectId) {
        try {
            analyzeProject(projectId);
        } catch (Exception e) {
            log.error("Async analysis failed for project {}", projectId, e);
            updateProjectStatus(projectId, ProjectStatus.FAILED);
        }
    }

    @Transactional
    public void analyzeProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        updateProjectStatus(projectId, ProjectStatus.ANALYZING);
        Path extractedPath = null;

        try {
            Path zipPath = fileStorageService.getFilePath(project.getStoragePath());
            extractedPath = zipExtractionService.extract(zipPath, project.getName());

            clearPreviousAnalysis(projectId);

            List<Path> javaFiles = zipExtractionService.findJavaFiles(extractedPath);
            log.info("Found {} Java files in project {}", javaFiles.size(), project.getName());

            Map<String, AnalyzedClass> classMap = new HashMap<>();
            for (Path javaFile : javaFiles) {
                AnalyzedClass saved = processJavaFile(javaFile, project);
                if (saved != null) {
                    classMap.put(saved.getName(), saved);
                }
            }

            relationshipService.analyzeRelationships(project, javaFiles, classMap);
            log.info("Analyzed relationships for {} classes", classMap.size());

            List<SecurityIssue> issues = securityScannerService.scanProject(project, extractedPath, classMap);
            log.info("Found {} security issues", issues.size());

            metricsService.calculateMetrics(project, extractedPath);
            log.info("Calculated code metrics");

            Path pomFile = zipExtractionService.findPomFile(extractedPath);
            if (pomFile != null) {
                List<Dependency> dependencies = pomParserService.parsePom(pomFile, project);
                dependencyRepository.saveAll(dependencies);
                log.info("Found {} dependencies", dependencies.size());
            }

            project.setStatus(ProjectStatus.COMPLETED);
            project.setAnalyzedAt(LocalDateTime.now());
            projectRepository.save(project);

            log.info("Analysis completed for project: {}", project.getName());

        } catch (Exception e) {
            log.error("Analysis failed for project {}", project.getName(), e);
            updateProjectStatus(projectId, ProjectStatus.FAILED);
            throw e;
        } finally {
            if (extractedPath != null) {
                zipExtractionService.cleanup(extractedPath);
            }
        }
    }

    private AnalyzedClass processJavaFile(Path file, Project project) {
        JavaParserService.ParsedClass parsed = javaParserService.parseJavaFile(file);
        if (parsed == null || parsed.getName() == null) return null;

        AnalyzedClass analyzedClass = AnalyzedClass.builder()
                .project(project)
                .name(parsed.getName())
                .packageName(parsed.getPackageName())
                .fullPath(parsed.getFullPath())
                .type(parsed.getClassType())
                .annotations(String.join(",", parsed.getAnnotations()))
                .extendsClass(parsed.getExtendsClass())
                .implementsInterfaces(String.join(",", parsed.getImplementsInterfaces()))
                .fieldCount(parsed.getFieldCount())
                .methodCount(parsed.getMethodCount())
                .build();

        AnalyzedClass saved = classRepository.save(analyzedClass);

        for (JavaParserService.ParsedEndpoint pe : parsed.getEndpoints()) {
            Endpoint endpoint = Endpoint.builder()
                    .project(project)
                    .analyzedClass(saved)
                    .httpMethod(pe.getHttpMethod())
                    .path(pe.getPath())
                    .methodName(pe.getMethodName())
                    .returnType(pe.getReturnType())
                    .parameters(pe.getParameters())
                    .build();
            endpointRepository.save(endpoint);
        }
        return saved;
    }

    @Transactional
    public void clearPreviousAnalysis(Long projectId) {
        securityIssueRepository.deleteByProjectId(projectId);
        codeMetricsRepository.deleteByProjectId(projectId);
        relationshipRepository.deleteByProjectId(projectId);
        endpointRepository.deleteByProjectId(projectId);
        classRepository.deleteByProjectId(projectId);
        dependencyRepository.deleteByProjectId(projectId);
    }

    private void updateProjectStatus(Long projectId, ProjectStatus status) {
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus(status);
            projectRepository.save(p);
        });
    }
}
