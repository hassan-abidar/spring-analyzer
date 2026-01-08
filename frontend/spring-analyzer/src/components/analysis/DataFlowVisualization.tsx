import { useRef, useEffect, useState } from 'react';
import { DataFlowResult, DataFlowNode } from '../../types/dataflow.types';
import Card from '../common/Card';
import './DataFlowVisualization.css';

interface Props {
  data: DataFlowResult;
}

const LAYER_COLORS: Record<number, string> = {
  0: '#3b82f6', // API - Blue
  1: '#8b5cf6', // Service - Purple
  2: '#10b981', // Repository - Green
  3: '#f59e0b'  // Entity - Orange
};

const LAYER_NAMES = ['API Layer', 'Service Layer', 'Repository Layer', 'Entity Layer'];

export function DataFlowVisualization({ data }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [selectedNode, setSelectedNode] = useState<DataFlowNode | null>(null);

  // Layer-based layout
  const layerPositions = calculateLayerPositions(data.nodes);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    canvas.width = container.clientWidth;
    canvas.height = 700;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Draw background
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw layer separators and labels
    drawLayers(ctx, canvas.width, canvas.height);

    // Draw edges first
    data.edges.forEach(edge => {
      const sourceNode = data.nodes.find(n => n.id === edge.source);
      const targetNode = data.nodes.find(n => n.id === edge.target);
      if (sourceNode && targetNode) {
        const sourcePos = layerPositions[sourceNode.id];
        const targetPos = layerPositions[targetNode.id];
        if (sourcePos && targetPos) {
          drawEdge(ctx, sourcePos, targetPos, edge, sourceNode === selectedNode || targetNode === selectedNode);
        }
      }
    });

    // Draw nodes
    data.nodes.forEach(node => {
      const pos = layerPositions[node.id];
      if (pos) {
        const isSelected = selectedNode?.id === node.id;
        const isConnected = selectedNode && data.edges.some(e =>
          (e.source === selectedNode.id && e.target === node.id) ||
          (e.target === selectedNode.id && e.source === node.id)
        );
        drawNode(ctx, node, pos, isSelected, !!isConnected);
      }
    });
  }, [data.nodes, data.edges, selectedNode, layerPositions]);

  const handleCanvasClick = (e: React.MouseEvent) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Check which node was clicked
    for (const node of data.nodes) {
      const pos = layerPositions[node.id];
      if (pos) {
        const distance = Math.sqrt(Math.pow(x - pos.x, 2) + Math.pow(y - pos.y, 2));
        if (distance < 30) {
          setSelectedNode(selectedNode?.id === node.id ? null : node);
          return;
        }
      }
    }
  };

  return (
    <Card className="dataflow-visualization">
      <div className="dataflow-header">
        <h3>Data Flow Visualization</h3>
        <div className="dataflow-info">
          <span className="dataflow-stat">Nodes: {data.nodes.length}</span>
          <span className="dataflow-stat">Flows: {data.edges.length}</span>
          <span className="dataflow-stat">Paths: {data.flowPaths.length}</span>
        </div>
      </div>

      <div className="dataflow-container" ref={containerRef}>
        <canvas
          ref={canvasRef}
          onClick={handleCanvasClick}
          style={{ cursor: 'pointer' }}
        />
      </div>

      {selectedNode && (
        <div className="dataflow-node-details">
          <h4>{selectedNode.name}</h4>
          <div className="dataflow-details">
            <p><strong>Class:</strong> {selectedNode.className}</p>
            {selectedNode.methodName && <p><strong>Method:</strong> {selectedNode.methodName}</p>}
            <p><strong>Type:</strong> <span className="dataflow-type-badge" style={{ backgroundColor: LAYER_COLORS[selectedNode.layer] }}>{selectedNode.type}</span></p>
            {selectedNode.dtoNames.length > 0 && (
              <p><strong>DTOs:</strong> {selectedNode.dtoNames.join(', ')}</p>
            )}
            {selectedNode.entityNames.length > 0 && (
              <p><strong>Entities:</strong> {selectedNode.entityNames.join(', ')}</p>
            )}
          </div>
        </div>
      )}

      <div className="dataflow-legend">
        {data.layers.map(layer => (
          <div key={layer.layerName} className="dataflow-legend-item">
            <span className="dataflow-legend-color" style={{ backgroundColor: LAYER_COLORS[data.nodes.find(n => n.className === layer.nodeNames[0])?.layer || 0] }} />
            <span>{layer.layerName} ({layer.nodeCount})</span>
          </div>
        ))}
      </div>
    </Card>
  );
}

