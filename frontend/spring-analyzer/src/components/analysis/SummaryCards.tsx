import { 
  Boxes, Gamepad2, Settings, Database, 
  ClipboardList, Link, Library, GitBranch 
} from 'lucide-react';
import { AnalysisSummary } from '../../types/analysis.types';
import './SummaryCards.css';

interface SummaryCardsProps {
  summary: AnalysisSummary;
}

const iconMap = {
  totalClasses: Boxes,
  controllers: Gamepad2,
  services: Settings,
  repositories: Database,
  entities: ClipboardList,
  endpoints: Link,
  dependencies: Library,
  relationships: GitBranch
};

export function SummaryCards({ summary }: SummaryCardsProps) {
  const cards = [
    { label: 'Total Classes', key: 'totalClasses', value: summary.totalClasses, color: '#3b82f6' },
    { label: 'Controllers', key: 'controllers', value: summary.controllers, color: '#8b5cf6' },
    { label: 'Services', key: 'services', value: summary.services, color: '#10b981' },
    { label: 'Repositories', key: 'repositories', value: summary.repositories, color: '#f59e0b' },
    { label: 'Entities', key: 'entities', value: summary.entities, color: '#ef4444' },
    { label: 'Endpoints', key: 'endpoints', value: summary.endpoints, color: '#06b6d4' },
    { label: 'Dependencies', key: 'dependencies', value: summary.dependencies, color: '#ec4899' },
    { label: 'Relationships', key: 'relationships', value: summary.relationships || 0, color: '#14b8a6' },
  ];

  return (
    <div className="summary-cards">
      {cards.map((card) => {
        const Icon = iconMap[card.key as keyof typeof iconMap];
        return (
          <div key={card.label} className="summary-card" style={{ '--accent-color': card.color } as React.CSSProperties}>
            <div className="card-icon-wrapper" style={{ background: `${card.color}15`, color: card.color }}>
              <Icon size={20} />
            </div>
            <div className="card-content">
              <span className="card-value">{card.value}</span>
              <span className="card-label">{card.label}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
