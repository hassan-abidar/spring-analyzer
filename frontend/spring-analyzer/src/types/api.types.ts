export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  error?: ErrorDetails;
}

export interface ErrorDetails {
  code: string;
  details: string;
  path: string;
}

export interface HealthResponse {
  status: string;
  application: string;
  version: string;
  timestamp: string;
}
