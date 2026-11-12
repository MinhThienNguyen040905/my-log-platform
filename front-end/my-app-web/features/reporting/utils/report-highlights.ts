import type { Report } from '../api/reports';

export type Trend = 'up' | 'down' | 'same';
export type MetricComparison = { current: number | null; previous: number | null; delta: number | null; trend: Trend | null };
export type HighlightDay = { kind: 'best' | 'lowest'; date: string; mood: number; entryId: string | null; title: string | null };
export type CodeChange = { code: string; count: number; previousCount: number; trend: Trend };

const asNumber = (value: unknown): number | null => {
  const n = typeof value === 'number' ? value : typeof value === 'string' ? Number(value) : NaN;
  return Number.isFinite(n) ? n : null;
};

const asRecord = (value: unknown): Record<string, unknown> | null =>
  value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : null;

/** Current vs previous period for one average (`averageMood`, `averageStress`, …). Small moves count as "same". */
export function compareMetric(report: Report, key: string, threshold = 0.3): MetricComparison {
  const current = asNumber(report.metrics[key]);
  const previous = asNumber(asRecord(report.metrics.previous)?.[key]);
  if (current === null || previous === null) return { current, previous, delta: null, trend: null };
  const delta = Math.round((current - previous) * 10) / 10;
  return { current, previous, delta, trend: Math.abs(current - previous) < threshold ? 'same' : delta > 0 ? 'up' : 'down' };
}

/** Best and lowest mood days with the entry title the owner wrote that day (when there is one). */
export function highlightDays(report: Report): HighlightDay[] {
  const out: HighlightDay[] = [];
  for (const [kind, key] of [['best', 'bestDay'], ['lowest', 'lowestDay']] as const) {
    const day = asRecord(report.metrics[key]);
    const mood = asNumber(day?.moodScore);
    if (!day || typeof day.date !== 'string' || mood === null) continue;
    const entryId = typeof day.entryId === 'string' ? day.entryId : null;
    out.push({ kind, date: day.date, mood, entryId, title: entryId ? report.entryTitles?.[entryId] ?? null : null });
  }
  return out;
}

/** Emotion/topic codes of the period with how often they appeared in the previous one. */
export function codeChanges(report: Report, key: 'topEmotions' | 'topTopics'): CodeChange[] {
  const list = Array.isArray(report.metrics[key]) ? report.metrics[key] as unknown[] : [];
  return list.map(asRecord).filter((item): item is Record<string, unknown> => !!item && typeof item.code === 'string')
    .map((item) => {
      const count = asNumber(item.count) ?? 0;
      const previousCount = asNumber(item.previousCount) ?? 0;
      return { code: item.code as string, count, previousCount, trend: count > previousCount ? 'up' : count < previousCount ? 'down' : 'same' };
    });
}

/** The API lists every version; show only the newest version of each period (older ones stay reachable by ID). */
export function latestPerPeriod(reports: Report[]): Report[] {
  const latest = new Map<string, Report>();
  for (const report of reports) {
    const key = `${report.type}|${report.from}|${report.to}`;
    const current = latest.get(key);
    if (!current || report.version > current.version) latest.set(key, report);
  }
  return reports.filter((report) => latest.get(`${report.type}|${report.from}|${report.to}`) === report);
}

const isoDay = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

/**
 * First day of the most recent finished period, matching the backend (`ReportPeriod.previousWeek/Month`):
 * last week's Monday or last month's 1st. Uses the browser date; the backend still decides the real period.
 */
export function lastFinishedPeriodStart(type: 'WEEKLY' | 'MONTHLY', today: Date = new Date()): string {
  if (type === 'MONTHLY') return isoDay(new Date(today.getFullYear(), today.getMonth() - 1, 1));
  const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - ((today.getDay() + 6) % 7));
  monday.setDate(monday.getDate() - 7);
  return isoDay(monday);
}

export function formatScore(value: number | null, locale: string): string {
  if (value === null) return '–';
  return new Intl.NumberFormat(locale === 'en' ? 'en-US' : 'vi-VN', { maximumFractionDigits: 1 }).format(value);
}

/** Day label such as "T7, 07/11" / "Sat, 11/07" from an ISO date, independent of the browser timezone. */
export function formatDay(date: string, locale: string): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Intl.DateTimeFormat(locale === 'en' ? 'en-US' : 'vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: 'UTC' })
    .format(new Date(Date.UTC(y, m - 1, d)));
}
