import type { TopCode } from '../api/dashboard';

export type SpectrumItem = { key: string; label: string; value: number; color: string };
type Translate = (key: string, options?: Record<string, unknown>) => string;

/** Colours for the mood icons the user picks (`mood_code`; `very-bad` is how the API stores "Rất tệ"). */
const MOOD_COLORS: Record<string, string> = {
  happy: '#70E000', neutral: '#FFD166', angry: '#FF6B6B', sad: '#4D96FF', 'very-bad': '#BD6EFE',
};
/** Colours for analysis tones (backend AnalysisOutputValidator codes). */
const TONE_COLORS: Record<string, string> = {
  JOY: '#70E000', CALM: '#4FA3A5', HOPE: '#FFD166', ANXIETY: '#FF9F1C', SADNESS: '#4D96FF', ANGER: '#FF6B6B', FEAR: '#BD6EFE',
};
const FALLBACK = '#D9D7D2';

/** "Phổ cảm xúc": how many entries carried each mood icon the user picked. */
export function moodSpectrum(topMoods: TopCode[], t: Translate): SpectrumItem[] {
  return topMoods.map((item) => ({
    key: item.code,
    label: t(`dashboard.moodLabels.${item.code}`, { defaultValue: item.code }),
    value: item.count,
    color: MOOD_COLORS[item.code] ?? FALLBACK,
  }));
}

/**
 * "Sắc thái trong bài viết": share of analyzed entries whose main tone is each code. The API counts only
 * the primary tone, so the counts add up to the number of analyzed entries.
 */
export function toneSpectrum(topEmotions: TopCode[], t: Translate): SpectrumItem[] {
  const total = topEmotions.reduce((sum, item) => sum + item.count, 0);
  if (total === 0) return [];
  return topEmotions.map((item) => ({
    key: item.code,
    label: t(`dashboard.toneLabels.${item.code}`, { defaultValue: item.code }),
    value: Math.round((item.count / total) * 100),
    color: TONE_COLORS[item.code] ?? FALLBACK,
  }));
}

export function topicLabel(code: string, t: Translate): string {
  return t(`dashboard.topicLabels.${code}`, { defaultValue: code });
}
