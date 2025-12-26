import { useState, useCallback } from 'react';
import { projectService } from '../services/project.service';
import { Project, ProjectUploadRequest } from '../types/project.types';

export function useProjects() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchProjects = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await projectService.getAll();
      setProjects(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  }, []);

  const uploadProject = useCallback(async (data: ProjectUploadRequest) => {
    setLoading(true);
    setError(null);
    try {
      const project = await projectService.upload(data);
      setProjects(prev => [project, ...prev]);
      return project;
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to upload project';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const deleteProject = useCallback(async (id: number) => {
    setLoading(true);
    setError(null);
    try {
      await projectService.delete(id);
      setProjects(prev => prev.filter(p => p.id !== id));
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete project');
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    projects,
    loading,
    error,
    fetchProjects,
    uploadProject,
    deleteProject
  };
}
