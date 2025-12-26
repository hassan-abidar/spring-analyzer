import { useState, useCallback } from 'react';
import { analysisService } from '../services/analysis.service';
import { AnalysisResult } from '../types/analysis.types';

export function useAnalysis() {
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const analyze = useCallback(async (projectId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await analysisService.analyzeSync(projectId);
      setResult(data);
      return data;
    } catch (err: any) {
      const message = err.response?.data?.message || 'Analysis failed';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchResult = useCallback(async (projectId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await analysisService.getResult(projectId);
      setResult(data);
      return data;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch results');
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
    analyze,
    fetchResult,
    clearResult
  };
}
