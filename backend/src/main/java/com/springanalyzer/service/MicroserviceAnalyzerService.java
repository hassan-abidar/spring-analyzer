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
            "@FeignClient\\s*\\(\\s*(?:name\\s*=\\s*)?[\"']([^\"']+)[\"']"
    );
    private static final Pattern REST_TEMPLATE_PATTERN = Pattern.compile(
            "restTemplate\\s*\\.\\s*(getForObject|postForObject|exchange|getForEntity|postForEntity)\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern WEB_CLIENT_PATTERN = Pattern.compile(
            "webClient\\s*(?:\\.\\w+\\([^)]*\\))*\\.uri\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern KAFKA_LISTENER_PATTERN = Pattern.compile(
            "@KafkaListener\\s*\\(\\s*(?:topics\\s*=\\s*)?[{]?[\"']([^\"']+)[\"']"
    );
    private static final Pattern KAFKA_TEMPLATE_PATTERN = Pattern.compile(
            "kafkaTemplate\\s*\\.\\s*send\\s*\\([^)]*[\"']([^\"']+)[\"']"
    );
    private static final Pattern RABBIT_LISTENER_PATTERN = Pattern.compile(
            "@RabbitListener\\s*\\(\\s*(?:queues\\s*=\\s*)?[\"']([^\"']+)[\"']"
    );
    private static final Pattern RABBIT_TEMPLATE_PATTERN = Pattern.compile(
            "rabbitTemplate\\s*\\.\\s*convertAndSend\\s*\\([^)]*[\"']([^\"']+)[\"']"
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
            builder.dependencies(String.join(",", dependencies));

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
            } catch (IOException e) {
                log.debug("Could not read application.yml", e);
            }
        }

        return config;
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
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetService(targetService)
                        .communicationType(CommunicationType.FEIGN_CLIENT)
                        .className(className)
                        .feignClientName(targetService)
                        .build());
            }
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectRestTemplateCalls(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        if (content.contains("RestTemplate") || content.contains("restTemplate")) {
            Matcher matcher = REST_TEMPLATE_PATTERN.matcher(content);
            while (matcher.find()) {
                String method = matcher.group(1);
                String url = matcher.group(2);
                String httpMethod = method.toLowerCase().contains("post") ? "POST" : "GET";
                
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetUrl(url)
                        .communicationType(CommunicationType.REST_TEMPLATE)
                        .httpMethod(httpMethod)
                        .className(className)
                        .build());
            }
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectWebClientCalls(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        if (content.contains("WebClient") || content.contains("webClient")) {
            Matcher matcher = WEB_CLIENT_PATTERN.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                
                comms.add(ServiceCommunication.builder()
                        .project(project)
                        .sourceService(sourceService)
                        .targetUrl(url)
                        .communicationType(CommunicationType.WEB_CLIENT)
                        .className(className)
                        .build());
            }
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectKafkaCommunications(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        // Kafka listeners
        Matcher listenerMatcher = KAFKA_LISTENER_PATTERN.matcher(content);
        while (listenerMatcher.find()) {
            String topic = listenerMatcher.group(1);
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.KAFKA)
                    .messageChannel(topic + " (consumer)")
                    .className(className)
                    .build());
        }
        
        // Kafka template (producer)
        Matcher templateMatcher = KAFKA_TEMPLATE_PATTERN.matcher(content);
        while (templateMatcher.find()) {
            String topic = templateMatcher.group(1);
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.KAFKA)
                    .messageChannel(topic + " (producer)")
                    .className(className)
                    .build());
        }
        
        return comms;
    }

    private List<ServiceCommunication> detectRabbitMQCommunications(
            Project project, String content, String className, String sourceService) {
        
        List<ServiceCommunication> comms = new ArrayList<>();
        
        // RabbitMQ listeners
        Matcher listenerMatcher = RABBIT_LISTENER_PATTERN.matcher(content);
        while (listenerMatcher.find()) {
            String queue = listenerMatcher.group(1);
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.RABBITMQ)
                    .messageChannel(queue + " (consumer)")
                    .className(className)
                    .build());
        }
        
        // RabbitMQ template (producer)
        Matcher templateMatcher = RABBIT_TEMPLATE_PATTERN.matcher(content);
        while (templateMatcher.find()) {
            String exchange = templateMatcher.group(1);
            comms.add(ServiceCommunication.builder()
                    .project(project)
                    .sourceService(sourceService)
                    .communicationType(CommunicationType.RABBITMQ)
                    .messageChannel(exchange + " (producer)")
                    .className(className)
                    .build());
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
