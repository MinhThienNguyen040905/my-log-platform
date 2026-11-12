import type { DailyMetric } from '../api/dashboard';

export type MetricKey = 'moodScore' | 'stressScore' | 'energyScore' | 'sleepMinutes';
export type MetricTrend = {
  current: number | null;
  previous: number | null;
  /** null when either window has too few days to compare. */
  direction: 'up' | 'down' | 'same' | null;
  /** Whether the change is good for the user (stress going down is good). */
  good: boolean | null;
};

/** Days with a value needed in each window before a comparison is shown. */
export const MIN_DAYS_TO_COMPARE = 3;
/** Smaller changes read as "about the same" (scores are 1–10; sleep is in minutes). */
const SAME_WITHIN: Record<MetricKey, number> = { moodScore: 0.3, stressScore: 0.3, energyScore: 0.3, sleepMinutes: 15 };
const LOWER_IS_BETTER: Record<MetricKey, boolean> = { moodScore: false, stressScore: true, energyScore: false, sleepMinutes: false };

const DAY_MS = 86_400_000;
const dayNumber = (iso: string) => Date.parse(`${iso}T00:00:00Z`) / DAY_MS;

function average(days: DailyMetric[], key: MetricKey): { value: number | null; count: number } {
  const values = days.map((day) => day[key]).filter((value): value is number => value !== null);
  return { value: values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : null, count: values.length };
}

/**
 * Average of the last `windowDays` days (ending at `to`) against the window right before it, from a timeline that
 * covers both windows (the 90-day dashboard range).
 */
export function metricTrend(timeline: DailyMetric[], to: string, key: MetricKey, windowDays = 30): MetricTrend {
  const end = dayNumber(to);
  const inWindow = (day: DailyMetric, start: number, stop: number) => {
    const n = dayNumber(day.date);
    return n > start && n <= stop;
  };
  const current = average(timeline.filter((day) => inWindow(day, end - windowDays, end)), key);
  const previous = average(timeline.filter((day) => inWindow(day, end - 2 * windowDays, end - windowDays)), key);
  if (current.count < MIN_DAYS_TO_COMPARE || previous.count < MIN_DAYS_TO_COMPARE || current.value === null || previous.value === null) {
    return { current: current.value, previous: previous.value, direction: null, good: null };
  }
  const delta = current.value - previous.value;
  if (Math.abs(delta) < SAME_WITHIN[key]) return { current: current.value, previous: previous.value, direction: 'same', good: null };
  const direction = delta > 0 ? 'up' : 'down';
  return { current: current.value, previous: previous.value, direction, good: (direction === 'down') === LOWER_IS_BETTER[key] };
}

/** Trailing average over the last `size` points that have a value; null until a point has a value. */
export function movingAverage(values: Array<number | null>, size = 7): Array<number | null> {
  return values.map((_, index) => {
    const window = values.slice(Math.max(0, index - size + 1), index + 1).filter((value): value is number => value !== null);
    return values[index] === null && window.length === 0 ? null : window.length ? window.reduce((s, v) => s + v, 0) / window.length : null;
  });
}
