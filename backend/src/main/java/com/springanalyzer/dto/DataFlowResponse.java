package com.springanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.springanalyzer.entity.DataFlowNode;
import com.springanalyzer.entity.DataFlowEdge;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataFlowResponse {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalNodes;
        private int totalFlows;
        private int dtoCount;
        private int entityCount;
        private int controllerCount;
        private int serviceCount;
        private int repositoryCount;
        private int avgLayerDepth;
        private Map<String, Integer> layerCounts;
        private List<String> commonDtoNames;
        private List<String> commonEntityNames;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayerInfo {
        private String layerName;
        private int nodeCount;
        private List<String> nodeNames;
    }

    private Summary summary;
    private List<DataFlowNode> nodes;
    private List<DataFlowEdge> edges;
    private List<LayerInfo> layers;
    private List<FlowPath> flowPaths;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowPath {
        private String id;
        private String name;
        private List<String> nodeIds; // Ordered list of node IDs in the flow
        private List<String> descriptions;
        private String endpoint;
        private String returnType;
    }
}
