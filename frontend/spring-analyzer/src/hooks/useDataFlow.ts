import { useState, useCallback } from 'react';
import { dataFlowService } from '../services';
import { DataFlowResult } from '../types/dataflow.types';

export function useDataFlow() {
  const [result, setResult] = useState<DataFlowResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDataFlow = useCallback(async (projectId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await dataFlowService.getDataFlow(projectId);
      setResult(data);
      return data;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch data flow';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearResult = useCallback(() => {
    setResult(null);
    setError(null);
  }, []);

  return {
    result,
    loading,
    error,
    fetchDataFlow,
    clearResult
  };
}
