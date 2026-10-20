import { apiRequest, jsonBody } from '@/lib/api/client';

export type JournalSummary = { id: string; title: string | null; occurredAt: string; localDate: string;
  moodCode: string | null; favorite: boolean; riskLevel: string | null; analysisStatus: string | null; rowVersion: number };
export type JournalEntry = JournalSummary & { contentJson: unknown; location: string | null; timezone: string;
  moodScore: number | null; stressScore: number | null; energyScore: number | null; sleepMinutes: number | null;
  entryStatus: string; contentVersion: number; createdAt: string; updatedAt: string };
export type JournalPage = { items: JournalSummary[]; nextCursor: string | null };
export type JournalTag = { id: string; name: string; color: string | null };
export type JournalInput = { title?: string; contentJson: unknown; timezone: string; occurredAt?: string;
  moodCode?: string };

export function textDocument(value: string) {
  return { type: 'doc', content: value.split('\n').map((line) => ({ type: 'paragraph',
    ...(line ? { content: [{ type: 'text', text: line }] } : {}) })) };
}
export function documentText(value: unknown): string {
  if (!value || typeof value !== 'object') return '';
  const node = value as { type?: string; text?: string; content?: unknown[] };
  if (node.type === 'text') return node.text ?? '';
  return (node.content ?? []).map(documentText).join(node.type === 'doc' ? '\n' : '');
}
export async function listEntries(cursor?: string, filters: Record<string, string> = {}): Promise<JournalPage> {
  const query = new URLSearchParams({ limit: '20', ...filters, ...(cursor ? { cursor } : {}) });
  return (await apiRequest(`/journal-entries?${query}`)).json() as Promise<JournalPage>;
}
export async function getEntry(id: string): Promise<JournalEntry> {
  return (await apiRequest(`/journal-entries/${encodeURIComponent(id)}`)).json() as Promise<JournalEntry>;
}
export async function createEntry(input: JournalInput, key: string): Promise<JournalEntry> {
  return (await apiRequest('/journal-entries', { method: 'POST', headers: { 'Idempotency-Key': key }, ...jsonBody(input) })).json() as Promise<JournalEntry>;
}
export async function updateEntry(id: string, version: number, input: Partial<JournalInput>): Promise<JournalEntry> {
  return (await apiRequest(`/journal-entries/${encodeURIComponent(id)}`, { method: 'PATCH',
    headers: { 'If-Match': `"${version}"` }, ...jsonBody(input) })).json() as Promise<JournalEntry>;
}
export async function deleteEntry(id: string, version: number) {
  await apiRequest(`/journal-entries/${encodeURIComponent(id)}`, { method: 'DELETE', headers: { 'If-Match': `"${version}"` } });
}
export async function favoriteEntry(id: string, favorite: boolean): Promise<JournalEntry> {
  return (await apiRequest(`/journal-entries/${encodeURIComponent(id)}/favorite`,
    { method: favorite ? 'PUT' : 'DELETE' })).json() as Promise<JournalEntry>;
}
export async function listTags(): Promise<JournalTag[]> {
  return (await apiRequest('/journal-tags')).json() as Promise<JournalTag[]>;
}
export async function createTag(name: string): Promise<JournalTag> {
  return (await apiRequest('/journal-tags', { method: 'POST', ...jsonBody({ name }) })).json() as Promise<JournalTag>;
}
export async function setEntryTag(entryId: string, tagId: string, attach: boolean) {
  await apiRequest(`/journal-entries/${encodeURIComponent(entryId)}/tags/${encodeURIComponent(tagId)}`,
    { method: attach ? 'PUT' : 'DELETE' });
}
