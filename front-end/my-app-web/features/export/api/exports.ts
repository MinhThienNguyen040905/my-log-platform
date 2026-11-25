import { apiJson, apiJsonBody, apiResponse } from '@/lib/api/client';

export type ExportJob = {
  id: string; format: string; status: string; createdAt: string;
  completedAt: string | null; expiresAt: string | null;
};
export type DownloadGrant = { url: string; expiresAt: string };

export function createExport(format: 'CSV' | 'PDF'): Promise<ExportJob> {
  return apiJson('exports', { method: 'POST', ...apiJsonBody({ format }) });
}
export function getExport(id: string): Promise<ExportJob> {
  return apiJson(`exports/${encodeURIComponent(id)}`);
}
/**
 * Confirms identity before the 60-second download grant: the password, or none for a Google account that signed in
 * within the last 10 minutes. Refusals are 403 with detail PASSWORD_INCORRECT or RECENT_LOGIN_REQUIRED, 429 after
 * too many wrong passwords.
 */
export function authorizeExportDownload(id: string, password: string | null): Promise<DownloadGrant> {
  return apiJson(`exports/${encodeURIComponent(id)}:authorize-download`, {
    method: 'POST', ...apiJsonBody(password ? { password } : {}),
  });
}
export async function downloadExport(id: string, grant: DownloadGrant): Promise<Response> {
  return apiResponse('export-download', { method: 'POST', ...apiJsonBody({ id, url: grant.url }) });
}
