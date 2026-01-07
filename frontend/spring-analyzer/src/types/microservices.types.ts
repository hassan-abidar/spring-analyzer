export interface MicroservicesResult {
  projectId: number;
  projectName: string;
  isMultiModule: boolean;
  summary: MicroservicesSummary;
  services: MicroserviceInfo[];
  communications: CommunicationInfo[];
}

export interface MicroservicesSummary {
  totalServices: number;
  totalCommunications: number;
  serviceTypeBreakdown: Record<string, number>;
  communicationTypeBreakdown: Record<string, number>;
  hasServiceDiscovery: boolean;
  hasApiGateway: boolean;
  hasConfigServer: boolean;
  hasLoadBalancing: boolean;
  hasCircuitBreaker: boolean;
  messagingTechnologies: string[];
  communicationMethods: string[];
  eurekaServerUrl?: string;
}

export interface MicroserviceInfo {
  id: number;
  name: string;
  applicationName: string;
  basePackage: string;
  modulePath: string;
  serverPort: string;
  serviceType: string;
  profiles: string[];
  // Service Discovery
  hasEurekaClient: boolean;
  hasConfigClient: boolean;
  hasGateway: boolean;
  eurekaServiceUrl?: string;
  // Communication Methods
  hasFeignClients: boolean;
  hasRestTemplate: boolean;
  hasWebClient: boolean;
  hasKafka: boolean;
  hasRabbitmq: boolean;
  hasGrpc: boolean;
  // Resilience
  hasLoadBalancer: boolean;
  hasCircuitBreaker: boolean;
  // Stats
  classCount: number;
  endpointCount: number;
  dependencies: string[];
  messagingTypes: string[];
  communicationMethods: string[];
  consumedServices: string[];
  // Gateway specific
  gatewayRoutes: string[];
  // Database
  databaseType?: string;
}

export interface CommunicationInfo {
  id: number;
  sourceService: string;
  targetService?: string;
  targetUrl?: string;
  communicationType: string;
  httpMethod?: string;
  feignClientName?: string;
  className: string;
  methodName?: string;
  messageChannel?: string;
  endpointPath?: string;
  isLoadBalanced: boolean;
  isAsync: boolean;
  description?: string;
}
