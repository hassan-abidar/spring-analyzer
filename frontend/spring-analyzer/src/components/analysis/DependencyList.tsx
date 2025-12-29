import { useState } from 'react';
import { DependencyInfo } from '../../types/analysis.types';
import './DependencyList.css';

interface DependencyListProps {
  dependencies: DependencyInfo[];
}

export function DependencyList({ dependencies }: DependencyListProps) {
  const [search, setSearch] = useState('');
  const [scopeFilter, setScopeFilter] = useState('ALL');

  const scopes = ['ALL', ...Array.from(new Set(dependencies.map(d => d.scope)))];

  const filtered = dependencies.filter(dep => {
    const matchesSearch = search === '' || 
      dep.artifactId.toLowerCase().includes(search.toLowerCase()) ||
      dep.groupId.toLowerCase().includes(search.toLowerCase());
    const matchesScope = scopeFilter === 'ALL' || dep.scope === scopeFilter;
    return matchesSearch && matchesScope;
  });

  const isSpring = (dep: DependencyInfo) => 
    dep.groupId.includes('springframework') || dep.artifactId.includes('spring');

  return (
    <div className="dependency-list">
      <div className="list-header">
        <h3>Dependencies ({filtered.length}/{dependencies.length})</h3>
        <div className="filters">
          <input
            type="text"
            placeholder="Search dependencies..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="search-input"
          />
          <select value={scopeFilter} onChange={e => setScopeFilter(e.target.value)} className="scope-filter">
            {scopes.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      </div>
      <div className="dependency-grid">
        {filtered.map((dep) => (
          <div key={dep.id} className={`dependency-item ${isSpring(dep) ? 'spring' : ''}`}>
            <div className="dep-artifact">{dep.artifactId}</div>
            <div className="dep-group">{dep.groupId}</div>
            <div className="dep-meta">
              {dep.version && <span className="dep-version">{dep.version}</span>}
              <span className="dep-scope">{dep.scope}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
