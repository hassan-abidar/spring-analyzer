import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Search, XCircle, Loader2 } from 'lucide-react';
import { useAnalysis, useMicroservices, useDataFlow } from '../hooks';
import { Button } from '../components';
import { SummaryCards, ClassList, EndpointList, DependencyList, DataFlowVisualization } from '../components/analysis';
import RelationshipList from '../components/analysis/RelationshipList';
import { ClassDiagram } from '../components/analysis/ClassDiagram';
import { MicroservicesPanel, MicroservicesArchitectureDiagram } from '../components/microservices';
import ModuleSelector from '../components/ModuleSelector';
import './AnalysisPage.css';

type TabType = 'classes' | 'endpoints' | 'dependencies' | 'relationships' | 'diagram' | 'microservices' | 'dataflow';

export function AnalysisPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { result, loading, error, analyze, fetchResult, exportJson, exportMarkdown } = useAnalysis();
  const { result: microservicesResult, loading: microservicesLoading, error: microservicesError, fetchMicroservices } = useMicroservices();
  const { result: dataFlowResult, loading: dataFlowLoading, error: dataFlowError, fetchDataFlow } = useDataFlow();
  const [activeTab, setActiveTab] = useState<TabType>('classes');
  const [selectedModule, setSelectedModule] = useState<string | null>(null);

  const projectId = Number(id);

  useEffect(() => {
    if (projectId) {
      fetchResult(projectId);
    }
  }, [projectId, fetchResult]);

  // Fetch microservices when analysis is complete and tab is selected
  useEffect(() => {
    if (projectId && result?.status === 'COMPLETED' && activeTab === 'microservices' && !microservicesResult && !microservicesLoading) {
      fetchMicroservices(projectId).catch(console.error);
    }
  }, [projectId, result?.status, activeTab, microservicesResult, microservicesLoading, fetchMicroservices]);

  // Fetch data flow when analysis is complete and tab is selected
  useEffect(() => {
    if (projectId && result?.status === 'COMPLETED' && activeTab === 'dataflow' && !dataFlowResult && !dataFlowLoading) {
      fetchDataFlow(projectId).catch(console.error);
    }
  }, [projectId, result?.status, activeTab, dataFlowResult, dataFlowLoading, fetchDataFlow]);

  // Filter data by selected module
  const filteredClasses = useMemo(() => {
    if (!result?.classes) return [];
    if (!selectedModule) return result.classes;
    return result.classes.filter(c => c.moduleName === selectedModule);
  }, [result?.classes, selectedModule]);

  const filteredEndpoints = useMemo(() => {
    if (!result?.endpoints) return [];
    if (!selectedModule) return result.endpoints;
    return result.endpoints.filter(e => e.moduleName === selectedModule);
  }, [result?.endpoints, selectedModule]);

  const filteredDependencies = useMemo(() => {
    if (!result?.dependencies) return [];
    if (!selectedModule) return result.dependencies;
    return result.dependencies.filter(d => d.moduleName === selectedModule);
  }, [result?.dependencies, selectedModule]);

  const handleAnalyze = async () => {
    if (projectId) {
      try {
        await analyze(projectId);
      } catch (err) {
        // Error is already set in the hook, just catch to prevent unhandled rejection
        console.error('Analysis failed:', err);
      }
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

          {result.isMultiModule && (
            <ModuleSelector
              modules={result.modules || []}
              selectedModule={selectedModule}
              onModuleChange={setSelectedModule}
              moduleSummaries={result.moduleSummaries}
            />
          )}

          <div className="tabs">
            <button 
              className={`tab ${activeTab === 'classes' ? 'active' : ''}`}
              onClick={() => setActiveTab('classes')}
            >
              Classes ({filteredClasses.length})
            </button>
            <button 
              className={`tab ${activeTab === 'endpoints' ? 'active' : ''}`}
              onClick={() => setActiveTab('endpoints')}
            >
              Endpoints ({filteredEndpoints.length})
            </button>
            <button 
              className={`tab ${activeTab === 'dependencies' ? 'active' : ''}`}
              onClick={() => setActiveTab('dependencies')}
            >
              Dependencies ({filteredDependencies.length})
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
            <button 
              className={`tab ${activeTab === 'microservices' ? 'active' : ''}`}
              onClick={() => setActiveTab('microservices')}
            >
              Microservices
            </button>
            <button 
              className={`tab ${activeTab === 'dataflow' ? 'active' : ''}`}
              onClick={() => setActiveTab('dataflow')}
            >
              Data Flow
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'classes' && <ClassList classes={filteredClasses} />}
            {activeTab === 'endpoints' && <EndpointList endpoints={filteredEndpoints} />}
            {activeTab === 'dependencies' && <DependencyList dependencies={filteredDependencies} />}
            {activeTab === 'relationships' && <RelationshipList relationships={result.relationships || []} />}
            {activeTab === 'diagram' && <ClassDiagram classes={filteredClasses} relationships={result.relationships || []} />}
            {activeTab === 'microservices' && (
              <div className="microservices-tab-content">
                {microservicesLoading && (
                  <div className="loading-state">
                    <Loader2 size={32} className="spin" />
                    <p>Loading microservices analysis...</p>
                  </div>
                )}
                {microservicesError && (
                  <div className="error-message">{microservicesError}</div>
                )}
                {microservicesResult && (
                  <>
                    <MicroservicesPanel data={microservicesResult} />
                    <div className="microservices-diagram-section">
                      <h3>Architecture Diagram</h3>
                      <MicroservicesArchitectureDiagram 
                        services={microservicesResult.services} 
                        communications={microservicesResult.communications} 
                      />
                    </div>
                  </>
                )}
                {!microservicesLoading && !microservicesError && !microservicesResult && (
                  <div className="empty-state">
                    <p>Click to load microservices analysis</p>
                    <Button onClick={() => fetchMicroservices(projectId)}>
                      Load Microservices
                    </Button>
                  </div>
                )}
              </div>
            )}
            {activeTab === 'dataflow' && (
              <div className="dataflow-tab-content">
                {dataFlowLoading && (
                  <div className="loading-state">
                    <Loader2 size={32} className="spin" />
                    <p>Analyzing data flow...</p>
                  </div>
                )}
                {dataFlowError && (
                  <div className="error-message">{dataFlowError}</div>
                )}
                {dataFlowResult && (
                  <DataFlowVisualization data={dataFlowResult} />
                )}
                {!dataFlowLoading && !dataFlowError && !dataFlowResult && (
                  <div className="empty-state">
                    <p>Click to load data flow analysis</p>
                    <Button onClick={() => fetchDataFlow(projectId)}>
                      Load Data Flow
                    </Button>
                  </div>
                )}
              </div>
            )}
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
