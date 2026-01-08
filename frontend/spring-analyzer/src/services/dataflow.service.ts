import api from './api';
import { ApiResponse } from '../types/api.types';
import { DataFlowResult } from '../types/dataflow.types';

export const dataFlowService = {
  getDataFlow: async (projectId: number): Promise<DataFlowResult> => {
    const response = await api.get<ApiResponse<DataFlowResult>>(`/projects/${projectId}/dataflow`);
    return response.data.data;
  }
};
