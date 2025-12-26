import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAnalysis } from '../hooks/useAnalysis';
import { Button } from '../components';
import { SummaryCards, ClassList, EndpointList, DependencyList } from '../components/analysis';
import './AnalysisPage.css';

export function AnalysisPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { result, loading, error, analyze, fetchResult } = useAnalysis();
  const [activeTab, setActiveTab] = useState<'classes' | 'endpoints' | 'dependencies'>('classes');

  const projectId = Number(id);

  useEffect(() => {
    if (projectId) {
      fetchResult(projectId);
    }
  }, [projectId, fetchResult]);

  const handleAnalyze = async () => {
    if (projectId) {
      await analyze(projectId);
    }
  };

  if (loading) {
    return (
      <div className="analysis-page">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Analyzing project...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="analysis-page">
      <div className="analysis-header">
        <div>
          <button className="back-btn" onClick={() => navigate('/projects')}>← Back</button>
          <h1>{result?.projectName || 'Project Analysis'}</h1>
          {result && <span className={`status status-${result.status.toLowerCase()}`}>{result.status}</span>}
        </div>
        <Button onClick={handleAnalyze} disabled={loading}>
          {loading ? 'Analyzing...' : result?.status === 'COMPLETED' ? 'Re-analyze' : 'Start Analysis'}
        </Button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {result?.status === 'COMPLETED' && result.summary && (
        <>
          <SummaryCards summary={result.summary} />

          <div className="tabs">
            <button 
              className={`tab ${activeTab === 'classes' ? 'active' : ''}`}
              onClick={() => setActiveTab('classes')}
            >
              Classes ({result.classes.length})
            </button>
            <button 
              className={`tab ${activeTab === 'endpoints' ? 'active' : ''}`}
              onClick={() => setActiveTab('endpoints')}
            >
              Endpoints ({result.endpoints.length})
            </button>
            <button 
              className={`tab ${activeTab === 'dependencies' ? 'active' : ''}`}
              onClick={() => setActiveTab('dependencies')}
            >
              Dependencies ({result.dependencies.length})
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'classes' && <ClassList classes={result.classes} />}
            {activeTab === 'endpoints' && <EndpointList endpoints={result.endpoints} />}
            {activeTab === 'dependencies' && <DependencyList dependencies={result.dependencies} />}
          </div>
        </>
      )}

      {result?.status === 'UPLOADED' && (
        <div className="empty-state">
          <div className="empty-icon">🔍</div>
          <h3>Ready to Analyze</h3>
          <p>Click the button above to start analyzing this project</p>
        </div>
      )}

      {result?.status === 'FAILED' && (
        <div className="error-state">
          <div className="error-icon">❌</div>
          <h3>Analysis Failed</h3>
          <p>Something went wrong. Please try again.</p>
        </div>
      )}
    </div>
  );
}
