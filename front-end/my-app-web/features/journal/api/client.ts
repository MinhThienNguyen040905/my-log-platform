import { generateHTML, type JSONContent } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import { type JournalEntry, type JournalEmotion, type MoodType } from '@/types';

export type JournalSummary = {
  id: string; title: string; occurredAt: string; localDate: string; moodCode: string | null;
  moodScore?: number | null; stressScore?: number | null; energyScore?: number | null; sleepMinutes?: number | null;
  favorite: boolean; riskLevel: string; analysisStatus: string; rowVersion: number;
};
export type JournalDetail = JournalSummary & {
  contentJson: JSONContent; location: string | null; timezone: string;
  entryStatus: string; contentVersion: number; updatedAt: string;
};
export type JournalPage = { items: JournalSummary[]; nextCursor: string | null };
export type JournalTag = { id: string; name: string; color: string | null };

const containers = new Set(['doc', 'paragraph', 'heading', 'blockquote', 'bulletList', 'orderedList', 'listItem']);
const marks = new Set(['bold', 'italic', 'underline', 'strike', 'code']);

export function prepareContent(node: JSONContent): JSONContent {
  if (node.type === 'text') {
    if (typeof node.text !== 'string' || node.marks?.some((mark) => !marks.has(mark.type))) {
      throw new Error('Bài viết có kiểu chữ chưa được hỗ trợ.');
    }
    return { type: 'text', text: node.text, ...(node.marks?.length ? { marks: node.marks.map((mark) => ({ type: mark.type })) } : {}) };
  }
  if (node.type === 'hardBreak') return { type: 'hardBreak' };
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

const emotions: JournalEmotion[] = ['neutral', 'happy', 'angry', 'sad', 'very_bad'];
export function toEntry(value: JournalSummary | JournalDetail): JournalEntry {
  const code = value.moodCode === 'very-bad' ? 'very_bad' : value.moodCode;
  const emotion = emotions.includes(code as JournalEmotion) ? code as JournalEmotion : 'neutral';
  const mood: MoodType = emotion === 'happy' ? 'calm-joy' : emotion === 'angry' ? 'anxiety-stress'
    : emotion === 'sad' || emotion === 'very_bad' ? 'sadness-reflect' : 'neutral';
  const detail = 'contentJson' in value ? value : null;
  let content = '';
  if (detail?.contentJson) {
    try { content = generateHTML(detail.contentJson, [StarterKit, Underline]); }
    catch { content = ''; }
  }
  return {
    id: value.id, rowVersion: value.rowVersion, contentVersion: detail?.contentVersion, title: value.title, content,
    contentJson: detail?.contentJson as Record<string, unknown> | undefined,
    date: value.localDate, time: new Date(value.occurredAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    mood, emotion, moodScore: value.moodScore ?? null, stressScore: value.stressScore ?? null,
    energyScore: value.energyScore ?? null, sleepHours: value.sleepMinutes == null ? null : value.sleepMinutes / 60,
    tags: [], isFavorite: value.favorite, status: value.analysisStatus as JournalEntry['status'], riskLevel: value.riskLevel,
    updatedAt: detail?.updatedAt,
  };
}
