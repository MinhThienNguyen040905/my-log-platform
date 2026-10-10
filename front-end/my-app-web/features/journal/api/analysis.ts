import { apiJson } from '@/lib/api/client';

export type JournalAnalysis = {
  entryId: string;
  contentVersion: number;
  status: string;
  analysisId: string | null;
  sentiment: string | null;
  sentimentScore: number | null;
  emotions: string[];
  topics: string[];
  reflection: string | null;
};

export function getJournalAnalysis(entryId: string): Promise<JournalAnalysis> {
  return apiJson(`journal-entries/${encodeURIComponent(entryId)}/analysis`);
}

export function retryJournalAnalysis(entryId: string): Promise<{ status: string }> {
  return apiJson(`journal-entries/${encodeURIComponent(entryId)}/analysis:retry`, { method: 'POST' });
}
