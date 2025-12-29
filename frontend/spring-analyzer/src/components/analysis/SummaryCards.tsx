import { AnalysisSummary } from '../../types/analysis.types';
import './SummaryCards.css';

interface SummaryCardsProps {
  summary: AnalysisSummary;
}

export function SummaryCards({ summary }: SummaryCardsProps) {
  const cards = [
    { label: 'Total Classes', value: summary.totalClasses, icon: '📦', color: '#3b82f6' },
    { label: 'Controllers', value: summary.controllers, icon: '🎮', color: '#8b5cf6' },
    { label: 'Services', value: summary.services, icon: '⚙️', color: '#10b981' },
    { label: 'Repositories', value: summary.repositories, icon: '🗄️', color: '#f59e0b' },
    { label: 'Entities', value: summary.entities, icon: '📋', color: '#ef4444' },
    { label: 'Endpoints', value: summary.endpoints, icon: '🔗', color: '#06b6d4' },
    { label: 'Dependencies', value: summary.dependencies, icon: '📚', color: '#ec4899' },
    { label: 'Relationships', value: summary.relationships || 0, icon: '🔀', color: '#14b8a6' },
  ];

  return (
    <div className="summary-cards">
      {cards.map((card) => (
        <div key={card.label} className="summary-card" style={{ borderColor: card.color }}>
          <span className="card-icon">{card.icon}</span>
          <div className="card-content">
            <span className="card-value">{card.value}</span>
            <span className="card-label">{card.label}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
