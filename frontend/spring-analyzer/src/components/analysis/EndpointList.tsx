import { EndpointInfo } from '../../types/analysis.types';
import './EndpointList.css';

interface EndpointListProps {
  endpoints: EndpointInfo[];
}

export function EndpointList({ endpoints }: EndpointListProps) {
  const getMethodColor = (method: string) => {
    const colors: Record<string, string> = {
      GET: '#10b981',
      POST: '#3b82f6',
      PUT: '#f59e0b',
      DELETE: '#ef4444',
      PATCH: '#8b5cf6'
    };
    return colors[method] || '#64748b';
  };

  return (
    <div className="endpoint-list">
      <h3>Endpoints ({endpoints.length})</h3>
      <div className="endpoint-table">
        <div className="endpoint-header">
          <span>Method</span>
          <span>Path</span>
          <span>Handler</span>
          <span>Return</span>
        </div>
        {endpoints.map((ep) => (
          <div key={ep.id} className="endpoint-row">
            <span className="http-method" style={{ background: getMethodColor(ep.httpMethod) }}>
              {ep.httpMethod}
            </span>
            <span className="endpoint-path">{ep.path}</span>
            <span className="endpoint-handler">
              {ep.className && <span className="handler-class">{ep.className}.</span>}
              {ep.methodName}()
            </span>
            <span className="endpoint-return">{ep.returnType}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
