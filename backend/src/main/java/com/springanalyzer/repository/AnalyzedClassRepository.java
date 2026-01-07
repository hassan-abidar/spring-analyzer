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
    
    List<AnalyzedClass> findByProjectIdAndModuleName(Long projectId, String moduleName);
    
    long countByProjectId(Long projectId);
    
    long countByProjectIdAndType(Long projectId, ClassType type);
    
    long countByProjectIdAndModuleName(Long projectId, String moduleName);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.moduleName FROM AnalyzedClass c WHERE c.project.id = :projectId")
    List<String> findDistinctModuleNamesByProjectId(@org.springframework.data.repository.query.Param("projectId") Long projectId);
    
    void deleteByProjectId(Long projectId);
}
