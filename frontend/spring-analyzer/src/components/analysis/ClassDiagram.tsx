import { useRef, useEffect, useState } from 'react';
import { ClassInfo, RelationshipInfo } from '../../types/analysis.types';
import Card from '../common/Card';
import './ClassDiagram.css';

interface Props {
  classes: ClassInfo[];
  relationships: RelationshipInfo[];
}

interface Node {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
}

interface Edge {
  source: string;
  target: string;
  type: string;
}

const typeColors: Record<string, string> = {
  REST_CONTROLLER: '#8b5cf6',
  CONTROLLER: '#8b5cf6',
  SERVICE: '#10b981',
  REPOSITORY: '#f59e0b',
  ENTITY: '#ef4444',
  COMPONENT: '#06b6d4',
  CONFIGURATION: '#ec4899',
  INTERFACE: '#64748b',
  OTHER: '#94a3b8'
};

const edgeColors: Record<string, string> = {
  EXTENDS: '#9333ea',
  IMPLEMENTS: '#2563eb',
  INJECTS: '#16a34a',
  USES: '#64748b',
  ONE_TO_ONE: '#f59e0b',
  ONE_TO_MANY: '#f97316',
  MANY_TO_ONE: '#ef4444',
  MANY_TO_MANY: '#dc2626'
};

export function ClassDiagram({ classes, relationships }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState<string | null>(null);
  const [panning, setPanning] = useState(false);
  const [lastMouse, setLastMouse] = useState({ x: 0, y: 0 });

  const types = ['ALL', ...Array.from(new Set(classes.map(c => c.type)))];

  useEffect(() => {
    const relevantClasses = classes.filter(c => 
      typeFilter === 'ALL' || c.type === typeFilter
    );

    const classNames = new Set(relevantClasses.map(c => c.name));
    const relevantRels = relationships.filter(r => 
      classNames.has(r.sourceClass) && classNames.has(r.targetClass)
    );

    const nodeCount = relevantClasses.length;
    const cols = Math.ceil(Math.sqrt(nodeCount));

    const newNodes: Node[] = relevantClasses.map((cls, i) => ({
      id: cls.name,
      name: cls.name,
      type: cls.type,
      x: (i % cols) * 180 + 100,
      y: Math.floor(i / cols) * 120 + 80
    }));

    const newEdges: Edge[] = relevantRels.map(r => ({
      source: r.sourceClass,
      target: r.targetClass,
      type: r.type
    }));

    setNodes(newNodes);
    setEdges(newEdges);
    setOffset({ x: 0, y: 0 });
    setScale(1);
  }, [classes, relationships, typeFilter]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, rect.width, rect.height);
    ctx.save();
    ctx.translate(offset.x, offset.y);
    ctx.scale(scale, scale);

    edges.forEach(edge => {
      const source = nodes.find(n => n.id === edge.source);
      const target = nodes.find(n => n.id === edge.target);
      if (!source || !target) return;

      ctx.beginPath();
      ctx.strokeStyle = edgeColors[edge.type] || '#94a3b8';
      ctx.lineWidth = 2;
      
      if (edge.type === 'IMPLEMENTS') {
        ctx.setLineDash([5, 5]);
      } else {
        ctx.setLineDash([]);
      }

      ctx.moveTo(source.x, source.y);
      ctx.lineTo(target.x, target.y);
      ctx.stroke();

      const angle = Math.atan2(target.y - source.y, target.x - source.x);
      const arrowX = target.x - 40 * Math.cos(angle);
      const arrowY = target.y - 40 * Math.sin(angle);

      ctx.beginPath();
      ctx.setLineDash([]);
      ctx.moveTo(arrowX, arrowY);
      ctx.lineTo(arrowX - 10 * Math.cos(angle - 0.4), arrowY - 10 * Math.sin(angle - 0.4));
      ctx.lineTo(arrowX - 10 * Math.cos(angle + 0.4), arrowY - 10 * Math.sin(angle + 0.4));
      ctx.closePath();
      ctx.fillStyle = edgeColors[edge.type] || '#94a3b8';
      ctx.fill();
    });

    nodes.forEach(node => {
      const isSelected = selectedNode === node.id;
      const boxWidth = 140;
      const boxHeight = 50;

      ctx.fillStyle = isSelected ? '#e0e7ff' : '#fff';
      ctx.strokeStyle = typeColors[node.type] || '#94a3b8';
      ctx.lineWidth = isSelected ? 3 : 2;
      
      ctx.beginPath();
      ctx.roundRect(node.x - boxWidth/2, node.y - boxHeight/2, boxWidth, boxHeight, 8);
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = typeColors[node.type] || '#94a3b8';
      ctx.font = 'bold 10px system-ui';
      ctx.textAlign = 'center';
      ctx.fillText(node.type, node.x, node.y - 10);

      ctx.fillStyle = '#1e293b';
      ctx.font = '12px system-ui';
      ctx.fillText(node.name.length > 16 ? node.name.slice(0, 14) + '...' : node.name, node.x, node.y + 8);
    });

    ctx.restore();
  }, [nodes, edges, offset, scale, selectedNode]);

  const getNodeAt = (x: number, y: number): string | null => {
    const canvasX = (x - offset.x) / scale;
    const canvasY = (y - offset.y) / scale;
    
    for (const node of nodes) {
      if (Math.abs(canvasX - node.x) < 70 && Math.abs(canvasY - node.y) < 25) {
        return node.id;
      }
    }
    return null;
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const nodeId = getNodeAt(x, y);

    if (nodeId) {
      setDragging(nodeId);
      setSelectedNode(nodeId);
    } else {
      setPanning(true);
      setSelectedNode(null);
    }
    setLastMouse({ x: e.clientX, y: e.clientY });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const dx = e.clientX - lastMouse.x;
    const dy = e.clientY - lastMouse.y;
    setLastMouse({ x: e.clientX, y: e.clientY });

    if (dragging) {
      setNodes(prev => prev.map(n => 
        n.id === dragging ? { ...n, x: n.x + dx/scale, y: n.y + dy/scale } : n
      ));
    } else if (panning) {
      setOffset(prev => ({ x: prev.x + dx, y: prev.y + dy }));
    }
  };

  const handleMouseUp = () => {
    setDragging(null);
    setPanning(false);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setScale(prev => Math.max(0.3, Math.min(2, prev * delta)));
  };

  return (
    <Card>
      <div className="diagram-header">
        <h3>Class Diagram</h3>
        <div className="diagram-controls">
          <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
            {types.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          <button onClick={() => setScale(s => Math.min(2, s * 1.2))}>+</button>
          <button onClick={() => setScale(s => Math.max(0.3, s * 0.8))}>-</button>
          <button onClick={() => { setScale(1); setOffset({ x: 0, y: 0 }); }}>Reset</button>
        </div>
      </div>

      <div ref={containerRef} className="diagram-container">
        <canvas
          ref={canvasRef}
          className="diagram-canvas"
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          onWheel={handleWheel}
        />
      </div>

      <div className="diagram-legend">
        {Object.entries(typeColors).map(([type, color]) => (
          <span key={type} className="legend-item">
            <span className="legend-color" style={{ background: color }}></span>
            {type}
          </span>
        ))}
      </div>

      {selectedNode && (
        <div className="node-info">
          <strong>{selectedNode}</strong>
          <div className="node-relations">
            {edges.filter(e => e.source === selectedNode || e.target === selectedNode).map((e, i) => (
              <span key={i}>
                {e.source === selectedNode ? `→ ${e.target}` : `← ${e.source}`} ({e.type})
              </span>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}
