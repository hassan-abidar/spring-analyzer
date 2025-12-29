import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import { ProjectsPage } from './pages/ProjectsPage';
import { AnalysisPage } from './pages/AnalysisPage';
import { DashboardPage } from './pages/DashboardPage';
import './App.css';

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:id/analysis" element={<AnalysisPage />} />
        <Route path="/projects/:id/dashboard" element={<DashboardPage />} />
      </Routes>
    </Layout>
  );
}

export default App;
