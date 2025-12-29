import { Lightbulb, ShieldCheck } from 'lucide-react';
import { SecuritySummary } from '../../types/dashboard.types';
import Card from '../common/Card';
import './SecurityPanel.css';

interface Props {
  security: SecuritySummary;
}

const severityColors: Record<string, string> = {
  CRITICAL: '#dc2626',
  HIGH: '#ea580c',
  MEDIUM: '#f59e0b',
  LOW: '#3b82f6',
  INFO: '#64748b'
};

export function SecurityPanel({ security }: Props) {
  const score = calculateSecurityScore(security);
  const scoreColor = score >= 80 ? '#10b981' : score >= 60 ? '#f59e0b' : '#ef4444';

  return (
    <Card>
      <div className="security-header">
        <h3>Security Analysis</h3>
        <div className="security-score" style={{ borderColor: scoreColor }}>
          <span className="score-value" style={{ color: scoreColor }}>{score}</span>
          <span className="score-label">Score</span>
        </div>
      </div>

      <div className="severity-grid">
        {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'] as const).map(sev => {
          const count = security[sev.toLowerCase() as keyof typeof security] as number;
          return (
            <div key={sev} className="severity-item">
              <span className="severity-badge" style={{ background: severityColors[sev] }}>
                {count}
              </span>
              <span className="severity-name">{sev}</span>
            </div>
          );
        })}
      </div>

      {security.topIssues.length > 0 && (
        <div className="issues-list">
          <h4>Top Issues</h4>
          {security.topIssues.map(issue => (
            <div key={issue.id} className="issue-item">
              <span 
                className="issue-severity" 
                style={{ background: severityColors[issue.severity] }}
              >
                {issue.severity}
              </span>
              <div className="issue-content">
                <span className="issue-title">{issue.title}</span>
                {issue.fileName && (
                  <span className="issue-location">
                    {issue.fileName}{issue.lineNumber && `:${issue.lineNumber}`}
                  </span>
                )}
                {issue.recommendation && (
                  <span className="issue-recommendation">
                    <Lightbulb size={12} />
                    {issue.recommendation}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {security.totalIssues === 0 && (
        <div className="no-issues">
          <div className="check-icon">
            <ShieldCheck size={28} />
          </div>
          <p>No security issues detected!</p>
        </div>
      )}
    </Card>
  );
}

function calculateSecurityScore(security: SecuritySummary): number {
  const { critical, high, medium, low } = security;
  let score = 100;
  score -= critical * 20;
  score -= high * 10;
  score -= medium * 5;
  score -= low * 2;
  return Math.max(0, Math.min(100, score));
}
