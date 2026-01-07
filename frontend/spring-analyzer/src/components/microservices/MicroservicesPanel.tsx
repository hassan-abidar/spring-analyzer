import { useState } from 'react';
import { 
  Server, 
  ArrowRightLeft, 
  Cloud, 
  Database, 
  Shield, 
  Zap, 
  Radio,
  GitBranch,
  Network,
  MessageSquare,
  CheckCircle,
  XCircle
} from 'lucide-react';
import { MicroservicesResult, MicroserviceInfo, CommunicationInfo } from '../../types/microservices.types';
import Card from '../common/Card';
import './MicroservicesPanel.css';

interface Props {
  data: MicroservicesResult;
}

const serviceTypeIcons: Record<string, React.ReactNode> = {
  API_GATEWAY: <Network size={16} />,
  DISCOVERY_SERVER: <Cloud size={16} />,
  CONFIG_SERVER: <Database size={16} />,
  BUSINESS_SERVICE: <Server size={16} />,
  MESSAGING_SERVICE: <MessageSquare size={16} />,
};

const serviceTypeColors: Record<string, string> = {
  API_GATEWAY: '#8b5cf6',
  DISCOVERY_SERVER: '#10b981',
  CONFIG_SERVER: '#f59e0b',
  BUSINESS_SERVICE: '#3b82f6',
  MESSAGING_SERVICE: '#ec4899',
  BATCH_SERVICE: '#06b6d4',
  SCHEDULED_SERVICE: '#f97316',
  ADMIN_SERVICE: '#6366f1',
  UNKNOWN: '#64748b'
};

const commTypeColors: Record<string, string> = {
  FEIGN_CLIENT: '#8b5cf6',
  REST_TEMPLATE: '#3b82f6',
  WEB_CLIENT: '#06b6d4',
  KAFKA: '#f97316',
  RABBITMQ: '#f59e0b',
  GATEWAY_ROUTE: '#10b981',
  GRPC: '#ec4899',
  LOAD_BALANCED: '#6366f1',
  DISCOVERY_CLIENT: '#22c55e',
};

