import { FileCode, FileText, Code, Package } from 'lucide-react';
import { MetricsInfo } from '../../types/dashboard.types';
import Card from '../common/Card';
import './MetricsPanel.css';

interface Props {
  metrics: MetricsInfo;
}

const metricIcons = {
  files: FileCode,
  lines: FileText,
  code: Code,
  packages: Package
};

export function MetricsPanel({ metrics }: Props) {
  const codeRatio = metrics.totalLines > 0 
    ? Math.round((metrics.codeLines / metrics.totalLines) * 100) 
    : 0;

  const metricCards = [
    { key: 'files', icon: metricIcons.files, value: metrics.totalFiles, label: 'Java Files', color: '#3b82f6' },
    { key: 'lines', icon: metricIcons.lines, value: metrics.totalLines.toLocaleString(), label: 'Total Lines', color: '#8b5cf6' },
    { key: 'code', icon: metricIcons.code, value: metrics.codeLines.toLocaleString(), label: `Code Lines (${codeRatio}%)`, color: '#10b981' },
    { key: 'packages', icon: metricIcons.packages, value: metrics.totalPackages, label: 'Packages', color: '#f59e0b' },
  ];

  return (
    <Card>
      <h3>Code Metrics</h3>
      
      <div className="metrics-grid">
        {metricCards.map(({ key, icon: Icon, value, label, color }) => (
          <div key={key} className="metric-card">
            <div className="metric-icon" style={{ background: `${color}12`, color }}>
              <Icon size={18} />
            </div>
            <div className="metric-info">
              <span className="metric-value">{value}</span>
              <span className="metric-label">{label}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="metrics-details">
        <h4>Class Statistics</h4>
        <div className="detail-grid">
          <div className="detail-item">
            <span className="detail-label">Avg Methods/Class</span>
            <span className="detail-value">{metrics.avgMethodsPerClass}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Avg Fields/Class</span>
            <span className="detail-value">{metrics.avgFieldsPerClass}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Max Methods</span>
            <span className="detail-value">{metrics.maxMethodsInClass}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Max Fields</span>
            <span className="detail-value">{metrics.maxFieldsInClass}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Max Package Depth</span>
            <span className="detail-value">{metrics.maxPackageDepth}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Comment Lines</span>
            <span className="detail-value">{metrics.commentLines}</span>
          </div>
        </div>
      </div>

      {metrics.packageStructure && (
        <div className="package-tree">
          <h4>Package Structure</h4>
          <pre>{metrics.packageStructure}</pre>
        </div>
      )}
    </Card>
  );
}
