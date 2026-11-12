import type { TopCode } from '../api/dashboard';

export type SpectrumItem = { key: string; label: string; value: number; color: string };
type Translate = (key: string, options?: Record<string, unknown>) => string;

/*
 * Category colours. Moods keep the brand mood colours; tones use the reference categorical palette. The chart
 * writes each category's name next to its bar (largest first, empty ones hidden), so colour is never the only
 * way to tell categories apart. Colour follows the category, never its rank.
 */
const MOODS: { code: string; color: string }[] = [
  { code: 'happy', color: '#70E000' },
  { code: 'sad', color: '#4D96FF' },
  { code: 'neutral', color: '#FFD166' },
  { code: 'angry', color: '#FF6B6B' },
  { code: 'very-bad', color: '#BD6EFE' },
];
const TONES: { code: string; color: string }[] = [
  { code: 'JOY', color: '#eda100' },
  { code: 'HOPE', color: '#008300' },
  { code: 'CALM', color: '#1baf7a' },
  { code: 'ANXIETY', color: '#eb6834' },
  { code: 'FEAR', color: '#6250d6' },
  { code: 'ANGER', color: '#e34948' },
  { code: 'SADNESS', color: '#2a78d6' },
];
const FALLBACK = '#B5B3AD';

function fixedOrder(items: TopCode[], palette: { code: string; color: string }[]) {
  const byCode = new Map(items.map((item) => [item.code, item.count]));
  const known = palette.map(({ code, color }) => ({ code, color, count: byCode.get(code) ?? 0 }));
  // Codes outside the palette (e.g. a legacy mood code) go last, in neutral grey.
  const extra = items.filter((item) => !palette.some((p) => p.code === item.code))
    .map((item) => ({ code: item.code, color: FALLBACK, count: item.count }));
  return [...known, ...extra];
}

/** "Phổ cảm xúc": how many entries carried each mood icon the user picked. */
export function moodSpectrum(topMoods: TopCode[], t: Translate): SpectrumItem[] {
  if (topMoods.length === 0) return [];
  return fixedOrder(topMoods, MOODS).map(({ code, color, count }) => ({
    key: code, label: t(`dashboard.moodLabels.${code}`, { defaultValue: code }), value: count, color,
  }));
}

/**
 * "Sắc thái trong bài viết": share of analyzed entries whose main tone is each code. The API counts only the
 * primary tone, so the counts add up to the number of analyzed entries.
 */
export function toneSpectrum(topEmotions: TopCode[], t: Translate): SpectrumItem[] {
  const total = topEmotions.reduce((sum, item) => sum + item.count, 0);
  if (total === 0) return [];
  return fixedOrder(topEmotions, TONES).map(({ code, color, count }) => ({
    key: code, label: t(`dashboard.toneLabels.${code}`, { defaultValue: code }), value: Math.round((count / total) * 100), color,
  }));
}

export function topicLabel(code: string, t: Translate): string {
  return t(`dashboard.topicLabels.${code}`, { defaultValue: code });
}

/** Brand colour of a mood code (`very-bad` style, as the API sends it); null for unknown codes. */
export function moodColor(code: string | null | undefined): string | null {
  return MOODS.find((mood) => mood.code === code)?.color ?? null;
}
