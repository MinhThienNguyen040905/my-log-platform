import { journalJson } from './client';

export type WritingSuggestion = {
  status: 'SUGGESTION' | 'CONSENT_REQUIRED' | 'SAFETY_FLOW' | 'BLOCKED' | 'UNAVAILABLE';
  suggestion: string | null;
  promptVersion: string | null;
};

export function getWritingSuggestion(text: string, signal?: AbortSignal): Promise<WritingSuggestion> {
  return journalJson('journal-writing-suggestions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
    signal,
  });
}
