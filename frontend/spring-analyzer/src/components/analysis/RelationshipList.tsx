import { useState } from 'react';
import { RelationshipInfo } from '../../types/analysis.types';
import Card from '../common/Card';

interface Props {
  relationships: RelationshipInfo[];
}

const typeColors: Record<string, string> = {
  EXTENDS: '#9333ea',
  IMPLEMENTS: '#2563eb',
  INJECTS: '#16a34a',
  USES: '#64748b',
  ONE_TO_ONE: '#f59e0b',
  ONE_TO_MANY: '#f97316',
  MANY_TO_ONE: '#ef4444',
  MANY_TO_MANY: '#dc2626'
};

const RelationshipList = ({ relationships }: Props) => {
  const [filter, setFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const types = ['ALL', ...Array.from(new Set(relationships.map(r => r.type)))];

  const filtered = relationships.filter(r => {
    const matchesSearch = filter === '' || 
      r.sourceClass.toLowerCase().includes(filter.toLowerCase()) ||
      r.targetClass.toLowerCase().includes(filter.toLowerCase());
    const matchesType = typeFilter === 'ALL' || r.type === typeFilter;
    return matchesSearch && matchesType;
  });

  const grouped = filtered.reduce((acc, r) => {
    const key = r.sourceClass;
    if (!acc[key]) acc[key] = [];
    acc[key].push(r);
    return acc;
  }, {} as Record<string, RelationshipInfo[]>);

  return (
    <Card>
      <div style={{ marginBottom: '16px' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 600 }}>
          Class Relationships ({relationships.length})
        </h3>
        
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <input
            type="text"
            placeholder="Search classes..."
            value={filter}
            onChange={e => setFilter(e.target.value)}
            style={{
              padding: '8px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              flex: 1,
              minWidth: '200px'
            }}
          />
          <select
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value)}
            style={{
              padding: '8px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              background: 'white'
            }}
          >
            {types.map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
      </div>

      {Object.keys(grouped).length === 0 ? (
        <p style={{ color: '#6b7280', textAlign: 'center', padding: '20px' }}>
          No relationships found
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {Object.entries(grouped).map(([source, rels]) => (
            <div key={source} style={{ 
              border: '1px solid #e5e7eb', 
              borderRadius: '8px',
              overflow: 'hidden'
            }}>
              <div style={{ 
                background: '#f9fafb', 
                padding: '12px 16px',
                borderBottom: '1px solid #e5e7eb',
                fontWeight: 600
              }}>
                {source}
              </div>
              <div style={{ padding: '12px 16px' }}>
                {rels.map(r => (
                  <div key={r.id} style={{ 
                    display: 'flex', 
                    alignItems: 'center',
                    gap: '12px',
                    padding: '8px 0',
                    borderBottom: '1px solid #f3f4f6'
                  }}>
                    <span style={{
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 500,
                      color: 'white',
                      background: typeColors[r.type] || '#6b7280'
                    }}>
                      {r.type}
                    </span>
                    <span style={{ color: '#374151' }}>→</span>
                    <span style={{ fontWeight: 500 }}>{r.targetClass}</span>
                    {r.fieldName && (
                      <span style={{ color: '#6b7280', fontSize: '14px' }}>
                        ({r.fieldName})
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
};

export default RelationshipList;
