import { apiJson, apiJsonBody, apiResponse } from '@/lib/api/client';

export type MoodAssociation = { completedDays: number; otherDays: number; averageMoodDifference: number | null; interpretation: string | null };
export type Habit = {
  id: string; goalId: string; title: string; targetValue: number; unit: string;
  frequencyType: string; daysOfWeek: number[]; timezone: string; status: string;
  version: number; streak: number; completionCount: number;
  completedDates: string[]; moodAssociation: MoodAssociation | null;
};
export type Goal = {
  id: string; category: string; title: string; description: string | null;
  status: string; startDate: string | null; targetDate: string | null;
  version: number; createdAt: string; updatedAt: string; habits: Habit[];
};
export type CreateGoal = { category: string; title: string; description?: string | null; startDate?: string | null; targetDate?: string | null };
export type UpdateGoal = { title?: string; description?: string | null; status?: string; startDate?: string | null; targetDate?: string | null };
export type CreateHabit = { title: string; targetValue: number; unit: string; frequencyType: string; daysOfWeek?: number[] };

export function listGoals(): Promise<Goal[]> { return apiJson('self-care/goals'); }
export function createGoal(input: CreateGoal): Promise<Goal> {
  return apiJson('self-care/goals', { method: 'POST', ...apiJsonBody(input) });
}
export function updateGoal(goalId: string, version: number, input: UpdateGoal): Promise<Goal> {
  return apiJson(`self-care/goals/${encodeURIComponent(goalId)}`, { method: 'PATCH', ...apiJsonBody(input, { 'If-Match': `"${version}"` }) });
}
export function createHabit(goalId: string, input: CreateHabit): Promise<Habit> {
  return apiJson(`self-care/goals/${encodeURIComponent(goalId)}/habits`, { method: 'POST', ...apiJsonBody(input) });
}
export function completeHabit(habitId: string, localDate: string, value: number): Promise<Habit> {
  return apiJson(`self-care/habits/${encodeURIComponent(habitId)}/completions/${encodeURIComponent(localDate)}`, { method: 'PUT', ...apiJsonBody({ value }) });
}
export async function undoHabitCompletion(habitId: string, localDate: string): Promise<void> {
  await apiResponse(`self-care/habits/${encodeURIComponent(habitId)}/completions/${encodeURIComponent(localDate)}`, { method: 'DELETE' });
}
