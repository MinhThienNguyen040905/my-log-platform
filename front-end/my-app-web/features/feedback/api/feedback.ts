import { apiJson, apiJsonBody, apiQuery } from '@/lib/api/client';

export type Feedback = {
  id: string; userId: string; category: string; status: string;
  message: string; assignedTo: string | null;
  createdAt: string; updatedAt: string; resolvedAt: string | null; version: number;
};

export function submitFeedback(category: string, message: string): Promise<{ id: string }> {
  return apiJson('feedback', { method: 'POST', ...apiJsonBody({ category, message }) });
}
export function getFeedback(id: string): Promise<Feedback> {
  return apiJson(`feedback/${encodeURIComponent(id)}`);
}
export function listAdminFeedback(params: { status?: string; limit?: number } = {}): Promise<Feedback[]> {
  return apiJson(`admin/feedback${apiQuery({ ...params })}`);
}
export function getAdminFeedback(id: string): Promise<Feedback> {
  return apiJson(`admin/feedback/${encodeURIComponent(id)}`);
}
export function updateAdminFeedback(id: string, version: number, input: { status: string; assignedTo?: string | null }): Promise<Feedback> {
  return apiJson(`admin/feedback/${encodeURIComponent(id)}`, {
    method: 'PATCH', ...apiJsonBody(input, { 'If-Match': `"${version}"` }),
  });
}
