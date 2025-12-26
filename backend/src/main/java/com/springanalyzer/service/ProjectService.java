package com.springanalyzer.service;

import com.springanalyzer.dto.ProjectRequest;
import com.springanalyzer.dto.ProjectResponse;
import com.springanalyzer.entity.Project;
import com.springanalyzer.entity.ProjectStatus;
import com.springanalyzer.exception.BadRequestException;
import com.springanalyzer.exception.ResourceNotFoundException;
import com.springanalyzer.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProjectResponse createProject(MultipartFile file, ProjectRequest request) {
        if (projectRepository.existsByName(request.getName())) {
            throw new BadRequestException("Project with name '" + request.getName() + "' already exists");
        }

        String storagePath = fileStorageService.store(file);

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .originalFilename(file.getOriginalFilename())
                .storagePath(storagePath)
                .fileSize(file.getSize())
                .status(ProjectStatus.UPLOADED)
                .build();

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProject(Long id) {
        Project project = findById(id);
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findById(id);
        
        if (project.getStoragePath() != null) {
            fileStorageService.delete(project.getStoragePath());
        }
        
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse updateStatus(Long id, ProjectStatus status) {
        Project project = findById(id);
        project.setStatus(status);
        return toResponse(projectRepository.save(project));
    }

    private Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .originalFilename(project.getOriginalFilename())
                .fileSize(project.getFileSize())
                .status(project.getStatus().name())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .analyzedAt(project.getAnalyzedAt())
                .build();
    }
}
