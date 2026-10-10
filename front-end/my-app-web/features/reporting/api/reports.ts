import { apiJson, apiQuery } from '@/lib/api/client';

export type ReportEvidence = { source: string; date: string; metric: string; value: number };
export type Report = {
  id: string; type: string; from: string; to: string; timezone: string;
  version: number; status: string; sampleSize: number;
  metrics: Record<string, unknown>; narrative: string | null;
  evidence: ReportEvidence[]; createdAt: string; completedAt: string | null;
};
export type ReportPage = { items: Report[]; nextCursor: string | null };

export function listReports(params: { type?: string; cursor?: string; limit?: number } = {}): Promise<ReportPage> {
  return apiJson(`reports${apiQuery(params)}`);
}

export function getReport(reportId: string): Promise<Report> {
  return apiJson(`reports/${encodeURIComponent(reportId)}`);
}
