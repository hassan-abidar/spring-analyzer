import { useState } from 'react';
import { Layers, Wrench, Search } from 'lucide-react';
import { ClassInfo } from '../../types/analysis.types';
import './ClassList.css';

interface ClassListProps {
  classes: ClassInfo[];
}

export function ClassList({ classes }: ClassListProps) {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const types = ['ALL', ...Array.from(new Set(classes.map(c => c.type)))];

  const filtered = classes.filter(cls => {
    const matchesSearch = search === '' || 
      cls.name.toLowerCase().includes(search.toLowerCase()) ||
      cls.packageName.toLowerCase().includes(search.toLowerCase());
    const matchesType = typeFilter === 'ALL' || cls.type === typeFilter;
    return matchesSearch && matchesType;
  });

  const getTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      REST_CONTROLLER: '#8b5cf6',
      CONTROLLER: '#8b5cf6',
      SERVICE: '#10b981',
      REPOSITORY: '#f59e0b',
      ENTITY: '#ef4444',
      COMPONENT: '#06b6d4',
      CONFIGURATION: '#ec4899',
      INTERFACE: '#64748b',
      ENUM: '#84cc16',
      OTHER: '#94a3b8'
    };
    return colors[type] || colors.OTHER;
  };

  return (
    <div className="class-list">
      <div className="list-header">
        <h3>Classes ({filtered.length}/{classes.length})</h3>
        <div className="filters">
          <div className="search-wrapper">
            <Search size={16} />
            <input
              type="text"
              placeholder="Search classes..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="search-input"
            />
          </div>
          <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)} className="type-filter">
            {types.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
      </div>
      <div className="class-grid">
        {filtered.map((cls) => (
          <div key={cls.id} className="class-item">
            <div className="class-header">
              <span className="class-name">{cls.name}</span>
              <span className="class-type" style={{ background: getTypeColor(cls.type) }}>
                {cls.type}
              </span>
            </div>
            <div className="class-package">{cls.packageName}</div>
            <div className="class-stats">
              <span><Layers size={13} /> {cls.fieldCount} fields</span>
              <span><Wrench size={13} /> {cls.methodCount} methods</span>
            </div>
            {cls.annotations.length > 0 && cls.annotations[0] && (
              <div className="class-annotations">
                {cls.annotations.slice(0, 3).map((a, i) => (
                  <span key={i} className="annotation">@{a}</span>
                ))}
                {cls.annotations.length > 3 && <span className="more">+{cls.annotations.length - 3}</span>}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
