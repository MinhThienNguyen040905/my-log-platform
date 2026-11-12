import { generateHTML, type JSONContent } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Highlight from '@tiptap/extension-highlight';
import { JournalImageBase } from '../extensions/journal-image-base';
import { PromptQuestion } from '../extensions/prompt-question';
import { type JournalEntry, type JournalEmotion, type MoodType } from '@/types';

export type JournalTag = { id: string; name: string; color: string | null };

export type JournalSummary = {
  id: string; title: string; occurredAt: string; localDate: string; moodCode: string | null;
  moodScore?: number | null; stressScore?: number | null; energyScore?: number | null; sleepMinutes?: number | null;
  favorite: boolean; riskLevel: string; analysisStatus: string; rowVersion: number;
  tags?: JournalTag[];
  photoUrl?: string | null;
};
export type JournalDetail = JournalSummary & {
  contentJson: JSONContent; location: string | null; timezone: string;
  entryStatus: string; contentVersion: number; updatedAt: string;
};
export type JournalPage = { items: JournalSummary[]; nextCursor: string | null };
export const JOURNAL_PAGE_SIZE = 5;

export async function getRecentEntries(limit = 2): Promise<JournalEntry[]> {
  const page = await journalJson<JournalPage>(`journal-entries?limit=${limit}`);
  return page.items.map(toEntry);
}

/** Saved entries per local day (`GET /journal-entries/daily-counts`, max 92 days); no journal content. */
export type JournalDayCount = { date: string; count: number; latestMoodCode: string | null };
export type JournalDailyCounts = { from: string; to: string; days: JournalDayCount[] };

export function getJournalDailyCounts(from: string, to: string): Promise<JournalDailyCounts> {
  const query = new URLSearchParams({ from, to });
  return journalJson<JournalDailyCounts>(`journal-entries/daily-counts?${query.toString()}`);
}

export type DetailedJournalPage = { items: JournalEntry[]; nextCursor: string | null };

export async function getDetailedJournalPage({ date, cursor }: { date?: string; cursor?: string | null } = {}): Promise<DetailedJournalPage> {
  const query = new URLSearchParams({ limit: String(JOURNAL_PAGE_SIZE) });
  if (date) { query.set('from', date); query.set('to', date); }
  if (cursor) query.set('cursor', cursor);
  const page = await journalJson<JournalPage>(`journal-entries?${query.toString()}`);
  const details = await Promise.all(page.items.map((entry) =>
    journalJson<JournalDetail>(`journal-entries/${encodeURIComponent(entry.id)}`),
  ));
  return { items: details.map(toEntry), nextCursor: page.nextCursor };
}

export async function getSearchJournalPage({ query, cursor, limit = JOURNAL_PAGE_SIZE }: {
  query: string;
  cursor?: string | null;
  limit?: number;
}): Promise<DetailedJournalPage> {
  const trimmed = query.trim();
  if (!trimmed) return { items: [], nextCursor: null };
  const params = new URLSearchParams({ query: trimmed, limit: String(limit) });
  if (cursor) params.set('cursor', cursor);
  const page = await journalJson<JournalPage>(`journal-entries/search?${params.toString()}`);
  const details = await Promise.all(page.items.map((entry) =>
    journalJson<JournalDetail>(`journal-entries/${encodeURIComponent(entry.id)}`),
  ));
  return { items: details.map(toEntry), nextCursor: page.nextCursor };
}

const containers = new Set(['doc', 'paragraph', 'heading', 'blockquote', 'promptQuestion', 'bulletList', 'orderedList', 'listItem']);
// Keep in sync with backend TipTapContent.MARKS (highlight is single-colour: no attributes are sent).
const marks = new Set(['bold', 'italic', 'underline', 'strike', 'code', 'highlight']);

export function prepareContent(node: JSONContent): JSONContent {
  if (node.type === 'text') {
    if (typeof node.text !== 'string' || node.marks?.some((mark) => !marks.has(mark.type))) {
      throw new Error('Bài viết có kiểu chữ chưa được hỗ trợ.');
    }
    return { type: 'text', text: node.text, ...(node.marks?.length ? { marks: node.marks.map((mark) => ({ type: mark.type })) } : {}) };
  }
  if (node.type === 'hardBreak') return { type: 'hardBreak' };
  if (node.type === 'image') {
    const assetId = (node.attrs?.['data-asset-id'] || node.attrs?.dataAssetId) as string | undefined;
    if (!assetId || typeof assetId !== 'string') {
      throw new Error('Ảnh chưa được tải lên hoặc thiếu mã định danh hợp lệ.');
    }
    return {
      type: 'image',
      attrs: {
        'data-asset-id': assetId,
        alt: typeof node.attrs?.alt === 'string' ? node.attrs.alt : '',
        title: typeof node.attrs?.title === 'string' ? node.attrs.title : '',
        src: '',
      },
    };
  }
  if (!node.type || !containers.has(node.type)) throw new Error('Bài viết có ảnh hoặc khối nội dung chưa được hỗ trợ.');
  if (node.type === 'orderedList' && node.attrs?.start && node.attrs.start !== 1) {
    throw new Error('Danh sách đánh số bắt đầu khác 1 chưa được hỗ trợ.');
  }
  const attrs = node.type === 'heading' ? { attrs: { level: node.attrs?.level } } : {};
  if (node.type === 'heading' && ![1, 2, 3].includes(node.attrs?.level)) throw new Error('Cấp tiêu đề chưa được hỗ trợ.');
  return { type: node.type, ...attrs, ...(node.content?.length ? { content: node.content.map(prepareContent) } : {}) };
}

