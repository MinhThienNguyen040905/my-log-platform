import type { JournalEmotion, JournalEntry, MoodType } from '@/types';
import { Cloud, CloudDrizzle, CloudRain, Sun, Zap, type LucideIcon } from 'lucide-react';

export const JOURNAL_EMOTIONS: Record<JournalEmotion, { label: string; mood: MoodType; score: number; icon: LucideIcon }> = {
  neutral: { label: 'Bình thường', mood: 'neutral', score: 6, icon: Cloud },
  happy: { label: 'Vui', mood: 'calm-joy', score: 8, icon: Sun },
  angry: { label: 'Tức giận', mood: 'anxiety-stress', score: 4.5, icon: Zap },
  sad: { label: 'Buồn', mood: 'sadness-reflect', score: 3, icon: CloudDrizzle },
  very_bad: { label: 'Rất tệ', mood: 'sadness-reflect', score: 1.5, icon: CloudRain },
};

export const JOURNAL_EMOTION_ORDER: JournalEmotion[] = ['neutral', 'happy', 'angry', 'sad', 'very_bad'];

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
