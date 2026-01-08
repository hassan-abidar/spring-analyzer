export interface DataFlowNode {
  id: string;
  name: string;
  type: 'API' | 'SERVICE' | 'REPOSITORY' | 'ENTITY' | 'DTO';
  className: string;
  methodName?: string;
  layer: number;
  description: string;
  dtoNames: string[];
  entityNames: string[];
  methodSignature?: string;
}

export interface DataFlowEdge {
  id: string;
  source: string;
  target: string;
  type: 'CALLS' | 'USES' | 'RETURNS' | 'TRANSFORMS';
  label: string;
  dataType?: string;
  isTransformation: boolean;
  transformationDetails?: string;
}

export interface FlowPath {
  id: string;
  name: string;
  nodeIds: string[];
  descriptions: string[];
  endpoint?: string;
  returnType?: string;
}

export interface LayerInfo {
  layerName: string;
  nodeCount: number;
  nodeNames: string[];
}

export interface DataFlowSummary {
  totalNodes: number;
  totalFlows: number;
  dtoCount: number;
  entityCount: number;
  controllerCount: number;
  serviceCount: number;
  repositoryCount: number;
  avgLayerDepth: number;
  layerCounts: Record<string, number>;
  commonDtoNames: string[];
  commonEntityNames: string[];
}

export interface DataFlowResult {
  summary: DataFlowSummary;
  nodes: DataFlowNode[];
  edges: DataFlowEdge[];
  layers: LayerInfo[];
  flowPaths: FlowPath[];
}
