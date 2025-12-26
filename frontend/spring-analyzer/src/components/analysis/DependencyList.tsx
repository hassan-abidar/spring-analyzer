import { DependencyInfo } from '../../types/analysis.types';
import './DependencyList.css';

interface DependencyListProps {
  dependencies: DependencyInfo[];
}

export function DependencyList({ dependencies }: DependencyListProps) {
  const isSpring = (dep: DependencyInfo) => 
    dep.groupId.includes('springframework') || dep.artifactId.includes('spring');

  return (
    <div className="dependency-list">
      <h3>Dependencies ({dependencies.length})</h3>
      <div className="dependency-grid">
        {dependencies.map((dep) => (
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
