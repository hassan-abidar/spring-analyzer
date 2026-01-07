package com.springanalyzer.repository;

import com.springanalyzer.entity.Endpoint;
import com.springanalyzer.entity.HttpMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    
    List<Endpoint> findByProjectId(Long projectId);
    
    List<Endpoint> findByProjectIdAndHttpMethod(Long projectId, HttpMethod httpMethod);
    
    List<Endpoint> findByProjectIdAndModuleName(Long projectId, String moduleName);
    
    long countByProjectId(Long projectId);
    
    long countByProjectIdAndModuleName(Long projectId, String moduleName);
    
    void deleteByProjectId(Long projectId);
}
