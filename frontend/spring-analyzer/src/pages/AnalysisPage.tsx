import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Search, XCircle, Loader2 } from 'lucide-react';
import { useAnalysis } from '../hooks/useAnalysis';
import { Button } from '../components';
import { SummaryCards, ClassList, EndpointList, DependencyList } from '../components/analysis';
import RelationshipList from '../components/analysis/RelationshipList';
import { ClassDiagram } from '../components/analysis/ClassDiagram';
import './AnalysisPage.css';

type TabType = 'classes' | 'endpoints' | 'dependencies' | 'relationships' | 'diagram';

export function AnalysisPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { result, loading, error, analyze, fetchResult, exportJson, exportMarkdown } = useAnalysis();
  const [activeTab, setActiveTab] = useState<TabType>('classes');

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

  const handleExportJson = () => {
    if (projectId && result) {
      exportJson(projectId, result.projectName);
    }
  };

  const handleExportMarkdown = () => {
    if (projectId && result) {
      exportMarkdown(projectId, result.projectName);
    }
  };

  if (loading) {
    return (
      <div className="analysis-page">
        <div className="loading-state">
          <Loader2 size={32} className="spin" />
          <p>Analyzing project...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="analysis-page">
      <div className="analysis-header">
        <div>
          <button className="back-btn" onClick={() => navigate('/projects')}>
            <ArrowLeft size={16} />
            Back
          </button>
          <h1>{result?.projectName || 'Project Analysis'}</h1>
          {result && <span className={`status status-${result.status.toLowerCase()}`}>{result.status}</span>}
        </div>
        <div className="header-actions">
          {result?.status === 'COMPLETED' && (
            <div className="export-buttons">
              <button className="export-btn" onClick={handleExportJson}>Export JSON</button>
              <button className="export-btn" onClick={handleExportMarkdown}>Export MD</button>
            </div>
          )}
          <Button onClick={handleAnalyze} disabled={loading}>
            {loading ? 'Analyzing...' : result?.status === 'COMPLETED' ? 'Re-analyze' : 'Start Analysis'}
          </Button>
        </div>
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
            <button 
              className={`tab ${activeTab === 'relationships' ? 'active' : ''}`}
              onClick={() => setActiveTab('relationships')}
            >
              Relationships ({result.relationships?.length || 0})
            </button>
            <button 
              className={`tab ${activeTab === 'diagram' ? 'active' : ''}`}
              onClick={() => setActiveTab('diagram')}
            >
              Diagram
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'classes' && <ClassList classes={result.classes} />}
            {activeTab === 'endpoints' && <EndpointList endpoints={result.endpoints} />}
            {activeTab === 'dependencies' && <DependencyList dependencies={result.dependencies} />}
            {activeTab === 'relationships' && <RelationshipList relationships={result.relationships || []} />}
            {activeTab === 'diagram' && <ClassDiagram classes={result.classes} relationships={result.relationships || []} />}
          </div>
        </>
      )}

      {result?.status === 'UPLOADED' && (
        <div className="empty-state">
          <div className="empty-icon-wrapper">
            <Search size={32} />
          </div>
          <h3>Ready to Analyze</h3>
          <p>Click the button above to start analyzing this project</p>
        </div>
      )}

      {result?.status === 'FAILED' && (
        <div className="error-state">
          <div className="error-icon-wrapper">
            <XCircle size={32} />
          </div>
          <h3>Analysis Failed</h3>
          <p>Something went wrong. Please try again.</p>
        </div>
      )}
    </div>
  );
}
