import { apiJson, apiQuery } from '@/lib/api/client';

export type DailyMetric = {
  date: string; source: string; journalCount: number;
  moodScore: number | null; stressScore: number | null;
  energyScore: number | null; sleepMinutes: number | null;
};
export type TopCode = { code: string; count: number; averageScore: number | null };
export type Dashboard = {
  range: string; timezone: string; from: string; to: string;
  timeline: DailyMetric[]; topEmotions: TopCode[]; topTopics: TopCode[];
  currentJournalStreak: number; longestJournalStreak: number;
  journalEntryCount: number; moodSampleSize: number;
};

export function getDashboard(range: string = '30d'): Promise<Dashboard> {
  return apiJson(`dashboard${apiQuery({ range })}`);
}