export function MicroservicesPanel({ data }: Props) {
  const [selectedService, setSelectedService] = useState<MicroserviceInfo | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'services' | 'communications'>('overview');

  const { summary, services, communications } = data;

  return (
    <div className="microservices-panel">
      {/* Summary Cards */}
      <div className="ms-summary-grid">
        <Card className="ms-summary-card">
          <div className="ms-summary-icon" style={{ backgroundColor: '#3b82f620' }}>
            <Server size={20} color="#3b82f6" />
          </div>
          <div className="ms-summary-content">
            <span className="ms-summary-value">{summary.totalServices}</span>
            <span className="ms-summary-label">Microservices</span>
          </div>
        </Card>

        <Card className="ms-summary-card">
          <div className="ms-summary-icon" style={{ backgroundColor: '#8b5cf620' }}>
            <ArrowRightLeft size={20} color="#8b5cf6" />
          </div>
          <div className="ms-summary-content">
            <span className="ms-summary-value">{summary.totalCommunications}</span>
            <span className="ms-summary-label">Communications</span>
          </div>
        </Card>

        <Card className="ms-summary-card">
          <div className="ms-summary-icon" style={{ backgroundColor: summary.hasServiceDiscovery ? '#10b98120' : '#64748b20' }}>
            <Cloud size={20} color={summary.hasServiceDiscovery ? '#10b981' : '#64748b'} />
          </div>
          <div className="ms-summary-content">
            <span className="ms-summary-value">{summary.hasServiceDiscovery ? 'Yes' : 'No'}</span>
            <span className="ms-summary-label">Service Discovery</span>
          </div>
        </Card>

        <Card className="ms-summary-card">
          <div className="ms-summary-icon" style={{ backgroundColor: summary.hasApiGateway ? '#f59e0b20' : '#64748b20' }}>
            <Network size={20} color={summary.hasApiGateway ? '#f59e0b' : '#64748b'} />
          </div>
          <div className="ms-summary-content">
            <span className="ms-summary-value">{summary.hasApiGateway ? 'Yes' : 'No'}</span>
            <span className="ms-summary-label">API Gateway</span>
          </div>
        </Card>
      </div>

      {/* Architecture Features */}
      <Card className="ms-features-card">
        <h3>Architecture Features</h3>
        <div className="ms-features-grid">
          <FeatureBadge 
            label="Service Discovery (Eureka)" 
            enabled={summary.hasServiceDiscovery}
            detail={summary.eurekaServerUrl}
          />
          <FeatureBadge label="API Gateway" enabled={summary.hasApiGateway} />
          <FeatureBadge label="Config Server" enabled={summary.hasConfigServer} />
          <FeatureBadge label="Load Balancing" enabled={summary.hasLoadBalancing} />
          <FeatureBadge label="Circuit Breaker" enabled={summary.hasCircuitBreaker} />
        </div>

        {summary.communicationMethods.length > 0 && (
          <div className="ms-comm-methods">
            <h4>Communication Methods Used</h4>
            <div className="ms-methods-list">
              {summary.communicationMethods.map(method => (
                <span 
                  key={method} 
                  className="ms-method-badge"
                  style={{ backgroundColor: commTypeColors[method] || '#64748b' }}
                >
                  {formatCommMethod(method)}
                </span>
              ))}
            </div>
          </div>
        )}

        {summary.messagingTechnologies.length > 0 && (
          <div className="ms-messaging">
            <h4>Messaging Technologies</h4>
            <div className="ms-methods-list">
              {summary.messagingTechnologies.map(tech => (
                <span 
                  key={tech} 
                  className="ms-method-badge"
                  style={{ backgroundColor: commTypeColors[tech] || '#64748b' }}
                >
                  {tech}
                </span>
              ))}
            </div>
          </div>
        )}
      </Card>

      {/* Tabs */}
      <div className="ms-tabs">
        <button 
          className={`ms-tab ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          Overview
        </button>
        <button 
          className={`ms-tab ${activeTab === 'services' ? 'active' : ''}`}
          onClick={() => setActiveTab('services')}
        >
          Services ({services.length})
        </button>
        <button 
          className={`ms-tab ${activeTab === 'communications' ? 'active' : ''}`}
          onClick={() => setActiveTab('communications')}
        >
          Communications ({communications.length})
        </button>
      </div>

      {/* Tab Content */}
      <div className="ms-tab-content">
        {activeTab === 'overview' && (
          <div className="ms-overview">
            <div className="ms-type-breakdown">
              <Card>
                <h4>Service Types</h4>
                <div className="ms-breakdown-list">
                  {Object.entries(summary.serviceTypeBreakdown).map(([type, count]) => (
                    <div key={type} className="ms-breakdown-item">
                      <span 
                        className="ms-type-dot" 
                        style={{ backgroundColor: serviceTypeColors[type] || '#64748b' }}
                      />
                      <span className="ms-type-name">{formatServiceType(type)}</span>
                      <span className="ms-type-count">{count}</span>
                    </div>
                  ))}
                </div>
              </Card>
              <Card>
                <h4>Communication Types</h4>
                <div className="ms-breakdown-list">
                  {Object.entries(summary.communicationTypeBreakdown).map(([type, count]) => (
                    <div key={type} className="ms-breakdown-item">
                      <span 
                        className="ms-type-dot" 
                        style={{ backgroundColor: commTypeColors[type] || '#64748b' }}
                      />
                      <span className="ms-type-name">{formatCommMethod(type)}</span>
                      <span className="ms-type-count">{count}</span>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          </div>
        )}

        {activeTab === 'services' && (
          <div className="ms-services-list">
            {services.map(service => (
              <ServiceCard 
                key={service.id} 
                service={service}
                isSelected={selectedService?.id === service.id}
                onClick={() => setSelectedService(
                  selectedService?.id === service.id ? null : service
                )}
              />
            ))}
          </div>
        )}

        {activeTab === 'communications' && (
          <div className="ms-communications-list">
            {communications.map(comm => (
              <CommunicationCard key={comm.id} communication={comm} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function FeatureBadge({ label, enabled, detail }: { label: string; enabled: boolean; detail?: string | null }) {
  return (
    <div className={`ms-feature-badge ${enabled ? 'enabled' : 'disabled'}`}>
      {enabled ? <CheckCircle size={16} /> : <XCircle size={16} />}
      <span>{label}</span>
      {detail && detail.length > 0 && <span className="ms-feature-detail">{detail}</span>}
    </div>
  );
}

function ServiceCard({ service, isSelected, onClick }: { 
  service: MicroserviceInfo; 
  isSelected: boolean;
  onClick: () => void;
}) {
  const commMethods = [];
  if (service.hasFeignClients) commMethods.push('Feign');
  if (service.hasRestTemplate) commMethods.push('RestTemplate');
  if (service.hasWebClient) commMethods.push('WebClient');
  if (service.hasKafka) commMethods.push('Kafka');
  if (service.hasRabbitmq) commMethods.push('RabbitMQ');
  if (service.hasGrpc) commMethods.push('gRPC');

  return (
    <Card className={`ms-service-card ${isSelected ? 'selected' : ''}`} onClick={onClick}>
      <div className="ms-service-header">
        <div 
          className="ms-service-type-badge"
          style={{ backgroundColor: serviceTypeColors[service.serviceType] || '#64748b' }}
        >
          {serviceTypeIcons[service.serviceType] || <Server size={16} />}
          {formatServiceType(service.serviceType)}
        </div>
        <span className="ms-service-port">:{service.serverPort}</span>
      </div>
      
      <h4 className="ms-service-name">{service.name}</h4>
      <p className="ms-service-app-name">{service.applicationName}</p>
      
      <div className="ms-service-stats">
        <span><Server size={14} /> {service.classCount} classes</span>
        <span><Zap size={14} /> {service.endpointCount} endpoints</span>
      </div>

      {/* Communication Methods */}
      {commMethods.length > 0 && (
        <div className="ms-service-comm">
          <span className="ms-service-comm-label">Communicates via:</span>
          <div className="ms-service-comm-badges">
            {commMethods.map(method => (
              <span key={method} className="ms-comm-badge">{method}</span>
            ))}
          </div>
        </div>
      )}

      {/* Service Discovery */}
      <div className="ms-service-features">
        {service.hasEurekaClient && (
          <span className="ms-feature-mini" title="Registered with Eureka">
            <Cloud size={12} /> Eureka
          </span>
        )}
        {service.hasConfigClient && (
          <span className="ms-feature-mini" title="Uses Config Server">
            <Database size={12} /> Config
          </span>
        )}
        {service.hasLoadBalancer && (
          <span className="ms-feature-mini" title="Load Balanced">
            <GitBranch size={12} /> LB
          </span>
        )}
        {service.hasCircuitBreaker && (
          <span className="ms-feature-mini" title="Circuit Breaker">
            <Shield size={12} /> CB
          </span>
        )}
      </div>

      {/* Gateway Routes */}
      {service.gatewayRoutes && service.gatewayRoutes.length > 0 && (
        <div className="ms-gateway-routes">
          <span className="ms-routes-label">Routes to:</span>
          {service.gatewayRoutes.map((route, idx) => (
            <span key={idx} className="ms-route-badge">{route}</span>
          ))}
        </div>
      )}

      {/* Database */}
      {service.databaseType && (
        <div className="ms-service-db">
          <Database size={14} /> {service.databaseType}
        </div>
      )}

      {isSelected && (
        <div className="ms-service-details">
          <div className="ms-detail-row">
            <span>Base Package:</span>
            <code>{service.basePackage}</code>
          </div>
          {service.eurekaServiceUrl && (
            <div className="ms-detail-row">
              <span>Eureka URL:</span>
              <code>{service.eurekaServiceUrl}</code>
            </div>
          )}
        </div>
      )}
    </Card>
  );
}

function CommunicationCard({ communication }: { communication: CommunicationInfo }) {
  return (
    <Card className="ms-comm-card">
      <div className="ms-comm-header">
        <span 
          className="ms-comm-type-badge"
          style={{ backgroundColor: commTypeColors[communication.communicationType] || '#64748b' }}
        >
          {formatCommMethod(communication.communicationType)}
        </span>
        {communication.isLoadBalanced && (
          <span className="ms-comm-lb-badge">Load Balanced</span>
        )}
        {communication.isAsync && (
          <span className="ms-comm-async-badge">Async</span>
        )}
      </div>

      <div className="ms-comm-flow">
        <span className="ms-comm-source">{communication.sourceService}</span>
        <ArrowRightLeft size={16} />
        <span className="ms-comm-target">
          {communication.targetService || communication.targetUrl || communication.messageChannel || 'Unknown'}
        </span>
      </div>

      {communication.description && (
        <p className="ms-comm-description">{communication.description}</p>
      )}

      <div className="ms-comm-details">
        {communication.httpMethod && (
          <span className="ms-comm-method">{communication.httpMethod}</span>
        )}
        {communication.endpointPath && (
          <span className="ms-comm-path">{communication.endpointPath}</span>
        )}
        {communication.feignClientName && (
          <span className="ms-comm-feign">Feign: {communication.feignClientName}</span>
        )}
        {communication.messageChannel && (
          <span className="ms-comm-channel">
            <Radio size={12} /> {communication.messageChannel}
          </span>
        )}
      </div>

      <div className="ms-comm-location">
        <code>{communication.className}{communication.methodName ? `.${communication.methodName}()` : ''}</code>
      </div>
    </Card>
  );
}

function formatServiceType(type: string): string {
  return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
}

function formatCommMethod(method: string): string {
  const names: Record<string, string> = {
    FEIGN_CLIENT: 'OpenFeign',
    REST_TEMPLATE: 'RestTemplate',
    WEB_CLIENT: 'WebClient',
    KAFKA: 'Kafka',
    RABBITMQ: 'RabbitMQ',
    GATEWAY_ROUTE: 'Gateway Route',
    GRPC: 'gRPC',
    LOAD_BALANCED: 'Load Balanced',
    DISCOVERY_CLIENT: 'Discovery Client',
    JMS: 'JMS',
  };
  return names[method] || method;
}

export default MicroservicesPanel;
