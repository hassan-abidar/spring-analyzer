package com.springanalyzer.service;

import com.springanalyzer.entity.Dependency;
import com.springanalyzer.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PomParserService {

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*" +
            "<groupId>([^<]+)</groupId>\\s*" +
            "<artifactId>([^<]+)</artifactId>\\s*" +
            "(?:<version>([^<]+)</version>\\s*)?" +
            "(?:<scope>([^<]+)</scope>\\s*)?" +
            "</dependency>",
            Pattern.DOTALL
    );

    public List<Dependency> parsePom(Path pomFile, Project project) {
        List<Dependency> dependencies = new ArrayList<>();
        
        try {
            String content = Files.readString(pomFile);
            Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
            
            while (matcher.find()) {
                Dependency dep = Dependency.builder()
                        .project(project)
                        .groupId(matcher.group(1).trim())
                        .artifactId(matcher.group(2).trim())
                        .version(matcher.group(3) != null ? matcher.group(3).trim() : null)
                        .scope(matcher.group(4) != null ? matcher.group(4).trim() : "compile")
                        .build();
                dependencies.add(dep);
            }
        } catch (IOException e) {
            log.error("Failed to parse pom.xml: {}", pomFile, e);
        }
        
        return dependencies;
    }
}
