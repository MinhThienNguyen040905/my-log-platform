import { apiJson, apiJsonBody, apiResponse } from '@/lib/api/client';

/** Consent that switches thinking-pattern recognition on; off until the user opts in. */
export const THINKING_PATTERNS_CONSENT = 'THINKING_PATTERNS';

export type PatternFeedback = 'CONFIRMED' | 'NOT_REALLY';
export type NotReallyReason = 'NOT_LIKE_ME' | 'DONT_WANT_TO_SEE' | 'WRONG_QUOTE';

/** A thinking-pattern card: a habit of thought seen in at least 3 entries over 2+ weeks. Never a diagnosis. */
export type ThinkingPattern = {
  id: string; code: string; name: string; narrative: string; explanation: string;
  steps: string[]; quotes: { text: string; date: string }[];
  entryCount: number; weekCount: number; from: string; to: string;
  feedback: PatternFeedback | null; planGoalId: string | null; planStartedAt: string | null;
  generatedBy: 'TEMPLATE' | 'AI_ASSISTED';
  /** False while the explanation and steps are draft texts awaiting professional review. */
  contentReviewed: boolean;
};

type PatternList = { items: ThinkingPattern[] };

export async function listThinkingPatterns(): Promise<ThinkingPattern[]> {
  return (await apiJson<PatternList>('patterns')).items;
}

/** Recomputes the cards from already-read entries (rate limited, no AI call). */
export async function refreshThinkingPatterns(): Promise<ThinkingPattern[]> {
  return (await apiJson<PatternList>('patterns:refresh', { method: 'POST' })).items;
}

export async function sendPatternFeedback(id: string, feedback: PatternFeedback, reason?: NotReallyReason): Promise<void> {
  await apiResponse(`patterns/${encodeURIComponent(id)}`, {
    method: 'PATCH', ...apiJsonBody(reason ? { feedback, reason } : { feedback }),
  });
}

export async function hideThinkingPattern(id: string): Promise<void> {
  await apiResponse(`patterns/${encodeURIComponent(id)}`, { method: 'PATCH', ...apiJsonBody({ status: 'HIDDEN' }) });
}

/** Creates the three-week self-care goal; safe to call again (returns the same goal). */
export function startPatternPlan(id: string): Promise<{ goalId: string; created: boolean }> {
  return apiJson(`patterns/${encodeURIComponent(id)}:start-plan`, { method: 'POST' });
}
