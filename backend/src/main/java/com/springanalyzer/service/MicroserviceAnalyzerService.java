package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import com.springanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MicroserviceAnalyzerService {

    private final MicroserviceRepository microserviceRepository;
    private final ServiceCommunicationRepository communicationRepository;
    private final AnalyzedClassRepository classRepository;
    private final EndpointRepository endpointRepository;

    // Patterns for detecting microservice components
    private static final Pattern FEIGN_CLIENT_PATTERN = Pattern.compile(
            "@FeignClient\\s*\\(\\s*(?:name\\s*=\\s*|value\\s*=\\s*)?[\"']([^\"']+)[\"']"
    );
    private static final Pattern FEIGN_CLIENT_URL_PATTERN = Pattern.compile(
            "@FeignClient\\s*\\([^)]*url\\s*=\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern REST_TEMPLATE_PATTERN = Pattern.compile(
            "restTemplate\\s*\\.\\s*(getForObject|postForObject|exchange|getForEntity|postForEntity|put|delete)\\s*\\("
    );
    private static final Pattern REST_TEMPLATE_URL_PATTERN = Pattern.compile(
            "restTemplate\\s*\\.\\s*\\w+\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern WEB_CLIENT_PATTERN = Pattern.compile(
            "WebClient|webClient"
    );
    private static final Pattern WEB_CLIENT_URL_PATTERN = Pattern.compile(
            "\\.uri\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern KAFKA_LISTENER_PATTERN = Pattern.compile(
            "@KafkaListener\\s*\\([^)]*topics?\\s*=\\s*[{]?\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern KAFKA_TEMPLATE_PATTERN = Pattern.compile(
            "kafkaTemplate\\s*\\.\\s*send\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern RABBIT_LISTENER_PATTERN = Pattern.compile(
            "@RabbitListener\\s*\\([^)]*queues?\\s*=\\s*[{]?\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern RABBIT_TEMPLATE_PATTERN = Pattern.compile(
            "rabbitTemplate\\s*\\.\\s*(convertAndSend|send)\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile(
            "spring\\.application\\.name\\s*=\\s*(.+)"
    );
    private static final Pattern SERVER_PORT_PATTERN = Pattern.compile(
            "server\\.port\\s*=\\s*(\\d+)"
    );
    private static final Pattern YAML_APP_NAME_PATTERN = Pattern.compile(
            "name:\\s*([\\w-]+)"
    );
    private static final Pattern YAML_PORT_PATTERN = Pattern.compile(
            "port:\\s*(\\d+)"
    );
    // Gateway route patterns
    private static final Pattern GATEWAY_ROUTE_PATTERN = Pattern.compile(
            "spring\\.cloud\\.gateway\\.routes\\[\\d+\\]\\.uri\\s*=\\s*(.+)"
    );
    private static final Pattern GATEWAY_ROUTE_ID_PATTERN = Pattern.compile(
            "spring\\.cloud\\.gateway\\.routes\\[\\d+\\]\\.id\\s*=\\s*(.+)"
    );
    // Load balancer pattern
    private static final Pattern LOAD_BALANCED_PATTERN = Pattern.compile(
            "@LoadBalanced|lb://|LoadBalancerClient"
    );
    // Circuit breaker patterns
    private static final Pattern CIRCUIT_BREAKER_PATTERN = Pattern.compile(
            "@CircuitBreaker|Resilience4j|HystrixCommand|@HystrixCommand"
    );
    // Eureka patterns
    private static final Pattern EUREKA_URL_PATTERN = Pattern.compile(
            "eureka\\.client\\.service-url\\.defaultZone\\s*=\\s*(.+)"
    );
    // Database patterns
    private static final Pattern DATABASE_URL_PATTERN = Pattern.compile(
            "spring\\.datasource\\.url\\s*=\\s*jdbc:(\\w+):"
    );
    // gRPC patterns
    private static final Pattern GRPC_PATTERN = Pattern.compile(
            "@GrpcService|@GrpcClient|ManagedChannel|ServerBuilder"
    );

    @Transactional
    public List<Microservice> analyzeProject(Project project, Path extractedPath) {
        log.info("Starting microservice analysis for project: {}", project.getName());
        
        // Clear previous microservice data
        communicationRepository.deleteByProjectId(project.getId());
        microserviceRepository.deleteByProjectId(project.getId());

        List<Microservice> microservices = new ArrayList<>();
        
        try {
            // Find all modules (directories with pom.xml or build.gradle)
            List<Path> modules = findModules(extractedPath);
            
            if (modules.isEmpty()) {
                // Single module project
                Microservice service = analyzeModule(project, extractedPath, extractedPath);
                if (service != null) {
                    microservices.add(service);
                }
            } else {
                // Multi-module project
                for (Path module : modules) {
                    Microservice service = analyzeModule(project, module, extractedPath);
                    if (service != null) {
                        microservices.add(service);
                    }
                }
            }

            // Save all microservices
            microserviceRepository.saveAll(microservices);

            // Analyze inter-service communications
            List<ServiceCommunication> communications = analyzeServiceCommunications(project, extractedPath, microservices);
            
            // Also detect gateway routes
            for (Microservice ms : microservices) {
                if (Boolean.TRUE.equals(ms.getHasGateway()) || ms.getServiceType() == ServiceType.API_GATEWAY) {
                    Path modulePath = ms.getModulePath() != null && !ms.getModulePath().isEmpty()
                            ? extractedPath.resolve(ms.getModulePath())
                            : extractedPath;
                    communications.addAll(detectGatewayRoutes(project, modulePath, ms.getName()));
                }
            }
            
            communicationRepository.saveAll(communications);

            log.info("Found {} microservices and {} inter-service communications", 
                    microservices.size(), communications.size());

        } catch (Exception e) {
            log.error("Error analyzing microservices", e);
        }

        return microservices;
    }

    private List<Path> findModules(Path rootPath) throws IOException {
        List<Path> modules = new ArrayList<>();
        
        try (Stream<Path> walk = Files.walk(rootPath, 3)) {
            List<Path> pomFiles = walk
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(p -> !p.equals(rootPath.resolve("pom.xml")))
                    .collect(Collectors.toList());
            
            for (Path pom : pomFiles) {
                modules.add(pom.getParent());
            }
        }

        // Also check for gradle modules
        try (Stream<Path> walk = Files.walk(rootPath, 3)) {
            List<Path> gradleFiles = walk
                    .filter(p -> p.getFileName().toString().equals("build.gradle"))
                    .filter(p -> !p.equals(rootPath.resolve("build.gradle")))
                    .collect(Collectors.toList());
            
            for (Path gradle : gradleFiles) {
                if (!modules.contains(gradle.getParent())) {
                    modules.add(gradle.getParent());
                }
            }
        }

        return modules;
    }

    private Microservice analyzeModule(Project project, Path modulePath, Path rootPath) {
        try {
            String moduleName = modulePath.equals(rootPath) 
                    ? project.getName() 
                    : modulePath.getFileName().toString();

            // Find Spring Boot main class
            Path mainClass = findSpringBootMainClass(modulePath);
            if (mainClass == null && !modulePath.equals(rootPath)) {
                // Not a Spring Boot module
                return null;
            }

            Microservice.MicroserviceBuilder builder = Microservice.builder()
                    .project(project)
                    .name(moduleName)
                    .modulePath(rootPath.relativize(modulePath).toString());

            // Parse application properties/yml
            Map<String, String> config = parseApplicationConfig(modulePath);
            builder.applicationName(config.getOrDefault("applicationName", moduleName));
            builder.serverPort(config.getOrDefault("serverPort", "8080"));
            builder.profiles(config.getOrDefault("profiles", ""));
            builder.eurekaServiceUrl(config.get("eurekaServiceUrl"));
            builder.gatewayRoutes(config.get("gatewayRoutes"));
            builder.databaseType(config.get("databaseType"));

            // Detect service type from dependencies
            ServiceType serviceType = detectServiceType(modulePath);
            builder.serviceType(serviceType);

            // Detect Spring Cloud components
            Set<String> dependencies = parseDependencies(modulePath);
            builder.hasEurekaClient(dependencies.contains("spring-cloud-starter-netflix-eureka-client"));
            builder.hasConfigClient(dependencies.contains("spring-cloud-starter-config"));
            builder.hasGateway(dependencies.contains("spring-cloud-starter-gateway") 
                    || dependencies.contains("spring-cloud-starter-netflix-zuul"));
            builder.hasFeignClients(dependencies.contains("spring-cloud-starter-openfeign"));
            builder.hasLoadBalancer(dependencies.contains("spring-cloud-starter-loadbalancer")
                    || dependencies.stream().anyMatch(d -> d.contains("ribbon")));
            builder.dependencies(String.join(",", dependencies));

            // Detect communication methods by scanning source files
            CommunicationMethodsResult commMethods = detectCommunicationMethods(modulePath);
            builder.hasRestTemplate(commMethods.hasRestTemplate);
            builder.hasWebClient(commMethods.hasWebClient);
            builder.hasKafka(commMethods.hasKafka);
            builder.hasRabbitmq(commMethods.hasRabbitmq);
            builder.hasGrpc(commMethods.hasGrpc);
            builder.hasCircuitBreaker(commMethods.hasCircuitBreaker);
            builder.communicationMethods(String.join(",", commMethods.methods));

            // Detect messaging
            List<String> messaging = new ArrayList<>();
            if (dependencies.stream().anyMatch(d -> d.contains("kafka"))) messaging.add("KAFKA");
            if (dependencies.stream().anyMatch(d -> d.contains("rabbitmq") || d.contains("amqp"))) messaging.add("RABBITMQ");
            if (dependencies.stream().anyMatch(d -> d.contains("jms") || d.contains("activemq"))) messaging.add("JMS");
            builder.messagingType(String.join(",", messaging));

            // Get base package
            if (mainClass != null) {
                String content = Files.readString(mainClass);
                Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+)");
                Matcher matcher = packagePattern.matcher(content);
                if (matcher.find()) {
                    builder.basePackage(matcher.group(1));
                }
            }

            // Count classes and endpoints for this module
            int classCount = countJavaFiles(modulePath);
            builder.classCount(classCount);
            builder.endpointCount(countEndpoints(modulePath));

            return builder.build();

        } catch (Exception e) {
            log.error("Error analyzing module: {}", modulePath, e);
            return null;
        }
    }

    private Path findSpringBootMainClass(Path modulePath) throws IOException {
        try (Stream<Path> walk = Files.walk(modulePath)) {
            return walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(this::isSpringBootMainClass)
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean isSpringBootMainClass(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            return content.contains("@SpringBootApplication") 
                    || (content.contains("@EnableAutoConfiguration") && content.contains("main("));
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, String> parseApplicationConfig(Path modulePath) {
        Map<String, String> config = new HashMap<>();
        
        // Check application.properties
        Path propsFile = modulePath.resolve("src/main/resources/application.properties");
        if (Files.exists(propsFile)) {
            try {
                String content = Files.readString(propsFile);
                Matcher nameMatcher = APPLICATION_NAME_PATTERN.matcher(content);
                if (nameMatcher.find()) {
                    config.put("applicationName", nameMatcher.group(1).trim());
                }
                Matcher portMatcher = SERVER_PORT_PATTERN.matcher(content);
                if (portMatcher.find()) {
                    config.put("serverPort", portMatcher.group(1));
                }
                // Extract profiles
                Pattern profilePattern = Pattern.compile("spring\\.profiles\\.active\\s*=\\s*(.+)");
                Matcher profileMatcher = profilePattern.matcher(content);
                if (profileMatcher.find()) {
                    config.put("profiles", profileMatcher.group(1).trim());
                }
                // Extract Eureka URL
                Matcher eurekaMatcher = EUREKA_URL_PATTERN.matcher(content);
                if (eurekaMatcher.find()) {
                    config.put("eurekaServiceUrl", eurekaMatcher.group(1).trim());
                }
                // Extract database type
                Matcher dbMatcher = DATABASE_URL_PATTERN.matcher(content);
                if (dbMatcher.find()) {
                    config.put("databaseType", dbMatcher.group(1).toUpperCase());
                }
                // Extract gateway routes
                StringBuilder routes = new StringBuilder();
                Matcher routeMatcher = GATEWAY_ROUTE_PATTERN.matcher(content);
                while (routeMatcher.find()) {
                    if (routes.length() > 0) routes.append(",");
                    routes.append(routeMatcher.group(1).trim());
                }
                if (routes.length() > 0) {
                    config.put("gatewayRoutes", routes.toString());
                }
            } catch (IOException e) {
                log.debug("Could not read application.properties", e);
            }
        }

        // Check application.yml
        Path ymlFile = modulePath.resolve("src/main/resources/application.yml");
        if (Files.exists(ymlFile)) {
            try {
                String content = Files.readString(ymlFile);
                Matcher nameMatcher = YAML_APP_NAME_PATTERN.matcher(content);
                if (nameMatcher.find() && !config.containsKey("applicationName")) {
                    config.put("applicationName", nameMatcher.group(1));
                }
                Matcher portMatcher = YAML_PORT_PATTERN.matcher(content);
                if (portMatcher.find() && !config.containsKey("serverPort")) {
                    config.put("serverPort", portMatcher.group(1));
                }
                // Parse YAML for gateway routes
                if (content.contains("cloud:") && content.contains("gateway:")) {
                    config.put("hasGatewayConfig", "true");
                    // Extract routes from YAML - simplified parsing
                    Pattern yamlRoutePattern = Pattern.compile("uri:\\s*(lb://[\\w-]+|http[s]?://[^\\s]+)");
                    Matcher yamlRouteMatcher = yamlRoutePattern.matcher(content);
                    StringBuilder yamlRoutes = new StringBuilder();
                    while (yamlRouteMatcher.find()) {
                        if (yamlRoutes.length() > 0) yamlRoutes.append(",");
                        yamlRoutes.append(yamlRouteMatcher.group(1));
                    }
                    if (yamlRoutes.length() > 0 && !config.containsKey("gatewayRoutes")) {
                        config.put("gatewayRoutes", yamlRoutes.toString());
                    }
                }
                // Extract eureka from YAML
                if (content.contains("eureka:") && content.contains("defaultZone:")) {
                    Pattern yamlEurekaPattern = Pattern.compile("defaultZone:\\s*([^\\s]+)");
                    Matcher yamlEurekaMatcher = yamlEurekaPattern.matcher(content);
                    if (yamlEurekaMatcher.find() && !config.containsKey("eurekaServiceUrl")) {
                        config.put("eurekaServiceUrl", yamlEurekaMatcher.group(1));
                    }
                }
            } catch (IOException e) {
                log.debug("Could not read application.yml", e);
            }
        }

        return config;
    }

    // Helper class for communication methods detection result
    private static class CommunicationMethodsResult {
        boolean hasRestTemplate = false;
        boolean hasWebClient = false;
        boolean hasKafka = false;
        boolean hasRabbitmq = false;
        boolean hasGrpc = false;
        boolean hasCircuitBreaker = false;
        Set<String> methods = new HashSet<>();
    }

    private CommunicationMethodsResult detectCommunicationMethods(Path modulePath) {
        CommunicationMethodsResult result = new CommunicationMethodsResult();
        
        try (Stream<Path> walk = Files.walk(modulePath)) {
            List<Path> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/") && !p.toString().contains("\\test\\"))
                    .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                try {
                    String content = Files.readString(file);
                    
                    // Check for RestTemplate
                    if (content.contains("RestTemplate")) {
                        result.hasRestTemplate = true;
                        result.methods.add("REST_TEMPLATE");
                    }
                    
                    // Check for WebClient
                    if (WEB_CLIENT_PATTERN.matcher(content).find()) {
                        result.hasWebClient = true;
                        result.methods.add("WEB_CLIENT");
                    }
                    
                    // Check for Feign
                    if (content.contains("@FeignClient")) {
                        result.methods.add("FEIGN_CLIENT");
                    }
                    
                    // Check for Kafka
                    if (content.contains("@KafkaListener") || content.contains("KafkaTemplate")) {
                        result.hasKafka = true;
                        result.methods.add("KAFKA");
                    }
                    
                    // Check for RabbitMQ
                    if (content.contains("@RabbitListener") || content.contains("RabbitTemplate") || content.contains("@RabbitHandler")) {
                        result.hasRabbitmq = true;
                        result.methods.add("RABBITMQ");
                    }
                    
                    // Check for gRPC
                    if (GRPC_PATTERN.matcher(content).find()) {
                        result.hasGrpc = true;
                        result.methods.add("GRPC");
                    }
                    
                    // Check for Circuit Breaker
                    if (CIRCUIT_BREAKER_PATTERN.matcher(content).find()) {
                        result.hasCircuitBreaker = true;
                    }
                    
                    // Check for Load Balancer
                    if (LOAD_BALANCED_PATTERN.matcher(content).find()) {
                        result.methods.add("LOAD_BALANCED");
                    }
                    
                } catch (IOException e) {
                    log.debug("Error reading file: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.debug("Error walking module path", e);
        }
        
        return result;
    }

    private ServiceType detectServiceType(Path modulePath) {
        Set<String> dependencies = parseDependencies(modulePath);
        
        if (dependencies.contains("spring-cloud-starter-gateway") 
                || dependencies.contains("spring-cloud-starter-netflix-zuul")) {
            return ServiceType.API_GATEWAY;
        }
        if (dependencies.contains("spring-cloud-config-server")) {
            return ServiceType.CONFIG_SERVER;
        }
        if (dependencies.contains("spring-cloud-starter-netflix-eureka-server")) {
            return ServiceType.DISCOVERY_SERVER;
        }
        if (dependencies.contains("spring-boot-admin-starter-server")) {
            return ServiceType.ADMIN_SERVICE;
        }
        if (dependencies.contains("spring-boot-starter-batch")) {
            return ServiceType.BATCH_SERVICE;
        }
        if (dependencies.stream().anyMatch(d -> d.contains("kafka") || d.contains("rabbitmq") || d.contains("amqp"))) {
            // Check if it's primarily a messaging service
            if (!dependencies.contains("spring-boot-starter-web")) {
                return ServiceType.MESSAGING_SERVICE;
            }
        }
        if (dependencies.contains("spring-boot-starter-web") || dependencies.contains("spring-boot-starter-webflux")) {
            return ServiceType.BUSINESS_SERVICE;
        }

        // Check for scheduled service
        try {
            boolean hasScheduled = Files.walk(modulePath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .anyMatch(p -> {
                        try {
                            return Files.readString(p).contains("@Scheduled");
                        } catch (IOException e) {
                            return false;
                        }
                    });
            if (hasScheduled) {
                return ServiceType.SCHEDULED_SERVICE;
            }
        } catch (IOException e) {
            log.debug("Error checking for scheduled tasks", e);
        }

        return ServiceType.UNKNOWN;
    }

    private Set<String> parseDependencies(Path modulePath) {
        Set<String> dependencies = new HashSet<>();
        
        // Parse pom.xml
        Path pomFile = modulePath.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            try {
                String content = Files.readString(pomFile);
                Pattern artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
                Matcher matcher = artifactPattern.matcher(content);
                while (matcher.find()) {
                    dependencies.add(matcher.group(1));
                }
            } catch (IOException e) {
                log.debug("Could not parse pom.xml", e);
            }
        }

        // Parse build.gradle
        Path gradleFile = modulePath.resolve("build.gradle");
        if (Files.exists(gradleFile)) {
            try {
                String content = Files.readString(gradleFile);
                Pattern depPattern = Pattern.compile("['\"]([\\w.-]+:[\\w.-]+)(?::[\\w.-]+)?['\"]");
                Matcher matcher = depPattern.matcher(content);
                while (matcher.find()) {
                    String[] parts = matcher.group(1).split(":");
                    if (parts.length >= 2) {
                        dependencies.add(parts[1]);
                    }
                }
            } catch (IOException e) {
                log.debug("Could not parse build.gradle", e);
            }
        }

        return dependencies;
    }

    private int countJavaFiles(Path modulePath) {
        try (Stream<Path> walk = Files.walk(modulePath)) {
            return (int) walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private int countEndpoints(Path modulePath) {
        int count = 0;
        try (Stream<Path> walk = Files.walk(modulePath)) {
            List<Path> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))
                    .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                if (content.contains("@RestController") || content.contains("@Controller")) {
                    Pattern mappingPattern = Pattern.compile("@(Get|Post|Put|Delete|Patch|Request)Mapping");
                    Matcher matcher = mappingPattern.matcher(content);
                    while (matcher.find()) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Error counting endpoints", e);
        }
        return count;
    }

    private List<ServiceCommunication> analyzeServiceCommunications(
            Project project, Path rootPath, List<Microservice> microservices) {
        
        List<ServiceCommunication> communications = new ArrayList<>();
        
        try (Stream<Path> walk = Files.walk(rootPath)) {
            List<Path> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))
                    .collect(Collectors.toList());

            for (Path file : javaFiles) {
                try {
                    String content = Files.readString(file);
                    String className = extractClassName(file);
                    String sourceService = findServiceForFile(file, rootPath, microservices);

                    // Detect Feign clients
                    communications.addAll(detectFeignClients(project, content, className, sourceService));
                    
                    // Detect RestTemplate calls
                    communications.addAll(detectRestTemplateCalls(project, content, className, sourceService));
                    
                    // Detect WebClient calls
                    communications.addAll(detectWebClientCalls(project, content, className, sourceService));
                    
                    // Detect Kafka communications
                    communications.addAll(detectKafkaCommunications(project, content, className, sourceService));
                    
                    // Detect RabbitMQ communications
                    communications.addAll(detectRabbitMQCommunications(project, content, className, sourceService));

                } catch (IOException e) {
                    log.debug("Error reading file: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Error walking directory", e);
        }

        return communications;
    }

    private String extractClassName(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.replace(".java", "");
    }

    private String findServiceForFile(Path file, Path rootPath, List<Microservice> microservices) {
        String filePath = rootPath.relativize(file).toString();
        
        for (Microservice service : microservices) {
            if (service.getModulePath() != null && !service.getModulePath().isEmpty()) {
                if (filePath.startsWith(service.getModulePath())) {
                    return service.getName();
                }
            }
        }
        
        // Default to first service or project name
        return microservices.isEmpty() ? "main" : microservices.get(0).getName();
    }

    private List<ServiceCommunication> detectFeignClients(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        if (content.contains("@FeignClient")) {
            Matcher matcher = FEIGN_CLIENT_PATTERN.matcher(content);
            while (matcher.find()) {
                String targetService = matcher.group(1);
                
                // Also try to get the URL if specified
                String url = null;
                Matcher urlMatcher = FEIGN_CLIENT_URL_PATTERN.matcher(content);
                if (urlMatcher.find()) {
                    url = urlMatcher.group(1);
                }
                
                // Check if load balanced
                boolean isLoadBalanced = content.contains("lb://") || !content.contains("url =");
                
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetService(targetService)
                        .targetUrl(url)
                        .communicationType(CommunicationType.FEIGN_CLIENT)
                        .className(className)
                        .feignClientName(targetService)
                        .isLoadBalanced(isLoadBalanced)
                        .isAsync(false)
                        .description("Feign client calling " + targetService + (isLoadBalanced ? " via service discovery" : ""))
                        .build());
            }
            
            // Extract methods in the Feign interface
            Pattern methodPattern = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping\\s*\\([^)]*[\"']([^\"']*)[\"']");
            Matcher methodMatcher = methodPattern.matcher(content);
            while (methodMatcher.find()) {
                String httpMethod = methodMatcher.group(1).toUpperCase();
                String path = methodMatcher.group(2);
                // Add individual endpoint communications
                if (!comms.isEmpty()) {
                    ServiceCommunication lastComm = comms.get(comms.size() - 1);
                    if (lastComm.getEndpointPath() == null) {
                        lastComm.setEndpointPath(path);
                        lastComm.setHttpMethod(httpMethod);
                    }
                }
            }
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectRestTemplateCalls(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        if (content.contains("RestTemplate") || content.contains("restTemplate")) {
            // Check if using @LoadBalanced
            boolean isLoadBalanced = content.contains("@LoadBalanced") || content.contains("lb://");
            
            Matcher urlMatcher = REST_TEMPLATE_URL_PATTERN.matcher(content);
            Set<String> processedUrls = new HashSet<>();
            
            while (urlMatcher.find()) {
                String url = urlMatcher.group(1);
                if (processedUrls.contains(url)) continue;
                processedUrls.add(url);
                
                // Determine HTTP method
                String httpMethod = "GET";
                if (content.contains("postFor") || content.contains(".post(")) httpMethod = "POST";
                else if (content.contains(".put(")) httpMethod = "PUT";
                else if (content.contains(".delete(")) httpMethod = "DELETE";
                
                // Try to extract target service name from URL
                String targetService = extractServiceNameFromUrl(url);
                
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetService(targetService)
                        .targetUrl(url)
                        .communicationType(CommunicationType.REST_TEMPLATE)
                        .httpMethod(httpMethod)
                        .className(className)
                        .isLoadBalanced(isLoadBalanced)
                        .isAsync(false)
                        .description("RestTemplate " + httpMethod + " call" + (isLoadBalanced ? " with load balancing" : ""))
                        .build());
            }
            
            // If no specific URLs found but RestTemplate is used
            if (comms.isEmpty() && REST_TEMPLATE_PATTERN.matcher(content).find()) {
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .communicationType(CommunicationType.REST_TEMPLATE)
                        .className(className)
                        .isLoadBalanced(isLoadBalanced)
                        .isAsync(false)
                        .description("RestTemplate HTTP calls detected")
                        .build());
            }
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectWebClientCalls(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        if (WEB_CLIENT_PATTERN.matcher(content).find()) {
            Matcher urlMatcher = WEB_CLIENT_URL_PATTERN.matcher(content);
            Set<String> processedUrls = new HashSet<>();
            
            while (urlMatcher.find()) {
                String url = urlMatcher.group(1);
                if (processedUrls.contains(url)) continue;
                processedUrls.add(url);
                
                String targetService = extractServiceNameFromUrl(url);
                boolean isLoadBalanced = url.startsWith("lb://") || content.contains("@LoadBalanced");
                
                // Check for reactive/async patterns
                boolean isAsync = content.contains(".subscribe(") || content.contains("Mono<") || content.contains("Flux<");
                
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetService(targetService)
                        .targetUrl(url)
                        .communicationType(CommunicationType.WEB_CLIENT)
                        .className(className)
                        .isLoadBalanced(isLoadBalanced)
                        .isAsync(isAsync)
                        .description("WebClient call" + (isAsync ? " (reactive/async)" : "") + (isLoadBalanced ? " with load balancing" : ""))
                        .build());
            }
            
            // If WebClient is used but no specific URLs found
            if (comms.isEmpty()) {
                boolean isAsync = content.contains(".subscribe(") || content.contains("Mono<") || content.contains("Flux<");
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .communicationType(CommunicationType.WEB_CLIENT)
                        .className(className)
                        .isAsync(isAsync)
                        .description("WebClient HTTP calls detected" + (isAsync ? " (reactive)" : ""))
                        .build());
            }
        }
        
        return comms;
    }
    
    private String extractServiceNameFromUrl(String url) {
        if (url == null) return null;
        
        // Handle lb:// URLs (load balanced)
        if (url.startsWith("lb://")) {
            String serviceName = url.substring(5);
            int slashIdx = serviceName.indexOf('/');
            return slashIdx > 0 ? serviceName.substring(0, slashIdx) : serviceName;
        }
        
        // Handle http:// URLs
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                String host = url.replaceFirst("https?://", "").split("[:/]")[0];
                // Check if it looks like a service name (not an IP or localhost)
                if (!host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !host.equals("localhost")) {
                    return host;
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        // Handle ${} placeholders
        if (url.contains("${")) {
            Pattern placeholderPattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher m = placeholderPattern.matcher(url);
            if (m.find()) {
                String placeholder = m.group(1);
                // Extract meaningful name from placeholder like ${user-service.url}
                if (placeholder.contains(".")) {
                    return placeholder.split("\\.")[0];
                }
            }
        }
        
        return null;
    }

    private List<ServiceCommunication> detectKafkaCommunications(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        // Kafka listeners (consumers)
        Matcher listenerMatcher = KAFKA_LISTENER_PATTERN.matcher(content);
        while (listenerMatcher.find()) {
            String topic = listenerMatcher.group(1);
            
            // Try to find the method name
            String methodName = extractMethodNameAfterAnnotation(content, listenerMatcher.end());
            
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.KAFKA)
                    .messageChannel(topic)
                    .className(className)
                    .methodName(methodName)
                    .isAsync(true)
                    .description("Kafka consumer listening to topic: " + topic)
                    .build());
        }
        
        // Kafka template (producer)
        Matcher templateMatcher = KAFKA_TEMPLATE_PATTERN.matcher(content);
        Set<String> processedTopics = new HashSet<>();
        while (templateMatcher.find()) {
            String topic = templateMatcher.group(1);
            if (processedTopics.contains(topic)) continue;
            processedTopics.add(topic);
            
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.KAFKA)
                    .messageChannel(topic + " (producer)")
                    .className(className)
                    .isAsync(true)
                    .description("Kafka producer sending to topic: " + topic)
                    .build());
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectRabbitMQCommunications(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        // RabbitMQ listeners (consumers)
        Matcher listenerMatcher = RABBIT_LISTENER_PATTERN.matcher(content);
        while (listenerMatcher.find()) {
            String queue = listenerMatcher.group(1);
            String methodName = extractMethodNameAfterAnnotation(content, listenerMatcher.end());
            
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.RABBITMQ)
                    .messageChannel(queue)
                    .className(className)
                    .methodName(methodName)
                    .isAsync(true)
                    .description("RabbitMQ consumer listening to queue: " + queue)
                    .build());
        }
        
        // RabbitMQ template (producer)
        Matcher templateMatcher = RABBIT_TEMPLATE_PATTERN.matcher(content);
        Set<String> processedExchanges = new HashSet<>();
        while (templateMatcher.find()) {
            String exchange = templateMatcher.group(2);
            if (processedExchanges.contains(exchange)) continue;
            processedExchanges.add(exchange);
            
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.RABBITMQ)
                    .messageChannel(exchange + " (producer)")
                    .className(className)
                    .isAsync(true)
                    .description("RabbitMQ producer sending to exchange: " + exchange)
                    .build());
        }
        
        return comms;
    }
    
    private String extractMethodNameAfterAnnotation(String content, int position) {
        // Find the method declaration after the annotation
        String remaining = content.substring(position);
        Pattern methodPattern = Pattern.compile("\\s*(public|private|protected)?\\s*\\w+\\s+(\\w+)\\s*\\(");
        Matcher m = methodPattern.matcher(remaining);
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    // Additional method to detect gateway routes as communications
    public List<ServiceCommunication> detectGatewayRoutes(
            Project project, Path modulePath, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        try {
            // Check application.yml for gateway routes
            Path ymlFile = modulePath.resolve("src/main/resources/application.yml");
            if (Files.exists(ymlFile)) {
                String content = Files.readString(ymlFile);
                
                // Parse gateway routes from YAML
                Pattern routeUriPattern = Pattern.compile("uri:\\s*(lb://([\\w-]+)|http[s]?://([^\\s]+))");
                Pattern routeIdPattern = Pattern.compile("id:\\s*([\\w-]+)");
                Pattern predicatePattern = Pattern.compile("Path=([^\\s,]+)");
                
                Matcher uriMatcher = routeUriPattern.matcher(content);
                while (uriMatcher.find()) {
                    String fullUri = uriMatcher.group(1);
                    String serviceName = uriMatcher.group(2); // lb:// service name
                    if (serviceName == null) {
                        serviceName = uriMatcher.group(3); // http URL
                    }
                    
                    boolean isLoadBalanced = fullUri.startsWith("lb://");
                    
                    comms.add(ServiceCommunication.builder()
                            .project(project)
                            .sourceService(sourceService)
                            .targetService(serviceName)
                            .targetUrl(fullUri)
                            .communicationType(CommunicationType.GATEWAY_ROUTE)
                            .isLoadBalanced(isLoadBalanced)
                            .isAsync(false)
                            .description("Gateway route to " + serviceName + (isLoadBalanced ? " via service discovery" : ""))
                            .build());
                }
            }
            
            // Check application.properties for gateway routes
            Path propsFile = modulePath.resolve("src/main/resources/application.properties");
            if (Files.exists(propsFile)) {
                String content = Files.readString(propsFile);
                Matcher routeMatcher = GATEWAY_ROUTE_PATTERN.matcher(content);
                while (routeMatcher.find()) {
                    String uri = routeMatcher.group(1).trim();
                    String serviceName = extractServiceNameFromUrl(uri);
                    boolean isLoadBalanced = uri.startsWith("lb://");
                    
                    comms.add(ServiceCommunication.builder()
                            .project(project)
                            .sourceService(sourceService)
                            .targetService(serviceName)
                            .targetUrl(uri)
                            .communicationType(CommunicationType.GATEWAY_ROUTE)
                            .isLoadBalanced(isLoadBalanced)
                            .isAsync(false)
                            .description("Gateway route to " + (serviceName != null ? serviceName : uri))
                            .build());
                }
            }
        } catch (IOException e) {
            log.debug("Error detecting gateway routes", e);
        }
        
        return comms;
    }

    public List<Microservice> getMicroservices(Long projectId) {
        return microserviceRepository.findByProjectId(projectId);
    }

    public List<ServiceCommunication> getCommunications(Long projectId) {
        return communicationRepository.findByProjectId(projectId);
    }
}
