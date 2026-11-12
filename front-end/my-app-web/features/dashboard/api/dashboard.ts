import { apiJson, apiQuery } from '@/lib/api/client';

export type DailyMetric = {
  date: string; source: string; journalCount: number;
  moodScore: number | null; stressScore: number | null;
  energyScore: number | null; sleepMinutes: number | null;
};
export type TopCode = { code: string; count: number; averageScore: number | null };
export type Dashboard = {
  range: string; timezone: string; from: string; to: string;
  timeline: DailyMetric[];
  /** Mood icons picked on entries (`happy`, `neutral`, `angry`, `sad`, `very-bad`), one per entry. */
  topMoods: TopCode[];
  /** Primary analysis tone per analyzed entry. */
  topEmotions: TopCode[]; topTopics: TopCode[];
  currentJournalStreak: number; longestJournalStreak: number;
  journalEntryCount: number; moodSampleSize: number;
};

export function getDashboard(range: string = '30d'): Promise<Dashboard> {
  return apiJson(`dashboard${apiQuery({ range })}`);
}
