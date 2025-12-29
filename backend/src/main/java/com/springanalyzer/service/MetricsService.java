package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import com.springanalyzer.repository.CodeMetricsRepository;
import com.springanalyzer.repository.AnalyzedClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final CodeMetricsRepository metricsRepository;
    private final AnalyzedClassRepository classRepository;

    public CodeMetrics calculateMetrics(Project project, Path extractedPath) {
        CodeMetrics metrics = CodeMetrics.builder()
            .project(project)
            .build();

        try {
            List<Path> javaFiles = Files.walk(extractedPath)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

            metrics.setTotalFiles(javaFiles.size());

            int totalLines = 0, codeLines = 0, commentLines = 0, blankLines = 0;
            Set<String> packages = new HashSet<>();
            int maxDepth = 0;

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                boolean inBlockComment = false;

                for (String line : lines) {
                    totalLines++;
                    String trimmed = line.trim();

                    if (trimmed.isEmpty()) {
                        blankLines++;
                    } else if (inBlockComment) {
                        commentLines++;
                        if (trimmed.contains("*/")) {
                            inBlockComment = false;
                        }
                    } else if (trimmed.startsWith("/*")) {
                        commentLines++;
                        if (!trimmed.contains("*/")) {
                            inBlockComment = true;
                        }
                    } else if (trimmed.startsWith("//")) {
                        commentLines++;
                    } else {
                        codeLines++;
                    }

                    if (trimmed.startsWith("package ")) {
                        String pkg = trimmed.substring(8).replace(";", "").trim();
                        packages.add(pkg);
                        int depth = pkg.split("\\.").length;
                        maxDepth = Math.max(maxDepth, depth);
                    }
                }
            }

            metrics.setTotalLines(totalLines);
            metrics.setCodeLines(codeLines);
            metrics.setCommentLines(commentLines);
            metrics.setBlankLines(blankLines);
            metrics.setTotalPackages(packages.size());
            metrics.setMaxPackageDepth(maxDepth);

            List<AnalyzedClass> classes = classRepository.findByProjectId(project.getId());
            if (!classes.isEmpty()) {
                double avgMethods = classes.stream().mapToInt(AnalyzedClass::getMethodCount).average().orElse(0);
                double avgFields = classes.stream().mapToInt(AnalyzedClass::getFieldCount).average().orElse(0);
                int maxMethods = classes.stream().mapToInt(AnalyzedClass::getMethodCount).max().orElse(0);
                int maxFields = classes.stream().mapToInt(AnalyzedClass::getFieldCount).max().orElse(0);

                metrics.setAvgMethodsPerClass(Math.round(avgMethods * 100.0) / 100.0);
                metrics.setAvgFieldsPerClass(Math.round(avgFields * 100.0) / 100.0);
                metrics.setMaxMethodsInClass(maxMethods);
                metrics.setMaxFieldsInClass(maxFields);
            }

            metrics.setPackageStructure(buildPackageTree(packages));

        } catch (IOException e) {
            log.error("Failed to calculate metrics", e);
        }

        return metricsRepository.save(metrics);
    }

    private String buildPackageTree(Set<String> packages) {
        Map<String, Set<String>> tree = new TreeMap<>();
        
        for (String pkg : packages) {
            String[] parts = pkg.split("\\.");
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String parent = current.toString();
                current.append(i > 0 ? "." : "").append(parts[i]);
                tree.computeIfAbsent(parent, k -> new TreeSet<>()).add(parts[i]);
            }
        }

        StringBuilder sb = new StringBuilder();
        buildTreeString(tree, "", sb, 0);
        return sb.toString();
    }

    private void buildTreeString(Map<String, Set<String>> tree, String current, StringBuilder sb, int depth) {
        Set<String> children = tree.get(current);
        if (children == null) return;

        for (String child : children) {
            sb.append("  ".repeat(depth)).append(child).append("\n");
            String next = current.isEmpty() ? child : current + "." + child;
            buildTreeString(tree, next, sb, depth + 1);
        }
    }
}
