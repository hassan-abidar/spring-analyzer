package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "microservices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Microservice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_package")
    private String basePackage;

    @Column(name = "module_path")
    private String modulePath;

    @Column(name = "application_name")
    private String applicationName;

    @Column(name = "server_port")
    private String serverPort;

    @Column(columnDefinition = "TEXT")
    private String profiles;

    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @Column(name = "has_eureka_client")
    private Boolean hasEurekaClient;

    @Column(name = "has_config_client")
    private Boolean hasConfigClient;

    @Column(name = "has_gateway")
    private Boolean hasGateway;

    @Column(name = "has_feign_clients")
    private Boolean hasFeignClients;

    @Column(name = "class_count")
    private Integer classCount;

    @Column(name = "endpoint_count")
    private Integer endpointCount;

    @Column(columnDefinition = "TEXT")
    private String dependencies;

    @Column(columnDefinition = "TEXT")
    private String consumedServices;

    @Column(columnDefinition = "TEXT")
    private String messagingType;
}
