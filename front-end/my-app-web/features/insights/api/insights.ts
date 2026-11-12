import { apiJson, apiJsonBody, apiQuery, apiResponse } from '@/lib/api/client';

export type InsightEvidence = { source: string; date: string; metric: string; value: number };
export type InsightGeneratedBy = 'RULE' | 'STATISTICAL' | 'AI_ASSISTED';
export type Insight = {
  id: string; type: string; from: string; to: string; timezone: string;
  direction: string; strength: number; sampleSize: number;
  algorithmVersion: string; generatedBy: InsightGeneratedBy; narrative: string; evidence: InsightEvidence[];
};
export type InsightPage = { items: Insight[]; nextCursor: string | null };

export function listInsights(params: { from?: string; to?: string; cursor?: string; limit?: number } = {}): Promise<InsightPage> {
  return apiJson(`insights${apiQuery(params)}`);
}

/** Recomputes the last-30-day pattern insights on the server (rate limited). */
export function refreshInsights(): Promise<InsightPage> {
  return apiJson('insights:refresh', { method: 'POST' });
}

export async function hideInsight(id: string): Promise<void> {
  await apiResponse(`insights/${encodeURIComponent(id)}`, { method: 'PATCH', ...apiJsonBody({ status: 'HIDDEN' }) });
}

/** Mood the user picked most in the last 7 local days (null when none), shown by the Insight-page mascot. */
export type MascotMoodSummary = {
  mood: string | null; moodCount: number; entryCount: number; from: string; to: string;
  /** True when an entry in the window was screened high-risk: the mascot stays gentle. */
  calm: boolean;
};

export function getMascotMood(): Promise<MascotMoodSummary> {
  return apiJson('insights/mascot');
}
