import { Link } from 'react-router-dom';
import { useHealth } from '../../hooks';
import './Header.css';

function Header() {
  const { health, loading } = useHealth();

  return (
    <header className="header">
      <div className="header-content">
        <Link to="/" className="logo">
          <span className="logo-icon">⚡</span>
          <span className="logo-text">Spring Analyzer</span>
        </Link>
        
        <nav className="nav">
          <Link to="/" className="nav-link">Home</Link>
          <Link to="/projects" className="nav-link">Projects</Link>
        </nav>

        <div className="status-indicator">
          {loading ? (
            <span className="status status-loading">Connecting...</span>
          ) : health?.status === 'UP' ? (
            <span className="status status-up">● Server Online</span>
          ) : (
            <span className="status status-down">● Server Offline</span>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
