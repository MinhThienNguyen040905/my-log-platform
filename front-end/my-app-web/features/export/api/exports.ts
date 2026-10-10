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
export function authorizeExportDownload(id: string, password: string): Promise<DownloadGrant> {
  return apiJson(`exports/${encodeURIComponent(id)}:authorize-download`, { method: 'POST', ...apiJsonBody({ password }) });
}
export async function downloadExport(id: string, grant: DownloadGrant): Promise<Response> {
  return apiResponse('export-download', { method: 'POST', ...apiJsonBody({ id, url: grant.url }) });
}
