import { useRef, useEffect, useState, useCallback } from 'react';
import { MicroserviceInfo, CommunicationInfo } from '../../types/microservices.types';
import Card from '../common/Card';
import './MicroservicesArchitectureDiagram.css';

interface Props {
  services: MicroserviceInfo[];
  communications: CommunicationInfo[];
}

interface ServiceNode {
  id: string;
  name: string;
  type: string;
  port: string;
  x: number;
  y: number;
  width: number;
  height: number;
  commMethods: string[];
}

interface CommunicationEdge {
  id: number;
  source: string;
  target: string;
  type: string;
  label: string;
  isLoadBalanced: boolean;
  isAsync: boolean;
}

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
  JMS: '#84cc16',
};

export function MicroservicesArchitectureDiagram({ services, communications }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [nodes, setNodes] = useState<ServiceNode[]>([]);
  const [edges, setEdges] = useState<CommunicationEdge[]>([]);
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState<string | null>(null);
  const [panning, setPanning] = useState(false);
  const [lastMouse, setLastMouse] = useState({ x: 0, y: 0 });
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [hoveredEdge] = useState<number | null>(null);

  // Convert services and communications to nodes and edges
  useEffect(() => {
    // Calculate layout - organize by service type
    const nodeWidth = 180;
    const nodeHeight = 100;
    const horizontalSpacing = 250;
    const verticalSpacing = 150;

    // Group services by type for layout
    const gatewayServices = services.filter(s => 
      s.serviceType === 'API_GATEWAY' || s.hasGateway
    );
    const discoveryServices = services.filter(s => 
      s.serviceType === 'DISCOVERY_SERVER'
    );
    const configServices = services.filter(s => 
      s.serviceType === 'CONFIG_SERVER'
    );
    const businessServices = services.filter(s => 
      !['API_GATEWAY', 'DISCOVERY_SERVER', 'CONFIG_SERVER'].includes(s.serviceType) &&
      !s.hasGateway
    );

    const newNodes: ServiceNode[] = [];
    const centerX = 400;
    let yOffset = 50;

    // Infrastructure services at the top
    const infraServices = [...discoveryServices, ...configServices];
    infraServices.forEach((service, idx) => {
      const commMethods = getServiceCommMethods(service);
      newNodes.push({
        id: service.name,
        name: service.applicationName || service.name,
        type: service.serviceType,
        port: service.serverPort,
        x: centerX + (idx - infraServices.length / 2) * horizontalSpacing,
        y: yOffset,
        width: nodeWidth,
        height: nodeHeight,
        commMethods
      });
    });
    yOffset += verticalSpacing;

    // Gateway in the middle
    gatewayServices.forEach((service, idx) => {
      const commMethods = getServiceCommMethods(service);
      newNodes.push({
        id: service.name,
        name: service.applicationName || service.name,
        type: service.serviceType,
        port: service.serverPort,
        x: centerX + (idx - gatewayServices.length / 2) * horizontalSpacing,
        y: yOffset,
        width: nodeWidth,
        height: nodeHeight,
        commMethods
      });
    });
    if (gatewayServices.length > 0) yOffset += verticalSpacing;

    // Business services at the bottom in a grid
    const cols = Math.ceil(Math.sqrt(businessServices.length));
    businessServices.forEach((service, idx) => {
      const row = Math.floor(idx / cols);
      const col = idx % cols;
      const commMethods = getServiceCommMethods(service);
      newNodes.push({
        id: service.name,
        name: service.applicationName || service.name,
        type: service.serviceType,
        port: service.serverPort,
        x: centerX + (col - cols / 2) * horizontalSpacing + horizontalSpacing / 2,
        y: yOffset + row * verticalSpacing,
        width: nodeWidth,
        height: nodeHeight,
        commMethods
      });
    });

    setNodes(newNodes);

    // Create edges from communications
    const newEdges: CommunicationEdge[] = communications
      .filter(comm => comm.sourceService && (comm.targetService || comm.messageChannel))
      .map(comm => ({
        id: comm.id,
        source: comm.sourceService,
        target: comm.targetService || comm.messageChannel || '',
        type: comm.communicationType,
        label: getEdgeLabel(comm),
        isLoadBalanced: comm.isLoadBalanced,
        isAsync: comm.isAsync
      }));

    setEdges(newEdges);
  }, [services, communications]);

  const getServiceCommMethods = (service: MicroserviceInfo): string[] => {
    const methods: string[] = [];
    if (service.hasFeignClients) methods.push('Feign');
    if (service.hasRestTemplate) methods.push('REST');
    if (service.hasWebClient) methods.push('WebClient');
    if (service.hasKafka) methods.push('Kafka');
    if (service.hasRabbitmq) methods.push('RabbitMQ');
    if (service.hasGrpc) methods.push('gRPC');
    return methods;
  };

  const getEdgeLabel = (comm: CommunicationInfo): string => {
    if (comm.feignClientName) return `Feign: ${comm.feignClientName}`;
    if (comm.messageChannel) return comm.messageChannel;
    if (comm.httpMethod) return comm.httpMethod;
    return comm.communicationType.replace('_', ' ');
  };

  // Draw canvas
  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Apply transformations
    ctx.save();
    ctx.translate(offset.x, offset.y);
    ctx.scale(scale, scale);

    // Draw edges first (behind nodes)
    edges.forEach(edge => {
      const sourceNode = nodes.find(n => n.id === edge.source);
      const targetNode = nodes.find(n => n.id === edge.target);
      
      if (sourceNode && targetNode) {
        const isHighlighted = hoveredEdge === edge.id || 
          selectedNode === edge.source || 
          selectedNode === edge.target;

        drawEdge(ctx, sourceNode, targetNode, edge, isHighlighted);
      }
    });

    // Draw nodes
    nodes.forEach(node => {
      const isSelected = selectedNode === node.id;
      const isConnected = selectedNode ? edges.some(e => 
        (e.source === selectedNode && e.target === node.id) ||
        (e.target === selectedNode && e.source === node.id)
      ) : null;
      drawNode(ctx, node, isSelected, isConnected);
    });

    ctx.restore();
  }, [nodes, edges, scale, offset, selectedNode, hoveredEdge]);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    canvas.width = container.clientWidth;
    canvas.height = 600;
    draw();
  }, [draw]);

  const drawNode = (
    ctx: CanvasRenderingContext2D, 
    node: ServiceNode, 
    isSelected: boolean,
    isConnected: boolean | null
  ) => {
    const { x, y, width, height, name, type, port, commMethods } = node;
    const color = serviceTypeColors[type] || '#64748b';

    // Shadow
    ctx.shadowColor = 'rgba(0, 0, 0, 0.1)';
    ctx.shadowBlur = isSelected ? 15 : 8;
    ctx.shadowOffsetY = 2;

    // Background
    ctx.fillStyle = isSelected ? `${color}30` : (isConnected ? `${color}15` : '#ffffff');
    ctx.beginPath();
    ctx.roundRect(x - width / 2, y - height / 2, width, height, 12);
    ctx.fill();

    // Border
    ctx.shadowBlur = 0;
    ctx.strokeStyle = isSelected ? color : (isConnected ? `${color}80` : '#e2e8f0');
    ctx.lineWidth = isSelected ? 3 : 2;
    ctx.stroke();

    // Type badge
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.roundRect(x - width / 2, y - height / 2, width, 24, [12, 12, 0, 0]);
    ctx.fill();

    // Type text
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 10px Inter, system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(formatType(type), x, y - height / 2 + 16);

    // Service name
    ctx.fillStyle = '#1e293b';
    ctx.font = 'bold 13px Inter, system-ui, sans-serif';
    ctx.fillText(truncateText(name, 20), x, y);

    // Port
    ctx.fillStyle = '#64748b';
    ctx.font = '11px monospace';
    ctx.fillText(`:${port}`, x, y + 18);

    // Communication methods badges
    if (commMethods.length > 0) {
      const badgeY = y + height / 2 - 18;
      const badgeWidth = 45;
      const startX = x - (commMethods.length * badgeWidth) / 2 + badgeWidth / 2;
      
      commMethods.slice(0, 3).forEach((method, idx) => {
        const badgeX = startX + idx * badgeWidth;
        ctx.fillStyle = '#f1f5f9';
        ctx.beginPath();
        ctx.roundRect(badgeX - 20, badgeY - 8, 40, 16, 4);
        ctx.fill();
        
        ctx.fillStyle = '#475569';
        ctx.font = '9px Inter, system-ui, sans-serif';
        ctx.fillText(method, badgeX, badgeY + 3);
      });
    }
  };

  const drawEdge = (
    ctx: CanvasRenderingContext2D,
    source: ServiceNode,
    target: ServiceNode,
    edge: CommunicationEdge,
    isHighlighted: boolean
  ) => {
    const color = commTypeColors[edge.type] || '#94a3b8';
    
    // Calculate edge points
    const startX = source.x;
    const startY = source.y + source.height / 2;
    const endX = target.x;
    const endY = target.y - target.height / 2;

    // Draw line
    ctx.beginPath();
    ctx.strokeStyle = isHighlighted ? color : `${color}60`;
    ctx.lineWidth = isHighlighted ? 3 : 2;
    
    if (edge.isAsync) {
      // Dashed line for async
      ctx.setLineDash([8, 4]);
    } else {
      ctx.setLineDash([]);
    }

    // Curved line
    const midY = (startY + endY) / 2;
    ctx.moveTo(startX, startY);
    ctx.bezierCurveTo(startX, midY, endX, midY, endX, endY);
    ctx.stroke();
    ctx.setLineDash([]);

    // Arrow head
    const angle = Math.atan2(endY - midY, endX - endX);
    const arrowSize = 8;
    ctx.fillStyle = isHighlighted ? color : `${color}60`;
    ctx.beginPath();
    ctx.moveTo(endX, endY);
    ctx.lineTo(endX - arrowSize * Math.cos(angle - Math.PI / 6), endY - arrowSize * Math.sin(angle - Math.PI / 6));
    ctx.lineTo(endX - arrowSize * Math.cos(angle + Math.PI / 6), endY - arrowSize * Math.sin(angle + Math.PI / 6));
    ctx.closePath();
    ctx.fill();

    // Label
    if (isHighlighted && edge.label) {
      const labelX = (startX + endX) / 2;
      const labelY = midY;
      
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.roundRect(labelX - 50, labelY - 10, 100, 20, 4);
      ctx.fill();
      ctx.strokeStyle = color;
      ctx.lineWidth = 1;
      ctx.stroke();
      
      ctx.fillStyle = '#1e293b';
      ctx.font = '10px Inter, system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(truncateText(edge.label, 15), labelX, labelY + 4);
    }

    // Load balanced indicator
    if (edge.isLoadBalanced && isHighlighted) {
      ctx.fillStyle = '#6366f1';
      ctx.font = '9px Inter, system-ui, sans-serif';
      ctx.fillText('⚖️ LB', (startX + endX) / 2, midY - 15);
    }
  };

  // Mouse handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = (e.clientX - rect.left - offset.x) / scale;
    const y = (e.clientY - rect.top - offset.y) / scale;

    const clickedNode = nodes.find(n => 
      x >= n.x - n.width / 2 && x <= n.x + n.width / 2 &&
      y >= n.y - n.height / 2 && y <= n.y + n.height / 2
    );

    if (clickedNode) {
      setDragging(clickedNode.id);
      setSelectedNode(clickedNode.id);
    } else {
      setPanning(true);
      setSelectedNode(null);
    }
    setLastMouse({ x: e.clientX, y: e.clientY });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const dx = e.clientX - lastMouse.x;
    const dy = e.clientY - lastMouse.y;

    if (dragging) {
      setNodes(nodes.map(n => 
        n.id === dragging 
          ? { ...n, x: n.x + dx / scale, y: n.y + dy / scale }
          : n
      ));
    } else if (panning) {
      setOffset({ x: offset.x + dx, y: offset.y + dy });
    }

    setLastMouse({ x: e.clientX, y: e.clientY });
  };

  const handleMouseUp = () => {
    setDragging(null);
    setPanning(false);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setScale(Math.max(0.3, Math.min(2, scale * delta)));
  };

  const resetView = () => {
    setScale(1);
    setOffset({ x: 0, y: 0 });
    setSelectedNode(null);
  };

  const formatType = (type: string): string => {
    const names: Record<string, string> = {
      API_GATEWAY: 'GATEWAY',
      DISCOVERY_SERVER: 'EUREKA',
      CONFIG_SERVER: 'CONFIG',
      BUSINESS_SERVICE: 'SERVICE',
      MESSAGING_SERVICE: 'MESSAGING',
      BATCH_SERVICE: 'BATCH',
      SCHEDULED_SERVICE: 'SCHEDULER',
      ADMIN_SERVICE: 'ADMIN',
    };
    return names[type] || type;
  };

  const truncateText = (text: string, maxLength: number): string => {
    return text.length > maxLength ? text.substring(0, maxLength - 2) + '..' : text;
  };

  return (
    <Card className="ms-architecture-diagram">
      <div className="ms-diagram-header">
        <h3>Microservices Architecture</h3>
        <div className="ms-diagram-controls">
          <button onClick={() => setScale(s => Math.min(2, s * 1.1))}>+</button>
          <span>{Math.round(scale * 100)}%</span>
          <button onClick={() => setScale(s => Math.max(0.3, s * 0.9))}>-</button>
          <button onClick={resetView}>Reset</button>
        </div>
      </div>

      <div className="ms-diagram-legend">
        <div className="ms-legend-section">
          <span className="ms-legend-title">Service Types:</span>
          {Object.entries(serviceTypeColors).slice(0, 5).map(([type, color]) => (
            <span key={type} className="ms-legend-item">
              <span className="ms-legend-dot" style={{ backgroundColor: color }} />
              {formatType(type)}
            </span>
          ))}
        </div>
        <div className="ms-legend-section">
          <span className="ms-legend-title">Communication:</span>
          <span className="ms-legend-item">
            <span className="ms-legend-line solid" /> Sync
          </span>
          <span className="ms-legend-item">
            <span className="ms-legend-line dashed" /> Async
          </span>
        </div>
      </div>

      <div 
        ref={containerRef} 
        className="ms-diagram-container"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onWheel={handleWheel}
      >
        <canvas ref={canvasRef} />
      </div>

      {selectedNode && (
        <div className="ms-diagram-info">
          <strong>Selected: {selectedNode}</strong>
          <span>
            Connections: {edges.filter(e => e.source === selectedNode || e.target === selectedNode).length}
          </span>
        </div>
      )}
    </Card>
  );
}

export default MicroservicesArchitectureDiagram;
