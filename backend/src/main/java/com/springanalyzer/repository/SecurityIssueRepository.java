package com.springanalyzer.repository;

import com.springanalyzer.entity.SecurityIssue;
import com.springanalyzer.entity.IssueSeverity;
import com.springanalyzer.entity.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SecurityIssueRepository extends JpaRepository<SecurityIssue, Long> {
    
    List<SecurityIssue> findByProjectId(Long projectId);
    
    List<SecurityIssue> findByProjectIdAndSeverity(Long projectId, IssueSeverity severity);
    
    List<SecurityIssue> findByProjectIdAndCategory(Long projectId, IssueCategory category);
    
    long countByProjectId(Long projectId);
    
    long countByProjectIdAndSeverity(Long projectId, IssueSeverity severity);
    
    void deleteByProjectId(Long projectId);
}
