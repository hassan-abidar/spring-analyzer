package com.springanalyzer.controller;

import com.springanalyzer.dto.MicroservicesResponse;
import com.springanalyzer.dto.response.ApiResponse;
import com.springanalyzer.entity.*;
import com.springanalyzer.repository.ProjectRepository;
import com.springanalyzer.service.MicroserviceAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/microservices")
@RequiredArgsConstructor
public class MicroservicesController {

    private final MicroserviceAnalyzerService microserviceAnalyzerService;
    private final ProjectRepository projectRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<MicroservicesResponse>> getMicroservices(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Microservice> microservices = microserviceAnalyzerService.getMicroservices(projectId);
        List<ServiceCommunication> communications = microserviceAnalyzerService.getCommunications(projectId);

        MicroservicesResponse response = buildResponse(project, microservices, communications);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private MicroservicesResponse buildResponse(
            Project project,
            List<Microservice> microservices,
            List<ServiceCommunication> communications) {

        // Build summary
        Map<String, Integer> serviceTypeBreakdown = microservices.stream()
                .filter(s -> s.getServiceType() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getServiceType().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> commTypeBreakdown = communications.stream()
                .filter(c -> c.getCommunicationType() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getCommunicationType().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        boolean hasServiceDiscovery = microservices.stream()
                .anyMatch(s -> s.getServiceType() == ServiceType.DISCOVERY_SERVER ||
                        Boolean.TRUE.equals(s.getHasEurekaClient()));

        boolean hasApiGateway = microservices.stream()
                .anyMatch(s -> s.getServiceType() == ServiceType.API_GATEWAY ||
                        Boolean.TRUE.equals(s.getHasGateway()));

        boolean hasConfigServer = microservices.stream()
                .anyMatch(s -> s.getServiceType() == ServiceType.CONFIG_SERVER ||
                        Boolean.TRUE.equals(s.getHasConfigClient()));

        Set<String> messagingTechs = new HashSet<>();
        for (Microservice ms : microservices) {
            if (ms.getMessagingType() != null && !ms.getMessagingType().isEmpty()) {
                messagingTechs.addAll(Arrays.asList(ms.getMessagingType().split(",")));
            }
        }

        MicroservicesResponse.MicroservicesSummary summary = MicroservicesResponse.MicroservicesSummary.builder()
                .totalServices(microservices.size())
                .totalCommunications(communications.size())
                .serviceTypeBreakdown(serviceTypeBreakdown)
                .communicationTypeBreakdown(commTypeBreakdown)
                .hasServiceDiscovery(hasServiceDiscovery)
                .hasApiGateway(hasApiGateway)
                .hasConfigServer(hasConfigServer)
                .messagingTechnologies(new ArrayList<>(messagingTechs))
                .build();

        // Build service list
        List<MicroservicesResponse.MicroserviceInfo> serviceInfos = microservices.stream()
                .map(this::toServiceInfo)
                .collect(Collectors.toList());

        // Build communication list
        List<MicroservicesResponse.CommunicationInfo> commInfos = communications.stream()
                .map(this::toCommunicationInfo)
                .collect(Collectors.toList());

        return MicroservicesResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .isMultiModule(microservices.size() > 1)
                .summary(summary)
                .services(serviceInfos)
                .communications(commInfos)
                .build();
    }

    private MicroservicesResponse.MicroserviceInfo toServiceInfo(Microservice ms) {
        return MicroservicesResponse.MicroserviceInfo.builder()
                .id(ms.getId())
                .name(ms.getName())
                .applicationName(ms.getApplicationName())
                .basePackage(ms.getBasePackage())
                .modulePath(ms.getModulePath())
                .serverPort(ms.getServerPort())
                .serviceType(ms.getServiceType() != null ? ms.getServiceType().name() : "UNKNOWN")
                .profiles(ms.getProfiles() != null && !ms.getProfiles().isEmpty() 
                        ? Arrays.asList(ms.getProfiles().split(",")) 
                        : Collections.emptyList())
                .hasEurekaClient(Boolean.TRUE.equals(ms.getHasEurekaClient()))
                .hasConfigClient(Boolean.TRUE.equals(ms.getHasConfigClient()))
                .hasGateway(Boolean.TRUE.equals(ms.getHasGateway()))
                .hasFeignClients(Boolean.TRUE.equals(ms.getHasFeignClients()))
                .classCount(ms.getClassCount() != null ? ms.getClassCount() : 0)
                .endpointCount(ms.getEndpointCount() != null ? ms.getEndpointCount() : 0)
                .dependencies(ms.getDependencies() != null && !ms.getDependencies().isEmpty()
                        ? Arrays.asList(ms.getDependencies().split(","))
                        : Collections.emptyList())
                .messagingTypes(ms.getMessagingType() != null && !ms.getMessagingType().isEmpty()
                        ? Arrays.asList(ms.getMessagingType().split(","))
                        : Collections.emptyList())
                .consumedServices(ms.getConsumedServices() != null && !ms.getConsumedServices().isEmpty()
                        ? Arrays.asList(ms.getConsumedServices().split(","))
                        : Collections.emptyList())
                .build();
    }

    private MicroservicesResponse.CommunicationInfo toCommunicationInfo(ServiceCommunication comm) {
        return MicroservicesResponse.CommunicationInfo.builder()
                .id(comm.getId())
                .sourceService(comm.getSourceService())
                .targetService(comm.getTargetService())
                .targetUrl(comm.getTargetUrl())
                .communicationType(comm.getCommunicationType() != null 
                        ? comm.getCommunicationType().name() 
                        : "UNKNOWN")
                .httpMethod(comm.getHttpMethod())
                .feignClientName(comm.getFeignClientName())
                .className(comm.getClassName())
                .methodName(comm.getMethodName())
                .messageChannel(comm.getMessageChannel())
                .build();
    }
}
