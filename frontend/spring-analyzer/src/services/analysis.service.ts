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
  }
};
