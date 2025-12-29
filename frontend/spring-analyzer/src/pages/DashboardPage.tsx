import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Loader2, AlertCircle } from 'lucide-react';
import { useDashboard } from '../hooks/useDashboard';
import { MetricsPanel, SecurityPanel } from '../components/dashboard';
import { PieChart, BarChart } from '../components/charts';
import './DashboardPage.css';

export function DashboardPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data, loading, error, fetchDashboard } = useDashboard();

  const projectId = Number(id);

  useEffect(() => {
    if (projectId) {
      fetchDashboard(projectId);
    }
  }, [projectId, fetchDashboard]);

  if (loading) {
    return (
      <div className="dashboard-page">
        <div className="loading-state">
          <Loader2 size={32} className="spin" />
          <p>Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="dashboard-page">
        <div className="error-state">
          <div className="error-icon-wrapper">
            <AlertCircle size={28} />
          </div>
          <p>{error || 'Failed to load dashboard'}</p>
          <button className="back-button" onClick={() => navigate('/projects')}>Back to Projects</button>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <button className="back-btn" onClick={() => navigate('/projects')}>
            <ArrowLeft size={16} />
            Back
          </button>
          <h1>{data.projectName}</h1>
          <span className={`status status-${data.status.toLowerCase()}`}>{data.status}</span>
        </div>
        <button className="analysis-btn" onClick={() => navigate(`/projects/${id}/analysis`)}>
          View Full Analysis
          <ArrowRight size={16} />
        </button>
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-section metrics-section">
          {data.metrics && <MetricsPanel metrics={data.metrics} />}
        </div>

        <div className="dashboard-section security-section">
          <SecurityPanel security={data.security} />
        </div>

        <div className="dashboard-section charts-section">
          <h3>Distribution Charts</h3>
          <div className="charts-grid">
            <PieChart 
              data={data.charts.classTypeDistribution} 
              title="Class Types"
              colors={['#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#64748b']}
            />
            <PieChart 
              data={data.charts.httpMethodDistribution} 
              title="HTTP Methods"
              colors={['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6']}
            />
            <BarChart 
              data={data.charts.dependencyByScope} 
              title="Dependencies by Scope"
              color="#3b82f6"
            />
            <PieChart 
              data={data.charts.relationshipTypes} 
              title="Relationship Types"
            />
          </div>
        </div>

        {data.charts.topPackages.length > 0 && (
          <div className="dashboard-section packages-section">
            <h3>Top Packages by Class Count</h3>
            <div className="package-bars">
              {data.charts.topPackages.map((pkg, i) => {
                const max = data.charts.topPackages[0].classCount;
                const width = (pkg.classCount / max) * 100;
                return (
                  <div key={i} className="package-bar">
                    <span className="package-name">{pkg.name}</span>
                    <div className="bar-container">
                      <div className="bar-fill" style={{ width: `${width}%` }}>
                        <span className="bar-value">{pkg.classCount}</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
