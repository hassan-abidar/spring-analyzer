import apiClient from './api';
import { ApiResponse, HealthResponse } from '../types';

export const healthService = {
  checkHealth: async (): Promise<ApiResponse<HealthResponse>> => {
    const response = await apiClient.get<ApiResponse<HealthResponse>>('/health');
    return response.data;
  },

  ping: async (): Promise<ApiResponse<string>> => {
    const response = await apiClient.get<ApiResponse<string>>('/health/ping');
    return response.data;
  },
};
