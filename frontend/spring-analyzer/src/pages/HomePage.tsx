import { FolderSearch, Scan, GitBranch, Globe, AlertTriangle, CheckCircle, XCircle, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useHealth } from '../hooks';
import { Button } from '../components';
import './HomePage.css';

function HomePage() {
  const { health, loading, error, refetch } = useHealth();
  const navigate = useNavigate();

  const features = [
    { icon: FolderSearch, title: 'Project Scanner', desc: 'Upload a ZIP file of your Spring Boot project and we\'ll analyze its structure.', color: '#3b82f6' },
    { icon: Scan, title: 'Entity Detection', desc: 'Automatically detect @Entity, @Repository, @Service, and @Controller annotations.', color: '#8b5cf6' },
    { icon: GitBranch, title: 'Relationship Mapping', desc: 'Visualize entity relationships including @OneToMany, @ManyToOne, and more.', color: '#10b981' },
    { icon: Globe, title: 'REST API Analysis', desc: 'List all REST endpoints with their HTTP methods and request/response models.', color: '#f59e0b' },
  ];

  return (
    <div className="home-page">
      <section className="hero">
        <div className="hero-badge">Spring Boot Analysis Tool</div>
        <h1>Understand your Spring project in seconds</h1>
        <p>Upload your Spring Boot project and get detailed insights about its structure, entities, relationships, and REST endpoints.</p>
        <button className="hero-cta" onClick={() => navigate('/projects')}>
          Get Started
          <ArrowRight size={18} />
        </button>
      </section>

      <section className="features">
        {features.map((f, i) => (
          <div key={i} className="feature-card">
            <div className="feature-icon" style={{ background: `${f.color}12`, color: f.color }}>
              <f.icon size={22} />
            </div>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </section>

      <section className="status-section">
        <div className="status-card">
          <h3>Server Status</h3>
          {loading ? (
            <p className="status-loading">Checking server status...</p>
          ) : error ? (
            <div className="status-error">
              <AlertTriangle size={18} />
              <p>{error}</p>
              <Button onClick={refetch} variant="outline" size="sm">Retry</Button>
            </div>
          ) : health ? (
            <div className="status-info">
              <div className="status-row">
                <span className="label">Status</span>
                <span className={`value ${health.status === 'UP' ? 'text-success' : 'text-error'}`}>
                  {health.status === 'UP' ? <CheckCircle size={14} /> : <XCircle size={14} />}
                  {health.status}
                </span>
              </div>
              <div className="status-row">
                <span className="label">Application</span>
                <span className="value">{health.application}</span>
              </div>
              <div className="status-row">
                <span className="label">Version</span>
                <span className="value">{health.version}</span>
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

export default HomePage;
