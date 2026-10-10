import { apiJson, apiQuery } from '@/lib/api/client';

export type InsightEvidence = { source: string; date: string; metric: string; value: number };
export type Insight = {
  id: string; type: string; from: string; to: string; timezone: string;
  direction: string; strength: number; sampleSize: number;
  algorithmVersion: string; narrative: string; evidence: InsightEvidence[];
};
export type InsightPage = { items: Insight[]; nextCursor: string | null };

export function listInsights(params: { from?: string; to?: string; cursor?: string; limit?: number } = {}): Promise<InsightPage> {
  return apiJson(`insights${apiQuery(params)}`);
}
