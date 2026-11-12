import { apiJson, apiJsonBody, apiQuery } from '@/lib/api/client';

export type ReportType = 'WEEKLY' | 'MONTHLY';
export type ReportEvidence = { source: string; date: string; metric: string; value: number };
export type Report = {
  id: string; type: string; from: string; to: string; timezone: string;
  version: number; status: string; sampleSize: number;
  metrics: Record<string, unknown>; narrative: string | null;
  /** RULE (template) or AI_ASSISTED (template reworded by AI from numbers only). */
  generatedBy: string | null;
  evidence: ReportEvidence[];
  /** Titles of highlighted entries, decrypted for the owner; keyed by entry ID. */
  entryTitles: Record<string, string> | null;
  createdAt: string; completedAt: string | null;
};
export type ReportPage = { items: Report[]; nextCursor: string | null };

export function listReports(params: { type?: string; cursor?: string; limit?: number } = {}): Promise<ReportPage> {
  return apiJson(`reports${apiQuery(params)}`);
}

export function getReport(reportId: string): Promise<Report> {
  return apiJson(`reports/${encodeURIComponent(reportId)}`);
}

/** Report for the most recent finished week/month; returns the existing one if already made. */
export function createReport(type: ReportType): Promise<Report> {
  return apiJson('reports', { method: 'POST', ...apiJsonBody({ type }) });
}

/** New version of a report with the latest data; the old version stays as it was. */
export function regenerateReport(reportId: string): Promise<Report> {
  return apiJson(`reports/${encodeURIComponent(reportId)}:regenerate`, { method: 'POST' });
}
