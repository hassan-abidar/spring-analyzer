import { useState, useEffect, useCallback } from 'react';
import { healthService } from '../services';
import { HealthResponse } from '../types';

interface UseHealthReturn {
  health: HealthResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useHealth(): UseHealthReturn {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await healthService.checkHealth();
      if (response.success) {
        setHealth(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError('Failed to connect to server');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHealth();
  }, [fetchHealth]);

  return { health, loading, error, refetch: fetchHealth };
}
