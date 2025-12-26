package com.springanalyzer.repository;

import com.springanalyzer.entity.Project;
import com.springanalyzer.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);
    
    List<Project> findAllByOrderByCreatedAtDesc();
    
    boolean existsByName(String name);
}
