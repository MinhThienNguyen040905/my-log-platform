import { apiRequest, jsonBody } from '@/lib/api/client';

export type DailyMetric = { date: string; source: string | null; journalCount: number;
  moodScore: number | null; stressScore: number | null; energyScore: number | null; sleepMinutes: number | null };
export type Dashboard = { range: string; timezone: string; from: string; to: string;
  timeline: DailyMetric[]; topEmotions: { code: string; count: number }[];
  topTopics: { code: string; count: number }[]; currentJournalStreak: number;
  longestJournalStreak: number; journalEntryCount: number; moodSampleSize: number };
export type Insight = { id: string; type: string; from: string; to: string; direction: string;
  strength: number | null; sampleSize: number; narrative: string | null };
export type Report = { id: string; type: string; from: string; to: string; status: string;
  sampleSize: number; narrative: string | null; metrics: Record<string, unknown> };
export type Page<T> = { items: T[]; nextCursor: string | null };
export type Habit = { id: string; title: string; unit: string; targetValue: number; status: string;
  streak: number; completedDates: string[] };
export type Goal = { id: string; category: string; title: string; description: string | null;
  status: string; version: number; habits: Habit[] };
export type AccountSession = { id: string; deviceName: string | null; createdAt: string;
  lastUsedAt: string | null; expiresAt: string };

export async function getDashboard(range: '7d' | '30d' | '90d' = '30d'): Promise<Dashboard> {
  return (await apiRequest(`/dashboard?range=${range}`)).json() as Promise<Dashboard>;
}
export async function listInsights(cursor?: string): Promise<Page<Insight>> {
  return (await apiRequest(`/insights?limit=20${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`)).json() as Promise<Page<Insight>>;
}
export async function listReports(cursor?: string): Promise<Page<Report>> {
  return (await apiRequest(`/reports?limit=20${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`)).json() as Promise<Page<Report>>;
}
export async function getReport(id: string): Promise<Report> {
  return (await apiRequest(`/reports/${encodeURIComponent(id)}`)).json() as Promise<Report>;
}
export async function listGoals(): Promise<Goal[]> {
  return (await apiRequest('/self-care/goals')).json() as Promise<Goal[]>;
}
export async function createGoal(category: string, title: string): Promise<Goal> {
  return (await apiRequest('/self-care/goals', { method: 'POST', ...jsonBody({ category, title }) })).json() as Promise<Goal>;
}
export async function createHabit(goalId: string, title: string): Promise<Habit> {
  return (await apiRequest(`/self-care/goals/${encodeURIComponent(goalId)}/habits`, { method: 'POST',
    ...jsonBody({ title, targetValue: 1, unit: 'times', frequencyType: 'DAILY' }) })).json() as Promise<Habit>;
}
export async function completeHabit(habitId: string, date: string) {
  await apiRequest(`/self-care/habits/${encodeURIComponent(habitId)}/completions/${date}`, { method: 'PUT',
    ...jsonBody({ value: 1 }) });
}
export async function undoHabit(habitId: string, date: string) {
  await apiRequest(`/self-care/habits/${encodeURIComponent(habitId)}/completions/${date}`, { method: 'DELETE' });
}
export async function listSessions(): Promise<AccountSession[]> {
  return (await apiRequest('/me/sessions')).json() as Promise<AccountSession[]>;
}
export async function revokeSession(id: string) {
  await apiRequest(`/me/sessions/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
export async function revokeOtherSessions() {
  await apiRequest('/me/sessions?exceptCurrent=true', { method: 'DELETE' });
}
