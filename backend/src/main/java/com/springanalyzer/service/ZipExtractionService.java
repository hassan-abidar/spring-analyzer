package com.springanalyzer.service;

import com.springanalyzer.exception.SpringAnalyzerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ZipExtractionService {

    private final Path extractDir;

    public ZipExtractionService(@Value("${app.extract.dir:extracts}") String extractDir) {
        this.extractDir = Paths.get(extractDir).toAbsolutePath().normalize();
        createDirectory(this.extractDir);
    }

    public Path extract(Path zipFile, String projectName) {
        Path targetDir = extractDir.resolve(projectName + "_" + System.currentTimeMillis());
        createDirectory(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                
                if (!entryPath.startsWith(targetDir)) {
                    throw new SpringAnalyzerException("Invalid zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            cleanup(targetDir);
            throw new SpringAnalyzerException("Failed to extract ZIP: " + e.getMessage());
        }

        return findProjectRoot(targetDir);
    }

    public List<Path> findJavaFiles(Path projectRoot) {
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning for Java files", e);
        }
        return javaFiles;
    }

    public Path findPomFile(Path projectRoot) {
        try {
            return Files.walk(projectRoot, 3)
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Find all pom.xml files in the project (for multi-module projects)
     */
    public List<Path> findAllPomFiles(Path projectRoot) {
        List<Path> pomFiles = new ArrayList<>();
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().equals("pom.xml")) {
                        pomFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning for pom.xml files", e);
        }
        return pomFiles;
    }

    /**
     * Find all modules (directories containing pom.xml or build.gradle)
     */
    public List<ModuleInfo> findModules(Path projectRoot) {
        List<ModuleInfo> modules = new ArrayList<>();
        
        try {
            // Check if root has pom.xml (could be parent pom or single module)
            Path rootPom = projectRoot.resolve("pom.xml");
            boolean isMultiModule = false;
            
            if (Files.exists(rootPom)) {
                String content = Files.readString(rootPom);
                isMultiModule = content.contains("<modules>") || content.contains("<module>");
            }

            // Find all pom.xml files
            List<Path> pomFiles = findAllPomFiles(projectRoot);
            
            for (Path pomFile : pomFiles) {
                Path moduleDir = pomFile.getParent();
                String moduleName = moduleDir.equals(projectRoot) 
                        ? "root" 
                        : projectRoot.relativize(moduleDir).toString().replace("\\", "/");
                
                // Skip parent pom in multi-module projects
                if (isMultiModule && moduleDir.equals(projectRoot)) {
                    continue;
                }
                
                // Check if this module has Java source files
                Path srcMain = moduleDir.resolve("src/main/java");
                if (Files.exists(srcMain) || !isMultiModule) {
                    modules.add(new ModuleInfo(moduleName, moduleDir, pomFile));
                }
            }
            
            // If no modules found, treat as single module
            if (modules.isEmpty()) {
                modules.add(new ModuleInfo("main", projectRoot, rootPom.toFile().exists() ? rootPom : null));
            }
            
        } catch (IOException e) {
            log.error("Error finding modules", e);
            modules.add(new ModuleInfo("main", projectRoot, null));
        }
        
        return modules;
    }

    /**
     * Determine which module a file belongs to
     */
    public String getModuleForFile(Path file, List<ModuleInfo> modules, Path projectRoot) {
        String filePath = file.toAbsolutePath().toString().replace("\\", "/");
        
        // Find the most specific (longest path) module that contains this file
        ModuleInfo bestMatch = null;
        int bestMatchLength = -1;
        
        for (ModuleInfo module : modules) {
            String modulePath = module.getPath().toAbsolutePath().toString().replace("\\", "/");
            if (filePath.startsWith(modulePath) && modulePath.length() > bestMatchLength) {
                bestMatch = module;
                bestMatchLength = modulePath.length();
            }
        }
        
        return bestMatch != null ? bestMatch.getName() : "main";
    }

    /**
     * Module information holder
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ModuleInfo {
        private String name;
        private Path path;
        private Path pomFile;
    }

    public void cleanup(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to cleanup directory: {}", directory, e);
        }
    }

    private Path findProjectRoot(Path extractedDir) {
        try {
            Path pomInRoot = extractedDir.resolve("pom.xml");
            if (Files.exists(pomInRoot)) {
                return extractedDir;
            }

            try (var stream = Files.list(extractedDir)) {
                List<Path> entries = stream.toList();
                if (entries.size() == 1 && Files.isDirectory(entries.get(0))) {
                    return findProjectRoot(entries.get(0));
                }
            }
        } catch (IOException e) {
            log.error("Error finding project root", e);
        }
        return extractedDir;
    }

    private void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new SpringAnalyzerException("Could not create directory: " + path);
        }
    }
}
