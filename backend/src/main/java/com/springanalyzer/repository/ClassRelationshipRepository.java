package com.springanalyzer.repository;

import com.springanalyzer.entity.ClassRelationship;
import com.springanalyzer.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassRelationshipRepository extends JpaRepository<ClassRelationship, Long> {
    
    List<ClassRelationship> findByProject_Id(Long projectId);
    
    List<ClassRelationship> findBySourceClass_Id(Long sourceClassId);
    
    List<ClassRelationship> findByTargetClass_Id(Long targetClassId);
    
    List<ClassRelationship> findByProject_IdAndType(Long projectId, RelationshipType type);
    
    @Query("SELECT cr FROM ClassRelationship cr WHERE cr.project.id = :projectId AND (cr.sourceClass.id = :classId OR cr.targetClass.id = :classId)")
    List<ClassRelationship> findByProjectIdAndClassId(Long projectId, Long classId);
    
    void deleteByProjectId(Long projectId);
}
