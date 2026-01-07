import { useState, useCallback } from 'react';
import { microservicesService } from '../services';
import { MicroservicesResult } from '../types/microservices.types';

export function useMicroservices() {
  const [result, setResult] = useState<MicroservicesResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchMicroservices = useCallback(async (projectId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await microservicesService.getMicroservices(projectId);
      setResult(data);
      return data;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch microservices data';
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
    fetchMicroservices,
    clearResult
  };
}
