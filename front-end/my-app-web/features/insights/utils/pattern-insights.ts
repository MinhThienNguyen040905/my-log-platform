import type { Insight, InsightEvidence } from '../api/insights';

/** Pattern insights come from the 30-day detectors; older ones are weekly/monthly report insights. */
export function isPatternInsight(insight: Insight): boolean {
  return insight.algorithmVersion.startsWith('pattern-');
}

export type EvidenceBar = { date: string; value: number; ratio: number };
export type EvidenceSummary =
  | { kind: 'bars'; metric: string; max: number; bars: EvidenceBar[] }
  | { kind: 'share'; metric: string; present: number; total: number };

/**
 * Turns stored evidence into something small to draw: the share of entries with an emotion, or the
 * latest daily values of the main metric (scores on a 10-point scale, sleep in hours).
 */
export function summarizeEvidence(evidence: InsightEvidence[], limit = 14): EvidenceSummary | null {
  if (evidence.length === 0) return null;
  const emotion = evidence.filter((e) => e.metric.startsWith('EMOTION_'));
  if (emotion.length > 0) {
    return { kind: 'share', metric: emotion[0].metric, present: emotion.filter((e) => e.value > 0).length, total: emotion.length };
  }
  const metric = evidence.some((e) => e.metric === 'STRESS_SCORE') ? 'STRESS_SCORE'
    : evidence.some((e) => e.metric === 'MOOD_SCORE') ? 'MOOD_SCORE' : evidence[0].metric;
  const sleep = metric === 'SLEEP_MINUTES';
  const points = evidence.filter((e) => e.metric === metric)
    .map((e) => ({ date: e.date, value: sleep ? Math.round((e.value / 60) * 10) / 10 : e.value }))
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(-limit);
  const max = sleep ? Math.max(10, ...points.map((p) => p.value)) : 10;
  return { kind: 'bars', metric, max, bars: points.map((p) => ({ ...p, ratio: Math.max(0.04, Math.min(1, p.value / max)) })) };
}
