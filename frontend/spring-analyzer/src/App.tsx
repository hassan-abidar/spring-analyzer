import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import { ProjectsPage } from './pages/ProjectsPage';
import { AnalysisPage } from './pages/AnalysisPage';
import './App.css';

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:id/analysis" element={<AnalysisPage />} />
      </Routes>
    </Layout>
  );
}

export default App;
