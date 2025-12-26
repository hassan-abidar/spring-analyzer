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
    
    long countByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
