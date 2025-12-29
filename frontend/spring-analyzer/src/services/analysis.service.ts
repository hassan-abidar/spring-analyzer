import api from './api';
import { ApiResponse } from '../types/api.types';
import { AnalysisResult } from '../types/analysis.types';

export const analysisService = {
  startAnalysis: async (projectId: number): Promise<string> => {
    const response = await api.post<ApiResponse<string>>(`/projects/${projectId}/analysis`);
    return response.data.data;
  },

  analyzeSync: async (projectId: number): Promise<AnalysisResult> => {
    const response = await api.post<ApiResponse<AnalysisResult>>(`/projects/${projectId}/analysis/sync`);
    return response.data.data;
  },

  getResult: async (projectId: number): Promise<AnalysisResult> => {
    const response = await api.get<ApiResponse<AnalysisResult>>(`/projects/${projectId}/analysis`);
    return response.data.data;
  },

  exportJson: async (projectId: number, projectName: string): Promise<void> => {
    const response = await api.get(`/projects/${projectId}/analysis/export/json`, {
      responseType: 'blob'
    });
    downloadFile(response.data, `${projectName}-analysis.json`);
  },

  exportMarkdown: async (projectId: number, projectName: string): Promise<void> => {
    const response = await api.get(`/projects/${projectId}/analysis/export/markdown`, {
      responseType: 'blob'
    });
    downloadFile(response.data, `${projectName}-analysis.md`);
  }
};

function downloadFile(data: Blob, filename: string) {
  const url = window.URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
