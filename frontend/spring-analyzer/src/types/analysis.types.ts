export interface AnalysisResult {
  projectId: number;
  projectName: string;
  status: string;
  summary: AnalysisSummary;
  classes: ClassInfo[];
  endpoints: EndpointInfo[];
  dependencies: DependencyInfo[];
}

export interface AnalysisSummary {
  totalClasses: number;
  controllers: number;
  services: number;
  repositories: number;
  entities: number;
  endpoints: number;
  dependencies: number;
  classTypeBreakdown: Record<string, number>;
  httpMethodBreakdown: Record<string, number>;
}

export interface ClassInfo {
  id: number;
  name: string;
  packageName: string;
  type: string;
  annotations: string[];
  extendsClass?: string;
  implementsInterfaces: string[];
  fieldCount: number;
  methodCount: number;
}

export interface EndpointInfo {
  id: number;
  httpMethod: string;
  path: string;
  methodName: string;
  returnType: string;
  className?: string;
}

export interface DependencyInfo {
  id: number;
  groupId: string;
  artifactId: string;
  version?: string;
  scope: string;
}
