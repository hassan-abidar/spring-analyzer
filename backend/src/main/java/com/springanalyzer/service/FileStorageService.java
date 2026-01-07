package com.springanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.springanalyzer.exception.BadRequestException;
import com.springanalyzer.exception.SpringAnalyzerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        createDirectory(this.uploadDir);
    }

    public String store(MultipartFile file) {
        validateFile(file);
        
        String filename = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        Path targetPath = uploadDir.resolve(filename);
        
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new SpringAnalyzerException("Failed to store file: " + e.getMessage());
        }
    }

    public Path getFilePath(String filename) {
        return uploadDir.resolve(filename).normalize();
    }

    public void delete(String filename) {
        try {
            Path filePath = getFilePath(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new SpringAnalyzerException("Failed to delete file: " + e.getMessage());
        }
    }

    public boolean exists(String filename) {
        return Files.exists(getFilePath(filename));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new BadRequestException("Only ZIP files are allowed");
        }
        
        long maxSize = 500 * 1024 * 1024; // 500MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("File size exceeds 500MB limit");
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new SpringAnalyzerException("Could not create upload directory");
        }
    }
}
