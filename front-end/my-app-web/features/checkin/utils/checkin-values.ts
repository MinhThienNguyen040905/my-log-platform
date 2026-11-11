import type { JournalEmotion } from '@/types';

/**
 * Mã `moodCode` gửi cho check-in. Backend chỉ nhận `[a-z0-9-]{1,32}`, nên `very_bad` thành `very-bad`.
 * Bảng map một-một, giữ đủ 5 mức (đã chốt 2026-11-11, xem backend/docs/WEB_MOBILE_GAP_IMPLEMENTATION_PLAN.md W2).
 * Mobile phải dùng đúng bảng này.
 */
export const CHECKIN_MOOD_CODES: Record<JournalEmotion, string> = {
  happy: 'happy',
  neutral: 'neutral',
  angry: 'angry',
  sad: 'sad',
  very_bad: 'very-bad',
};

export function emotionFromMoodCode(code: string | null | undefined): JournalEmotion | null {
  if (!code) return null;
  const match = (Object.entries(CHECKIN_MOOD_CODES) as [JournalEmotion, string][]).find(([, value]) => value === code);
  return match ? match[0] : null;
}

/** Ngày theo múi giờ của hồ sơ (không theo múi giờ trình duyệt), dạng YYYY-MM-DD. */
export function localDateIn(timezone: string, at: Date = new Date()): string {
  try {
    return new Intl.DateTimeFormat('en-CA', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(at);
  } catch {
    return new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(at);
  }
}

/** Giới hạn của backend (`CheckinService`). */
export const CHECKIN_LIMITS = {
  scoreMin: 1,
  scoreMax: 10,
  sleepHoursMax: 24,
  noteMaxLength: 4000,
} as const;

/** Điểm 1–10, tối đa 1 chữ số thập phân. */
export function roundScore(value: number): number {
  return Math.min(CHECKIN_LIMITS.scoreMax, Math.max(CHECKIN_LIMITS.scoreMin, Math.round(value * 10) / 10));
}
