import { ReactNode } from 'react';
import Header from './Header';
import './Layout.css';

interface LayoutProps {
  children: ReactNode;
}

function Layout({ children }: LayoutProps) {
  return (
    <div className="layout">
      <Header />
      <main className="main-content">
        {children}
      </main>
      <footer className="footer">
        <p>Spring Analyzer © 2025</p>
      </footer>
    </div>
  );
}

export default Layout;
