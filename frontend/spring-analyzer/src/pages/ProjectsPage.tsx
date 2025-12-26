import { useState, useEffect } from 'react';
import { FileUpload } from '../components/upload';
import { ProjectCard } from '../components/project';
import { Button } from '../components';
import { useProjects } from '../hooks/useProjects';
import './ProjectsPage.css';

export function ProjectsPage() {
  const { projects, loading, error, fetchProjects, uploadProject, deleteProject } = useProjects();
  const [showUpload, setShowUpload] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [projectName, setProjectName] = useState('');
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  const handleUpload = async () => {
    if (!selectedFile || !projectName.trim()) return;

    setUploading(true);
    try {
      await uploadProject({
        file: selectedFile,
        name: projectName.trim(),
        description: description.trim() || undefined
      });
      resetForm();
    } catch {
      // Error handled by hook
    } finally {
      setUploading(false);
    }
  };

  const resetForm = () => {
    setShowUpload(false);
    setSelectedFile(null);
    setProjectName('');
    setDescription('');
  };

  return (
    <div className="projects-page">
      <div className="projects-header">
        <div>
          <h1>Projects</h1>
          <p>Upload and analyze your Spring Boot projects</p>
        </div>
        <Button onClick={() => setShowUpload(!showUpload)}>
          {showUpload ? 'Cancel' : '+ New Project'}
        </Button>
      </div>

      {showUpload && (
        <div className="upload-section">
          <FileUpload onFileSelect={setSelectedFile} disabled={uploading} />
          
          <div className="upload-form">
            <input
              type="text"
              placeholder="Project name *"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              disabled={uploading}
              className="form-input"
            />
            <textarea
              placeholder="Description (optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={uploading}
              className="form-input"
              rows={2}
            />
            <Button 
              onClick={handleUpload} 
              disabled={!selectedFile || !projectName.trim() || uploading}
            >
              {uploading ? 'Uploading...' : 'Upload Project'}
            </Button>
          </div>
        </div>
      )}

      {error && <div className="error-message">{error}</div>}

      {loading && !uploading ? (
        <div className="loading">Loading projects...</div>
      ) : projects.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📂</div>
          <h3>No projects yet</h3>
          <p>Upload your first Spring Boot project to get started</p>
        </div>
      ) : (
        <div className="projects-grid">
          {projects.map((project) => (
            <ProjectCard
              key={project.id}
              project={project}
              onDelete={deleteProject}
            />
          ))}
        </div>
      )}
    </div>
  );
}
