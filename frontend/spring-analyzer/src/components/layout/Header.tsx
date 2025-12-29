import { Link } from 'react-router-dom';
import { Zap, Home, FolderKanban, Circle, Loader2 } from 'lucide-react';
import { useHealth } from '../../hooks';
import './Header.css';

function Header() {
  const { health, loading } = useHealth();

  return (
    <header className="header">
      <div className="header-content">
        <Link to="/" className="logo">
          <div className="logo-icon-wrapper">
            <Zap size={20} />
          </div>
          <span className="logo-text">Spring Analyzer</span>
        </Link>
        
        <nav className="nav">
          <Link to="/" className="nav-link">
            <Home size={16} />
            <span>Home</span>
          </Link>
          <Link to="/projects" className="nav-link">
            <FolderKanban size={16} />
            <span>Projects</span>
          </Link>
        </nav>

        <div className="status-indicator">
          {loading ? (
            <span className="status status-loading">
              <Loader2 size={14} className="spin" />
              Connecting...
            </span>
          ) : health?.status === 'UP' ? (
            <span className="status status-up">
              <Circle size={8} fill="currentColor" />
              Online
            </span>
          ) : (
            <span className="status status-down">
              <Circle size={8} fill="currentColor" />
              Offline
            </span>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
