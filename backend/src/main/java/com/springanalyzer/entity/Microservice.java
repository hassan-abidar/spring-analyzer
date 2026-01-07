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

    // Communication methods used by this service
    @Column(name = "has_rest_template")
    private Boolean hasRestTemplate;

    @Column(name = "has_web_client")
    private Boolean hasWebClient;

    @Column(name = "has_kafka")
    private Boolean hasKafka;

    @Column(name = "has_rabbitmq")
    private Boolean hasRabbitmq;

    @Column(name = "has_grpc")
    private Boolean hasGrpc;

    // Service discovery details
    @Column(name = "eureka_service_url")
    private String eurekaServiceUrl;

    @Column(name = "has_load_balancer")
    private Boolean hasLoadBalancer;

    @Column(name = "has_circuit_breaker")
    private Boolean hasCircuitBreaker;

    // Gateway routes (JSON format)
    @Column(name = "gateway_routes", columnDefinition = "TEXT")
    private String gatewayRoutes;

    // Database info
    @Column(name = "database_type")
    private String databaseType;

    // All detected communication methods as comma-separated
    @Column(name = "communication_methods", columnDefinition = "TEXT")
    private String communicationMethods;
}
