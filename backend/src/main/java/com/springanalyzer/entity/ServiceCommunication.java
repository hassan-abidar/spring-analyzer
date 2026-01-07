package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "service_communications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "source_service", nullable = false)
    private String sourceService;

    @Column(name = "target_service")
    private String targetService;

    @Column(name = "target_url")
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type", nullable = false)
    private CommunicationType communicationType;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "feign_client_name")
    private String feignClientName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "message_channel")
    private String messageChannel;

    // Additional details for better analysis
    @Column(name = "endpoint_path")
    private String endpointPath;

    @Column(name = "is_load_balanced")
    private Boolean isLoadBalanced;

    @Column(name = "is_async")
    private Boolean isAsync;

    @Column(columnDefinition = "TEXT")
    private String description;
}
