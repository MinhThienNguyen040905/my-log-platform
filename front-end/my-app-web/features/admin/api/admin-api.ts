import { apiJson, apiJsonBody, apiQuery, apiResponse } from '@/lib/api/client';
import type { AdminActionPayload, AdminDashboardData, AdminJobsResponse, AdminUserItem, AdminUsersResponse } from './types';

export const fetchAdminDashboard = () => apiJson<AdminDashboardData>('admin/dashboard');

export const fetchAdminUsers = (cursor?: string, limit = 20) =>
  apiJson<AdminUsersResponse>(`admin/users${apiQuery({ cursor, limit })}`);

export const fetchAdminUserMetadata = (userId: string) =>
  apiJson<AdminUserItem>(`admin/users/${encodeURIComponent(userId)}/metadata`);

export const suspendAdminUser = (userId: string, payload: AdminActionPayload) =>
  apiJson<AdminUserItem>(`admin/users/${encodeURIComponent(userId)}:suspend`, {
    method: 'POST', ...apiJsonBody(payload),
  });

export const restoreAdminUser = (userId: string, payload: AdminActionPayload) =>
  apiJson<AdminUserItem>(`admin/users/${encodeURIComponent(userId)}:restore`, {
    method: 'POST', ...apiJsonBody(payload),
  });

export const fetchAdminJobs = (cursor?: string, limit = 20) =>
  apiJson<AdminJobsResponse>(`admin/ai-jobs${apiQuery({ cursor, limit })}`);

export async function retryAdminJob(jobId: string): Promise<boolean> {
  const response = await apiResponse(`admin/ai-jobs/${encodeURIComponent(jobId)}:retry`, { method: 'POST' });
  return response.status === 202;
}
