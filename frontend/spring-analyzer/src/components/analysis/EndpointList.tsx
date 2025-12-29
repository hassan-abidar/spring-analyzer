import { useState } from 'react';
import { EndpointInfo } from '../../types/analysis.types';
import './EndpointList.css';

interface EndpointListProps {
  endpoints: EndpointInfo[];
}

export function EndpointList({ endpoints }: EndpointListProps) {
  const [search, setSearch] = useState('');
  const [methodFilter, setMethodFilter] = useState('ALL');

  const methods = ['ALL', ...Array.from(new Set(endpoints.map(e => e.httpMethod)))];

  const filtered = endpoints.filter(ep => {
    const matchesSearch = search === '' || 
      ep.path.toLowerCase().includes(search.toLowerCase()) ||
      ep.methodName.toLowerCase().includes(search.toLowerCase()) ||
      (ep.className?.toLowerCase().includes(search.toLowerCase()) ?? false);
    const matchesMethod = methodFilter === 'ALL' || ep.httpMethod === methodFilter;
    return matchesSearch && matchesMethod;
  });

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
      <div className="list-header">
        <h3>Endpoints ({filtered.length}/{endpoints.length})</h3>
        <div className="filters">
          <input
            type="text"
            placeholder="Search endpoints..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="search-input"
          />
          <select value={methodFilter} onChange={e => setMethodFilter(e.target.value)} className="method-filter">
            {methods.map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>
      </div>
      <div className="endpoint-table">
        <div className="endpoint-header">
          <span>Method</span>
          <span>Path</span>
          <span>Handler</span>
          <span>Return</span>
        </div>
        {filtered.map((ep) => (
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
