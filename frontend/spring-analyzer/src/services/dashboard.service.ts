import api from './api';
import { ApiResponse } from '../types/api.types';
import { DashboardData } from '../types/dashboard.types';

export const dashboardService = {
  getDashboard: async (projectId: number): Promise<DashboardData> => {
    const response = await api.get<ApiResponse<DashboardData>>(`/projects/${projectId}/dashboard`);
    return response.data.data;
  }
};
