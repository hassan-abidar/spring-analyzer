package com.springanalyzer.repository;

import com.springanalyzer.entity.AnalyzedClass;
import com.springanalyzer.entity.ClassType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalyzedClassRepository extends JpaRepository<AnalyzedClass, Long> {
    
    List<AnalyzedClass> findByProjectId(Long projectId);
    
    List<AnalyzedClass> findByProjectIdAndType(Long projectId, ClassType type);
    
    long countByProjectId(Long projectId);
    
    long countByProjectIdAndType(Long projectId, ClassType type);
    
    void deleteByProjectId(Long projectId);
}
