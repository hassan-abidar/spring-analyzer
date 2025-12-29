export interface DashboardData {
  projectId: number;
  projectName: string;
  status: string;
  analyzedAt?: string;
  metrics?: MetricsInfo;
  security: SecuritySummary;
  charts: ChartData;
}

export interface MetricsInfo {
  totalFiles: number;
  totalLines: number;
  codeLines: number;
  commentLines: number;
  blankLines: number;
  totalPackages: number;
  maxPackageDepth: number;
  avgMethodsPerClass: number;
  avgFieldsPerClass: number;
  maxMethodsInClass: number;
  maxFieldsInClass: number;
  packageStructure?: string;
}

export interface SecuritySummary {
  totalIssues: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
  info: number;
  byCategory: Record<string, number>;
  topIssues: SecurityIssueInfo[];
}

export interface SecurityIssueInfo {
  id: number;
  severity: string;
  category: string;
  title: string;
  description?: string;
  fileName?: string;
  lineNumber?: number;
  recommendation?: string;
}

export interface ChartData {
  classTypeDistribution: Record<string, number>;
  httpMethodDistribution: Record<string, number>;
  dependencyByScope: Record<string, number>;
  relationshipTypes: Record<string, number>;
  securityBySeverity: Record<string, number>;
  topPackages: PackageInfo[];
}

export interface PackageInfo {
  name: string;
  classCount: number;
}