function calculateLayerPositions(nodes: DataFlowNode[]): Record<string, { x: number; y: number }> {
  const positions: Record<string, { x: number; y: number }> = {};
  const layerWidth = 150;
  const startX = 50;
  const verticalSpacing = 80;
  const canvasHeight = 700;

  // Group nodes by layer
  const layers: Record<number, DataFlowNode[]> = {};
  nodes.forEach(node => {
    if (!layers[node.layer]) {
      layers[node.layer] = [];
    }
    layers[node.layer].push(node);
  });

  // Position nodes
  Object.keys(layers).forEach(layerStr => {
    const layer = parseInt(layerStr);
    const layerNodes = layers[layer];
    const x = startX + layer * (layerWidth + 100);
    const totalHeight = layerNodes.length * verticalSpacing;
    const startY = (canvasHeight - totalHeight) / 2;

    layerNodes.forEach((node, idx) => {
      positions[node.id] = {
        x,
        y: startY + idx * verticalSpacing
      };
    });
  });

  return positions;
}

function drawNode(ctx: CanvasRenderingContext2D, node: DataFlowNode, pos: { x: number; y: number }, isSelected: boolean, isConnected: boolean) {
  const radius = isSelected ? 35 : 30;
  const color = LAYER_COLORS[node.layer] || '#64748b';

  // Shadow
  ctx.shadowColor = 'rgba(0, 0, 0, 0.2)';
  ctx.shadowBlur = isSelected ? 12 : 6;
  ctx.shadowOffsetY = 2;

  // Background circle
  ctx.fillStyle = isSelected ? `${color}30` : (isConnected ? `${color}20` : '#ffffff');
  ctx.beginPath();
  ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
  ctx.fill();

  // Border
  ctx.shadowBlur = 0;
  ctx.strokeStyle = isSelected ? color : (isConnected ? `${color}80` : '#e2e8f0');
  ctx.lineWidth = isSelected ? 3 : 2;
  ctx.stroke();

  // Icon/Letter
  ctx.fillStyle = color;
  ctx.font = 'bold 12px Arial';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(node.type.charAt(0), pos.x, pos.y);

  // Label
  ctx.fillStyle = '#1e293b';
  ctx.font = '11px Arial';
  ctx.fillText(node.name.substring(0, 15), pos.x, pos.y + radius + 15);
}

function drawEdge(ctx: CanvasRenderingContext2D, source: { x: number; y: number }, target: { x: number; y: number }, edge: any, isHighlighted: boolean) {
  const color = edge.isTransformation ? '#f59e0b' : '#cbd5e1';
  const lineWidth = isHighlighted ? 3 : 1.5;

  ctx.strokeStyle = isHighlighted ? color : `${color}80`;
  ctx.lineWidth = lineWidth;
  ctx.beginPath();

  // Curved line
  const controlX = (source.x + target.x) / 2;
  ctx.moveTo(source.x, source.y);
  ctx.bezierCurveTo(
    controlX, source.y,
    controlX, target.y,
    target.x, target.y
  );

  if (edge.type === 'TRANSFORMS') {
    ctx.setLineDash([5, 5]);
  }
  ctx.stroke();
  ctx.setLineDash([]);

  // Arrow
  const angle = Math.atan2(target.y - source.y, target.x - source.x);
  const arrowSize = 8;
  ctx.fillStyle = isHighlighted ? color : `${color}80`;
  ctx.beginPath();
  ctx.moveTo(target.x, target.y);
  ctx.lineTo(target.x - arrowSize * Math.cos(angle - Math.PI / 6), target.y - arrowSize * Math.sin(angle - Math.PI / 6));
  ctx.lineTo(target.x - arrowSize * Math.cos(angle + Math.PI / 6), target.y - arrowSize * Math.sin(angle + Math.PI / 6));
  ctx.closePath();
  ctx.fill();
}

function drawLayers(ctx: CanvasRenderingContext2D, _width: number, height: number) {
  const layerWidth = 150;
  const startX = 50;

  for (let i = 0; i < 4; i++) {
    const x = startX + i * (layerWidth + 100);

    // Vertical separator
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x - 20, 0);
    ctx.lineTo(x - 20, height);
    ctx.stroke();

    // Layer label
    ctx.fillStyle = '#64748b';
    ctx.font = 'bold 12px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(LAYER_NAMES[i], x, 25);
  }
}

export default DataFlowVisualization;