export async function journalRequest(path: string, init?: RequestInit) {
  const response = await fetch(`/api/${path}`, { cache: 'no-store', ...init });
  if (!response.ok) {
    if (response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    if (response.status === 409) throw new Error('Bài viết đã thay đổi ở nơi khác. Hãy tải lại trước khi sửa tiếp.');
    if (response.status === 503) throw new Error('Không thể kết nối máy chủ. Vui lòng thử lại.');
    if (response.status === 429) throw new Error('Đã đạt giới hạn ghi tạm thời. Vui lòng thử lại sau.');
    throw new Error('Không thể hoàn tất thao tác với nhật ký. Vui lòng thử lại.');
  }
  return response;
}

export async function journalJson<T>(path: string, init?: RequestInit): Promise<T> {
  return (await journalRequest(path, init)).json() as Promise<T>;
}

export function journalBody(body: unknown, headers: Record<string, string> = {}): RequestInit {
  return { headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(body) };
}

export interface UploadedAsset {
  id: string;
  format: string;
  mimeType: string;
  width: number;
  height: number;
  sizeBytes: number;
  url: string;
}

export async function uploadJournalAsset(file: File): Promise<UploadedAsset> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await journalRequest('journal-assets/upload', {
    method: 'POST',
    body: formData,
  });
  return res.json() as Promise<UploadedAsset>;
}

const emotions: JournalEmotion[] = ['neutral', 'happy', 'angry', 'sad', 'very_bad'];
export function toEntry(value: JournalSummary | JournalDetail): JournalEntry {
  const code = value.moodCode === 'very-bad' ? 'very_bad' : value.moodCode;
  const emotion = emotions.includes(code as JournalEmotion) ? code as JournalEmotion : 'neutral';
  const mood: MoodType = emotion === 'happy' ? 'calm-joy' : emotion === 'angry' ? 'anxiety-stress'
    : emotion === 'sad' || emotion === 'very_bad' ? 'sadness-reflect' : 'neutral';
  const detail = 'contentJson' in value ? value : null;
  let content = '';
  if (detail?.contentJson) {
    // Must list every node/mark the editor can produce: an unknown one makes generateHTML throw, and the
    // entry would then open empty. Keep in sync with JournalTipTapEditor and backend TipTapContent.
    try { content = generateHTML(detail.contentJson, [StarterKit, Underline, Highlight.configure({ multicolor: false }), JournalImageBase, PromptQuestion]); }
    catch { content = ''; }
  }
  return {
    id: value.id, rowVersion: value.rowVersion, contentVersion: detail?.contentVersion, title: value.title, content,
    contentJson: detail?.contentJson as Record<string, unknown> | undefined,
    date: value.localDate, time: new Date(value.occurredAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    mood, emotion, moodScore: value.moodScore ?? null, stressScore: value.stressScore ?? null,
    energyScore: value.energyScore ?? null, sleepHours: value.sleepMinutes == null ? null : value.sleepMinutes / 60,
    tags: value.tags?.map((t) => t.name) || [], isFavorite: value.favorite, status: value.analysisStatus as JournalEntry['status'], riskLevel: value.riskLevel,
    updatedAt: detail?.updatedAt,
    photoUrl: value.photoUrl ?? undefined,
  };
}

export async function getOrCreateTagIds(tagNames: string[]): Promise<string[]> {
  const cleanNames = Array.from(new Set(tagNames.map((t) => t.trim()).filter(Boolean))).slice(0, 20);
  if (cleanNames.length === 0) return [];
  const existingTags = await journalJson<JournalTag[]>('journal-tags');
  const tagIds: string[] = [];
  for (const name of cleanNames) {
    let tag = existingTags.find((t) => t.name.toLowerCase() === name.toLowerCase());
    if (!tag) {
      tag = await journalJson<JournalTag>('journal-tags', {
        method: 'POST',
        ...journalBody({ name }),
      });
      existingTags.push(tag);
    }
    tagIds.push(tag.id);
  }
  return tagIds;
}
