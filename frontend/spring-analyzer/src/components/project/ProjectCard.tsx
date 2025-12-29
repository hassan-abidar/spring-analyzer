import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText, HardDrive, BarChart3, Trash2 } from 'lucide-react';
import { Project } from '../../types/project.types';
import './ProjectCard.css';

interface ProjectCardProps {
  project: Project;
  onDelete?: (id: number) => void;
  onClick?: (id: number) => void;
}

export function ProjectCard({ project, onDelete, onClick }: ProjectCardProps) {
  const navigate = useNavigate();
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
        <span className="meta-item">
          <FileText size={14} />
          {project.originalFilename}
        </span>
        <span className="meta-item">
          <HardDrive size={14} />
          {formatSize(project.fileSize)}
        </span>
      </div>
      
      <div className="project-card-footer">
        <span className="project-date">{formatDate(project.createdAt)}</span>
        <div className="footer-actions">
          {project.status === 'COMPLETED' && (
            <button 
              className="dashboard-btn" 
              onClick={(e) => { e.stopPropagation(); navigate(`/projects/${project.id}/dashboard`); }}
            >
              <BarChart3 size={15} />
              Dashboard
            </button>
          )}
          {onDelete && (
            <button className="delete-btn" onClick={handleDelete}>
              <Trash2 size={15} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
