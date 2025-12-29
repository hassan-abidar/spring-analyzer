import { useState, useCallback } from 'react';
import { dashboardService } from '../services/dashboard.service';
import { DashboardData } from '../types/dashboard.types';

export function useDashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async (projectId: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await dashboardService.getDashboard(projectId);
      setData(result);
      return result;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  return { data, loading, error, fetchDashboard };
}
