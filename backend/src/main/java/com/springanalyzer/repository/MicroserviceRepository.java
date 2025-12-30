package com.springanalyzer.repository;

import com.springanalyzer.entity.Microservice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {
    
    List<Microservice> findByProjectId(Long projectId);
    
    long countByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
