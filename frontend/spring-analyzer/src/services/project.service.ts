import api from './api';
import { ApiResponse } from '../types/api.types';
import { Project, ProjectUploadRequest } from '../types/project.types';

export const projectService = {
  upload: async (data: ProjectUploadRequest): Promise<Project> => {
    const formData = new FormData();
    formData.append('file', data.file);
    formData.append('name', data.name);
    if (data.description) {
      formData.append('description', data.description);
    }

    const response = await api.post<ApiResponse<Project>>('/projects', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data.data;
  },

  getAll: async (): Promise<Project[]> => {
    const response = await api.get<ApiResponse<Project[]>>('/projects');
    return response.data.data;
  },

  getById: async (id: number): Promise<Project> => {
    const response = await api.get<ApiResponse<Project>>(`/projects/${id}`);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/projects/${id}`);
  }
};
