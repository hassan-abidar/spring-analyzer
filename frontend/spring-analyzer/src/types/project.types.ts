export interface Project {
  id: number;
  name: string;
  description?: string;
  originalFilename: string;
  fileSize: number;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
  analyzedAt?: string;
}

export type ProjectStatus = 'UPLOADED' | 'ANALYZING' | 'COMPLETED' | 'FAILED';

export interface ProjectUploadRequest {
  file: File;
  name: string;
  description?: string;
}
