export interface AdminDashboardData {
  suppressed: boolean;
  activeUsers: number | null;
  journalEntries: number | null;
  dailyCheckins: number | null;
  minimumCohortSize: number;
}

export interface AdminUserItem {
  id: string;
  status: 'ACTIVE' | 'SUSPENDED' | string;
  createdAt: string;
  lastLoginAt: string | null;
  version: number;
}

export interface AdminUsersResponse {
  items: AdminUserItem[];
  nextCursor: string | null;
}

export interface AdminJobItem {
  id: string;
  userId: string;
  type: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | string;
  attempt: number;
  errorCode: string | null;
  createdAt: string;
  availableAt: string;
}

export interface AdminJobsResponse {
  items: AdminJobItem[];
  nextCursor: string | null;
}

export interface AdminActionPayload {
  reasonCode: string;
}

