import type { JournalEmotion, JournalEntry, MoodType } from '@/types';
import { FaceAngry, FaceGrinning, FaceNeutral, FaceSlightlyFrowning, Frown, type LucideIcon } from 'lucide-react';

/**
 * `icon` là mặt cảm xúc (bộ `face-*` của lucide; lucide chưa có mặt khóc nên "Rất tệ" tạm dùng `Frown`).
 * `swatch` là màu ô trên thanh pin cảm xúc: đỏ (rất tệ) → xanh đậm (vui).
 */
export const JOURNAL_EMOTIONS: Record<JournalEmotion, {
  label: string; mood: MoodType; score: number; icon: LucideIcon; swatch: string;
}> = {
  very_bad: { label: 'Rất tệ', mood: 'sadness-reflect', score: 1.5, icon: Frown, swatch: 'bg-battery-1 text-white' },
  sad: { label: 'Buồn', mood: 'sadness-reflect', score: 3, icon: FaceSlightlyFrowning, swatch: 'bg-battery-2 text-black' },
  angry: { label: 'Tức giận', mood: 'anxiety-stress', score: 4.5, icon: FaceAngry, swatch: 'bg-battery-3 text-black' },
  neutral: { label: 'Bình thường', mood: 'neutral', score: 6, icon: FaceNeutral, swatch: 'bg-battery-4 text-black' },
  happy: { label: 'Vui', mood: 'calm-joy', score: 8, icon: FaceGrinning, swatch: 'bg-battery-5 text-white' },
};

/** Thứ tự trên thanh pin: từ cạn (rất tệ) đến đầy (vui), theo `score`. */
export const JOURNAL_EMOTION_ORDER: JournalEmotion[] = ['very_bad', 'sad', 'angry', 'neutral', 'happy'];

export function normalizeJournalEmotion(value: unknown): JournalEmotion | undefined {
  if (value === 'crying') return 'very_bad';
  return typeof value === 'string' && Object.prototype.hasOwnProperty.call(JOURNAL_EMOTIONS, value)
    ? value as JournalEmotion
    : undefined;
}

export function journalEmotionLabel(entry: Pick<JournalEntry, 'emotion' | 'mood'>): string {
  const emotion = normalizeJournalEmotion(entry.emotion);
  if (emotion) return JOURNAL_EMOTIONS[emotion].label;
  switch (entry.mood) {
    case 'calm-joy': return 'Bình an';
    case 'hope-energy': return 'Năng lượng';
    case 'anxiety-stress': return 'Căng thẳng';
    case 'sadness-reflect': return 'Suy ngẫm';
    case 'neutral': return 'Bình thường';
  }
}
