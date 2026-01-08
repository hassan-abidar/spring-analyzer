package com.springanalyzer.service;

import com.springanalyzer.dto.DataFlowResponse;
import com.springanalyzer.entity.*;
import com.springanalyzer.repository.AnalyzedClassRepository;
import com.springanalyzer.repository.EndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFlowAnalyzerService {

    private final AnalyzedClassRepository classRepository;
    private final EndpointRepository endpointRepository;

    public DataFlowResponse analyzeDataFlow(Long projectId) {
        try {
            log.info("Starting data flow analysis for project: {}", projectId);

            // Get all classes for the project
            List<AnalyzedClass> allClasses = classRepository.findByProjectId(projectId);

            // Categorize classes
            Map<String, List<AnalyzedClass>> classCategories = categorizeClasses(allClasses);
            
            // Build nodes for each layer
            List<DataFlowNode> nodes = buildNodes(classCategories, projectId);
            
            // Build edges (connections between nodes)
            List<DataFlowEdge> edges = buildEdges(nodes, classCategories);
            
            // Build flow paths from API endpoints
            List<DataFlowResponse.FlowPath> flowPaths = buildFlowPaths(classCategories, nodes, edges, projectId);
            
            // Create summary
            DataFlowResponse.Summary summary = createSummary(nodes, edges, classCategories, flowPaths);
            
            // Create layer information
            List<DataFlowResponse.LayerInfo> layers = createLayers(nodes);

            DataFlowResponse response = new DataFlowResponse();
            response.setSummary(summary);
            response.setNodes(nodes);
            response.setEdges(edges);
            response.setFlowPaths(flowPaths);
            response.setLayers(layers);

            log.info("Data flow analysis completed. Nodes: {}, Edges: {}, Paths: {}", 
                    nodes.size(), edges.size(), flowPaths.size());

            return response;
        } catch (Exception e) {
            log.error("Error analyzing data flow for project: {}", projectId, e);
            return new DataFlowResponse();
        }
    }

    private Map<String, List<AnalyzedClass>> categorizeClasses(List<AnalyzedClass> allClasses) {
        Map<String, List<AnalyzedClass>> categories = new HashMap<>();
        
        List<AnalyzedClass> controllers = allClasses.stream()
                .filter(c -> c.getName().endsWith("Controller") || (c.getAnnotations() != null && c.getAnnotations().contains("Controller")))
                .collect(Collectors.toList());
        
        List<AnalyzedClass> services = allClasses.stream()
                .filter(c -> c.getName().endsWith("Service") || (c.getAnnotations() != null && c.getAnnotations().contains("Service")))
                .collect(Collectors.toList());
        
        List<AnalyzedClass> repositories = allClasses.stream()
                .filter(c -> c.getName().endsWith("Repository") || (c.getAnnotations() != null && c.getAnnotations().contains("Repository")))
                .collect(Collectors.toList());
        
        List<AnalyzedClass> entities = allClasses.stream()
                .filter(c -> c.getAnnotations() != null && c.getAnnotations().contains("Entity"))
                .collect(Collectors.toList());
        
        List<AnalyzedClass> dtos = allClasses.stream()
                .filter(c -> (c.getName().endsWith("DTO") || 
                             c.getName().endsWith("Dto") ||
                             c.getName().endsWith("Request") ||
                             c.getName().endsWith("Response")) &&
                           !controllers.contains(c) && !services.contains(c) && 
                           !repositories.contains(c) && !entities.contains(c))
                .collect(Collectors.toList());
        
        categories.put("CONTROLLER", controllers);
        categories.put("SERVICE", services);
        categories.put("REPOSITORY", repositories);
        categories.put("ENTITY", entities);
        categories.put("DTO", dtos);
        
        return categories;
    }

    private List<DataFlowNode> buildNodes(Map<String, List<AnalyzedClass>> classCategories, Long projectId) {
        List<DataFlowNode> nodes = new ArrayList<>();
        int nodeId = 0;

        // Controllers (Layer 0)
        for (AnalyzedClass controller : classCategories.getOrDefault("CONTROLLER", new ArrayList<>())) {
            nodes.add(createNode(controller, "API", 0, nodeId++));
        }

        // Services (Layer 1)
        for (AnalyzedClass service : classCategories.getOrDefault("SERVICE", new ArrayList<>())) {
            nodes.add(createNode(service, "SERVICE", 1, nodeId++));
        }

        // Repositories (Layer 2)
        for (AnalyzedClass repository : classCategories.getOrDefault("REPOSITORY", new ArrayList<>())) {
            nodes.add(createNode(repository, "REPOSITORY", 2, nodeId++));
        }

        // Entities (Layer 3)
        for (AnalyzedClass entity : classCategories.getOrDefault("ENTITY", new ArrayList<>())) {
            nodes.add(createNode(entity, "ENTITY", 3, nodeId++));
        }

        return nodes;
    }

    private DataFlowNode createNode(AnalyzedClass clazz, String type, int layer, int id) {
        DataFlowNode node = new DataFlowNode();
        node.setId(type + "_" + id);
        node.setName(clazz.getName());
        node.setType(type);
        node.setClassName(clazz.getName());
        node.setLayer(layer);
        node.setDescription(clazz.getName());
        return node;
    }

    private List<DataFlowEdge> buildEdges(List<DataFlowNode> nodes, 
                                                            Map<String, List<AnalyzedClass>> classCategories) {
        List<DataFlowEdge> edges = new ArrayList<>();
        int edgeId = 0;

        // Controllers → Services
        for (DataFlowNode controller : nodes.stream().filter(n -> "API".equals(n.getType())).collect(Collectors.toList())) {
            for (DataFlowNode service : nodes.stream().filter(n -> "SERVICE".equals(n.getType())).collect(Collectors.toList())) {
                if (shouldConnect(controller, service)) {
                    edges.add(createEdge(edgeId++, controller, service, "CALLS", "service call"));
                }
            }
        }

        // Services → Repositories
        for (DataFlowNode service : nodes.stream().filter(n -> "SERVICE".equals(n.getType())).collect(Collectors.toList())) {
            for (DataFlowNode repo : nodes.stream().filter(n -> "REPOSITORY".equals(n.getType())).collect(Collectors.toList())) {
                if (shouldConnect(service, repo)) {
                    edges.add(createEdge(edgeId++, service, repo, "CALLS", "data access"));
                }
            }
        }

        // Repositories → Entities
        for (DataFlowNode repo : nodes.stream().filter(n -> "REPOSITORY".equals(n.getType())).collect(Collectors.toList())) {
            for (DataFlowNode entity : nodes.stream().filter(n -> "ENTITY".equals(n.getType())).collect(Collectors.toList())) {
                if (shouldConnect(repo, entity)) {
                    edges.add(createEdge(edgeId++, repo, entity, "USES", "entity mapping"));
                }
            }
        }

        return edges;
    }

    private boolean shouldConnect(DataFlowNode source, DataFlowNode target) {
        // Connect if it's the logical layer progression
        if (source.getLayer() < target.getLayer()) {
            return true;
        }
        return false;
    }

    private DataFlowEdge createEdge(int id, DataFlowNode source, 
                                                      DataFlowNode target, String type, String label) {
        DataFlowEdge edge = new DataFlowEdge();
        edge.setId("edge_" + id);
        edge.setSource(source.getId());
        edge.setTarget(target.getId());
        edge.setType(type);
        edge.setLabel(label);
        edge.setDataType(target.getClassName());
        edge.setTransformation("TRANSFORMS".equals(type));
        return edge;
    }

    private List<DataFlowResponse.FlowPath> buildFlowPaths(Map<String, List<AnalyzedClass>> classCategories, 
                                                           List<DataFlowNode> nodes, 
                                                           List<DataFlowEdge> edges,
                                                           Long projectId) {
        List<DataFlowResponse.FlowPath> paths = new ArrayList<>();
        
        // Create flow path for each endpoint
        List<Endpoint> endpoints = endpointRepository.findByProjectId(projectId);
        
        for (Endpoint endpoint : endpoints.stream().limit(10).collect(Collectors.toList())) {
            DataFlowResponse.FlowPath path = new DataFlowResponse.FlowPath();
            path.setId("path_" + endpoint.getId());
            path.setName(endpoint.getHttpMethod() + " " + endpoint.getPath());
            path.setEndpoint(endpoint.getPath());
            path.setReturnType(endpoint.getReturnType());
            
            // Build simple flow path: Controller -> Service -> Repository -> Entity
            List<String> nodeIds = new ArrayList<>();
            List<String> descriptions = new ArrayList<>();
            
            // Find controller node
            nodes.stream()
                    .filter(n -> "API".equals(n.getType()))
                    .findFirst()
                    .ifPresent(n -> {
                        nodeIds.add(n.getId());
                        descriptions.add("API Endpoint: " + endpoint.getPath());
                    });
            
            // Add service, repo, entity
            nodes.stream().filter(n -> "SERVICE".equals(n.getType())).findFirst()
                    .ifPresent(n -> {
                        nodeIds.add(n.getId());
                        descriptions.add("Process in " + n.getClassName());
                    });
            
            nodes.stream().filter(n -> "REPOSITORY".equals(n.getType())).findFirst()
                    .ifPresent(n -> {
                        nodeIds.add(n.getId());
                        descriptions.add("Access data via " + n.getClassName());
                    });
            
            nodes.stream().filter(n -> "ENTITY".equals(n.getType())).findFirst()
                    .ifPresent(n -> {
                        nodeIds.add(n.getId());
                        descriptions.add("Entity: " + n.getClassName());
                    });
            
            if (!nodeIds.isEmpty()) {
                path.setNodeIds(nodeIds);
                path.setDescriptions(descriptions);
                paths.add(path);
            }
        }
        
        return paths;
    }

    private List<DataFlowResponse.LayerInfo> createLayers(List<DataFlowNode> nodes) {
        List<DataFlowResponse.LayerInfo> layers = new ArrayList<>();
        
        String[] layerNames = {"API Layer", "Service Layer", "Repository Layer", "Entity Layer"};
        
        for (int i = 0; i <= 3; i++) {
            final int layer = i;
            List<DataFlowNode> layerNodes = nodes.stream()
                    .filter(n -> n.getLayer() == layer)
                    .collect(Collectors.toList());
            
            if (!layerNodes.isEmpty()) {
                DataFlowResponse.LayerInfo info = new DataFlowResponse.LayerInfo();
                info.setLayerName(layerNames[i]);
                info.setNodeCount(layerNodes.size());
                info.setNodeNames(layerNodes.stream()
                        .map(DataFlowNode::getName)
                        .distinct()
                        .collect(Collectors.toList()));
                layers.add(info);
            }
        }
        
        return layers;
    }

    private DataFlowResponse.Summary createSummary(List<DataFlowNode> nodes, 
                                                    List<DataFlowEdge> edges, 
                                                    Map<String, List<AnalyzedClass>> classCategories,
                                                    List<DataFlowResponse.FlowPath> flowPaths) {
        DataFlowResponse.Summary summary = new DataFlowResponse.Summary();
        
        summary.setTotalNodes(nodes.size());
        summary.setTotalFlows(edges.size());
        summary.setDtoCount(classCategories.getOrDefault("DTO", new ArrayList<>()).size());
        summary.setEntityCount(classCategories.getOrDefault("ENTITY", new ArrayList<>()).size());
        summary.setControllerCount(classCategories.getOrDefault("CONTROLLER", new ArrayList<>()).size());
        summary.setServiceCount(classCategories.getOrDefault("SERVICE", new ArrayList<>()).size());
        summary.setRepositoryCount(classCategories.getOrDefault("REPOSITORY", new ArrayList<>()).size());
        
        Map<String, Integer> layerCounts = new HashMap<>();
        for (int i = 0; i <= 3; i++) {
            final int layer = i;
            long count = nodes.stream().filter(n -> n.getLayer() == layer).count();
            if (count > 0) {
                layerCounts.put("Layer " + i, (int) count);
            }
        }
        summary.setLayerCounts(layerCounts);
        
        if (!nodes.isEmpty()) {
            summary.setAvgLayerDepth((int) nodes.stream().mapToInt(DataFlowNode::getLayer).average().orElse(0));
        }
        
        return summary;
    }
}
