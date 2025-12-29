package com.springanalyzer.repository;

import com.springanalyzer.entity.CodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CodeMetricsRepository extends JpaRepository<CodeMetrics, Long> {
    
    Optional<CodeMetrics> findByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
