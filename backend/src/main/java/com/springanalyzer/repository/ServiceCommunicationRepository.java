package com.springanalyzer.repository;

import com.springanalyzer.entity.ServiceCommunication;
import com.springanalyzer.entity.CommunicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceCommunicationRepository extends JpaRepository<ServiceCommunication, Long> {
    
    List<ServiceCommunication> findByProjectId(Long projectId);
    
    List<ServiceCommunication> findByProjectIdAndSourceService(Long projectId, String sourceService);
    
    List<ServiceCommunication> findByProjectIdAndCommunicationType(Long projectId, CommunicationType type);
    
    long countByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
