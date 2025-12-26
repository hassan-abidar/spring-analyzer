import { ClassInfo } from '../../types/analysis.types';
import './ClassList.css';

interface ClassListProps {
  classes: ClassInfo[];
}

export function ClassList({ classes }: ClassListProps) {
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
      <h3>Classes ({classes.length})</h3>
      <div className="class-grid">
        {classes.map((cls) => (
          <div key={cls.id} className="class-item">
            <div className="class-header">
              <span className="class-name">{cls.name}</span>
              <span className="class-type" style={{ background: getTypeColor(cls.type) }}>
                {cls.type}
              </span>
            </div>
            <div className="class-package">{cls.packageName}</div>
            <div className="class-stats">
              <span>📊 {cls.fieldCount} fields</span>
              <span>🔧 {cls.methodCount} methods</span>
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
