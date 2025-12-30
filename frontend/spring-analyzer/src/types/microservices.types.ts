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
  messagingTechnologies: string[];
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
  hasEurekaClient: boolean;
  hasConfigClient: boolean;
  hasGateway: boolean;
  hasFeignClients: boolean;
  classCount: number;
  endpointCount: number;
  dependencies: string[];
  messagingTypes: string[];
  consumedServices: string[];
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
}
