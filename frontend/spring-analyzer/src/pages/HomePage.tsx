import { useHealth } from '../hooks';
import { Card, Button } from '../components';
import './HomePage.css';

function HomePage() {
  const { health, loading, error, refetch } = useHealth();

  return (
    <div className="home-page">
      <section className="hero">
        <h1>Spring Project Analyzer</h1>
        <p>Upload your Spring Boot project and get detailed insights about its structure, entities, relationships, and REST endpoints.</p>
      </section>

      <section className="features">
        <Card title="📦 Project Scanner" className="feature-card">
          <p>Upload a ZIP file of your Spring Boot project and we'll analyze its structure.</p>
        </Card>
        
        <Card title="🔍 Entity Detection" className="feature-card">
          <p>Automatically detect @Entity, @Repository, @Service, and @Controller annotations.</p>
        </Card>
        
        <Card title="🔗 Relationship Mapping" className="feature-card">
          <p>Visualize entity relationships including @OneToMany, @ManyToOne, and more.</p>
        </Card>
        
        <Card title="🌐 REST API Analysis" className="feature-card">
          <p>List all REST endpoints with their HTTP methods and request/response models.</p>
        </Card>
      </section>

      <section className="status-section">
        <Card title="Server Status">
          {loading ? (
            <p>Checking server status...</p>
          ) : error ? (
            <div className="status-error">
              <p>⚠️ {error}</p>
              <Button onClick={refetch} variant="outline" size="sm">
                Retry
              </Button>
            </div>
          ) : health ? (
            <div className="status-info">
              <div className="status-row">
                <span className="label">Status:</span>
                <span className={`value ${health.status === 'UP' ? 'text-success' : 'text-error'}`}>
                  {health.status}
                </span>
              </div>
              <div className="status-row">
                <span className="label">Application:</span>
                <span className="value">{health.application}</span>
              </div>
              <div className="status-row">
                <span className="label">Version:</span>
                <span className="value">{health.version}</span>
              </div>
            </div>
          ) : null}
        </Card>
      </section>
    </div>
  );
}

export default HomePage;
