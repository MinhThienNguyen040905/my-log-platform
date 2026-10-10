import { apiJson, apiJsonBody, apiResponse } from '@/lib/api/client';

export type DeletionRequest = {
  id: string; status: string; requestedAt: string;
  scheduledFor: string | null; completedAt: string | null;
};
type Reauthentication = { email: string; password: string };

export function requestAccountDeletion(password: string): Promise<DeletionRequest> {
  return apiJson('account-deletion-requests', { method: 'POST', ...apiJsonBody({ password }) });
}
export function getAccountDeletionStatus(id: string, credentials: Reauthentication): Promise<DeletionRequest> {
  return apiJson(`account-deletion-requests/${encodeURIComponent(id)}:status`, { method: 'POST', ...apiJsonBody(credentials) });
}
export async function cancelAccountDeletion(id: string, credentials: Reauthentication): Promise<void> {
  await apiResponse(`account-deletion-requests/${encodeURIComponent(id)}:cancel`, { method: 'POST', ...apiJsonBody(credentials) });
}
