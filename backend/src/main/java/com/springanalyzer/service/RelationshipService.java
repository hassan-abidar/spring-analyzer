package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import com.springanalyzer.repository.AnalyzedClassRepository;
import com.springanalyzer.repository.ClassRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipService {

    private final AnalyzedClassRepository classRepository;
    private final ClassRelationshipRepository relationshipRepository;

    private static final Pattern INJECTION_PATTERN = Pattern.compile("@(Autowired|Inject)\\s*(?:private|protected|public)?\\s*(\\w+)\\s+(\\w+)");
    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(?:private|protected|public)\\s+(?:final\\s+)?(\\w+)(?:<[^>]+>)?\\s+(\\w+)\\s*[;=]");
    private static final Pattern JPA_RELATION_PATTERN = Pattern.compile("@(OneToOne|OneToMany|ManyToOne|ManyToMany)");

    public void analyzeRelationships(Project project, List<Path> javaFiles, Map<String, AnalyzedClass> classMap) {
        for (Path file : javaFiles) {
            try {
                String content = Files.readString(file);
                String className = extractClassName(content);
                if (className == null) continue;

                AnalyzedClass sourceClass = classMap.get(className);
                if (sourceClass == null) continue;

                analyzeInheritance(content, sourceClass, classMap, project);
                analyzeInjections(content, sourceClass, classMap, project);
                analyzeJpaRelations(content, sourceClass, classMap, project);
                analyzeFieldUsages(content, sourceClass, classMap, project);
            } catch (IOException e) {
                log.error("Failed to analyze relationships in: {}", file, e);
            }
        }
    }

    private void analyzeInheritance(String content, AnalyzedClass source, Map<String, AnalyzedClass> classMap, Project project) {
        Pattern extendsPattern = Pattern.compile("class\\s+\\w+\\s+extends\\s+(\\w+)");
        Matcher matcher = extendsPattern.matcher(content);
        if (matcher.find()) {
            String parentClass = matcher.group(1);
            AnalyzedClass target = classMap.get(parentClass);
            if (target != null) {
                saveRelationship(project, source, target, RelationshipType.EXTENDS, null);
            }
        }

        Pattern implementsPattern = Pattern.compile("class\\s+\\w+(?:\\s+extends\\s+\\w+)?\\s+implements\\s+([\\w,\\s]+)");
        matcher = implementsPattern.matcher(content);
        if (matcher.find()) {
            String[] interfaces = matcher.group(1).split(",");
            for (String iface : interfaces) {
                AnalyzedClass target = classMap.get(iface.trim());
                if (target != null) {
                    saveRelationship(project, source, target, RelationshipType.IMPLEMENTS, null);
                }
            }
        }
    }

    private void analyzeInjections(String content, AnalyzedClass source, Map<String, AnalyzedClass> classMap, Project project) {
        Matcher matcher = INJECTION_PATTERN.matcher(content);
        Set<String> processed = new HashSet<>();
        
        while (matcher.find()) {
            String typeName = matcher.group(2);
            String fieldName = matcher.group(3);
            
            if (!processed.contains(typeName)) {
                AnalyzedClass target = classMap.get(typeName);
                if (target != null) {
                    saveRelationship(project, source, target, RelationshipType.INJECTS, fieldName);
                    processed.add(typeName);
                }
            }
        }

        Pattern constructorInjection = Pattern.compile("(?:private|protected|public)\\s+final\\s+(\\w+)\\s+(\\w+)\\s*;");
        matcher = constructorInjection.matcher(content);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            String fieldName = matcher.group(2);
            if (!processed.contains(typeName) && classMap.containsKey(typeName)) {
                saveRelationship(project, source, classMap.get(typeName), RelationshipType.INJECTS, fieldName);
                processed.add(typeName);
            }
        }
    }

    private void analyzeJpaRelations(String content, AnalyzedClass source, Map<String, AnalyzedClass> classMap, Project project) {
        if (source.getType() != ClassType.ENTITY) return;

        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            Matcher relationMatcher = JPA_RELATION_PATTERN.matcher(lines[i]);
            if (relationMatcher.find()) {
                RelationshipType relType = switch (relationMatcher.group(1)) {
                    case "OneToOne" -> RelationshipType.ONE_TO_ONE;
                    case "OneToMany" -> RelationshipType.ONE_TO_MANY;
                    case "ManyToOne" -> RelationshipType.MANY_TO_ONE;
                    case "ManyToMany" -> RelationshipType.MANY_TO_MANY;
                    default -> null;
                };

                if (relType != null) {
                    for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                        Matcher fieldMatcher = FIELD_TYPE_PATTERN.matcher(lines[j]);
                        if (fieldMatcher.find()) {
                            String typeName = fieldMatcher.group(1);
                            String fieldName = fieldMatcher.group(2);
                            
                            if (typeName.equals("List") || typeName.equals("Set")) {
                                Pattern genericPattern = Pattern.compile("<(\\w+)>");
                                Matcher genericMatcher = genericPattern.matcher(lines[j]);
                                if (genericMatcher.find()) {
                                    typeName = genericMatcher.group(1);
                                }
                            }
                            
                            AnalyzedClass target = classMap.get(typeName);
                            if (target != null) {
                                saveRelationship(project, source, target, relType, fieldName);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void analyzeFieldUsages(String content, AnalyzedClass source, Map<String, AnalyzedClass> classMap, Project project) {
        Set<String> processed = new HashSet<>();
        Matcher matcher = FIELD_TYPE_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String typeName = matcher.group(1);
            if (!processed.contains(typeName) && classMap.containsKey(typeName)) {
                boolean alreadyExists = relationshipRepository.findBySourceClass_Id(source.getId()).stream()
                        .anyMatch(r -> r.getTargetClass().getName().equals(typeName));
                
                if (!alreadyExists) {
                    saveRelationship(project, source, classMap.get(typeName), RelationshipType.USES, matcher.group(2));
                }
                processed.add(typeName);
            }
        }
    }

    private void saveRelationship(Project project, AnalyzedClass source, AnalyzedClass target, RelationshipType type, String fieldName) {
        if (source.getId().equals(target.getId())) return;
        
        ClassRelationship relationship = ClassRelationship.builder()
                .project(project)
                .sourceClass(source)
                .targetClass(target)
                .type(type)
                .fieldName(fieldName)
                .build();
        relationshipRepository.save(relationship);
    }

    private String extractClassName(String content) {
        Pattern pattern = Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
