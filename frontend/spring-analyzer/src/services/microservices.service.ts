import api from './api';
import { ApiResponse } from '../types/api.types';
import { MicroservicesResult } from '../types/microservices.types';

export const microservicesService = {
  getMicroservices: async (projectId: number): Promise<MicroservicesResult> => {
    const response = await api.get<ApiResponse<MicroservicesResult>>(`/projects/${projectId}/microservices`);
    return response.data.data;
  }
};
