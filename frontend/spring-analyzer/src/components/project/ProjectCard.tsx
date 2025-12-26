import React from 'react';
import { Project } from '../../types/project.types';
import './ProjectCard.css';

interface ProjectCardProps {
  project: Project;
  onDelete?: (id: number) => void;
  onClick?: (id: number) => void;
}

export function ProjectCard({ project, onDelete, onClick }: ProjectCardProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'status-completed';
      case 'ANALYZING': return 'status-analyzing';
      case 'FAILED': return 'status-failed';
      default: return 'status-uploaded';
    }
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onDelete && window.confirm('Delete this project?')) {
      onDelete(project.id);
    }
  };

  return (
    <div className="project-card" onClick={() => onClick?.(project.id)}>
      <div className="project-card-header">
        <h3 className="project-name">{project.name}</h3>
        <span className={`project-status ${getStatusColor(project.status)}`}>
          {project.status}
        </span>
      </div>
      
      {project.description && (
        <p className="project-description">{project.description}</p>
      )}
      
      <div className="project-meta">
        <span className="meta-item">📄 {project.originalFilename}</span>
        <span className="meta-item">💾 {formatSize(project.fileSize)}</span>
      </div>
      
      <div className="project-card-footer">
        <span className="project-date">{formatDate(project.createdAt)}</span>
        {onDelete && (
          <button className="delete-btn" onClick={handleDelete}>
            🗑️ Delete
          </button>
        )}
      </div>
    </div>
  );
}
