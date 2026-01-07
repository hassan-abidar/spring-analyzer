package com.springanalyzer.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MicroservicesResponse {
    private Long projectId;
    private String projectName;
    private boolean isMultiModule;
    private MicroservicesSummary summary;
    private List<MicroserviceInfo> services;
    private List<CommunicationInfo> communications;

    @Data
    @Builder
    public static class MicroservicesSummary {
        private int totalServices;
        private int totalCommunications;
        private Map<String, Integer> serviceTypeBreakdown;
        private Map<String, Integer> communicationTypeBreakdown;
        private boolean hasServiceDiscovery;
        private boolean hasApiGateway;
        private boolean hasConfigServer;
        private boolean hasLoadBalancing;
        private boolean hasCircuitBreaker;
        private List<String> messagingTechnologies;
        private List<String> communicationMethods;
        private String eurekaServerUrl;
    }

    @Data
    @Builder
    public static class MicroserviceInfo {
        private Long id;
        private String name;
        private String applicationName;
        private String basePackage;
        private String modulePath;
        private String serverPort;
        private String serviceType;
        private List<String> profiles;
        // Service Discovery
        private boolean hasEurekaClient;
        private boolean hasConfigClient;
        private boolean hasGateway;
        private String eurekaServiceUrl;
        // Communication Methods
        private boolean hasFeignClients;
        private boolean hasRestTemplate;
        private boolean hasWebClient;
        private boolean hasKafka;
        private boolean hasRabbitmq;
        private boolean hasGrpc;
        // Resilience
        private boolean hasLoadBalancer;
        private boolean hasCircuitBreaker;
        // Stats
        private int classCount;
        private int endpointCount;
        private List<String> dependencies;
        private List<String> messagingTypes;
        private List<String> communicationMethods;
        private List<String> consumedServices;
        // Gateway specific
        private List<String> gatewayRoutes;
        // Database
        private String databaseType;
    }

    @Data
    @Builder
    public static class CommunicationInfo {
        private Long id;
        private String sourceService;
        private String targetService;
        private String targetUrl;
        private String communicationType;
        private String httpMethod;
        private String feignClientName;
        private String className;
        private String methodName;
        private String messageChannel;
        private String endpointPath;
        private boolean isLoadBalanced;
        private boolean isAsync;
        private String description;
    }
}
