package com.springanalyzer.repository;

import com.springanalyzer.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long> {
    
    List<Dependency> findByProjectId(Long projectId);
    
    long countByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
