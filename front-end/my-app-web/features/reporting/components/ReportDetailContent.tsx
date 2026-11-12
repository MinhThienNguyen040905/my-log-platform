'use client';

import Link from 'next/link';
import { ArrowDownRight, ArrowRight, ArrowUpRight, Smile, CloudRain } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import type { Report } from '../api/reports';
import { codeChanges, compareMetric, formatDay, formatScore, highlightDays, type Trend } from '../utils/report-highlights';
import { WeeklyEvidenceTimelineChart } from './WeeklyEvidenceTimelineChart';

/**
 * Body of a READY weekly/monthly reflection: summary, comparison with the previous period, highlight days,
 * tones, topics and the daily chart. Shared by the reflection tab on /insight and the detail/print page.
 */
export function ReportDetailContent({ report }: { report: Report }) {
  const { t } = useTranslation();
  const monthly = report.type === 'MONTHLY';
  return <>
    {report.narrative && <section aria-labelledby={`report-summary-${report.id}`}>
      <h2 id={`report-summary-${report.id}`} className="sr-only">{t('report.summary')}</h2>
      <p className="font-serif text-base leading-relaxed">{report.narrative}</p>
      {report.generatedBy === 'AI_ASSISTED' && <p className="mt-1 text-xs text-gray-600">{t('report.aiWording')}</p>}
    </section>}

    <section aria-labelledby={`report-period-${report.id}`}>
      <h2 id={`report-period-${report.id}`} className="font-space text-lg font-extrabold mb-3">{monthly ? t('report.yourMonth') : t('report.yourWeek')}</h2>
      <dl className="grid grid-cols-2 gap-4 border-y border-black/30 py-4 text-sm sm:grid-cols-4">
        <Metric report={report} label={t('report.mood')} metricKey="averageMood" suffix="/10" goodWhen="up" monthly={monthly} />
        <Metric report={report} label={t('report.stress')} metricKey="averageStress" suffix="/10" goodWhen="down" monthly={monthly} />
        <Metric report={report} label={t('report.energy')} metricKey="averageEnergy" suffix="/10" goodWhen="up" monthly={monthly} />
        <Metric report={report} label={t('report.sleep')} metricKey="averageSleepMinutes" suffix={t('report.hours')} goodWhen="up" monthly={monthly} threshold={15} divisor={60} />
      </dl>
      <p className="mt-2 text-xs text-gray-600">{t('report.journalDays', { days: Number(report.metrics.journalDays ?? 0), total: Number(report.metrics.periodDays ?? 0), streak: Number(report.metrics.longestJournalStreak ?? 0) })}</p>
    </section>

    <Highlights report={report} />
    <CodeList report={report} metricsKey="topEmotions" title={t('report.emotions')} labelPrefix="report.emotionLabels" monthly={monthly} />
    <CodeList report={report} metricsKey="topTopics" title={t('report.topics')} labelPrefix="report.topicLabels" monthly={monthly} />

    {report.evidence && report.evidence.length > 0 && <WeeklyEvidenceTimelineChart evidence={report.evidence} />}
    <p className="text-xs text-gray-600">{t('report.disclaimer')}</p>
  </>;
}

function TrendIcon({ trend, good }: { trend: Trend; good: boolean }) {
  const className = `inline h-4 w-4 ${trend === 'same' ? 'text-gray-500' : good ? 'text-green-700' : 'text-amber-700'}`;
  if (trend === 'up') return <ArrowUpRight className={className} aria-hidden="true" />;
  if (trend === 'down') return <ArrowDownRight className={className} aria-hidden="true" />;
  return <ArrowRight className={className} aria-hidden="true" />;
}

/** `threshold` is in the metric's own unit; `divisor` only changes how values are shown (sleep: minutes → hours). */
function Metric({ report, label, metricKey, suffix, goodWhen, monthly, threshold, divisor = 1 }: {
  report: Report; label: string; metricKey: string; suffix: string; goodWhen: 'up' | 'down'; monthly: boolean; threshold?: number; divisor?: number;
}) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const c = compareMetric(report, metricKey, threshold);
  const shown = (value: number | null) => formatScore(value === null ? null : value / divisor, locale);
  return <div>
    <dt>{label}</dt>
    <dd className="font-bold">{c.current === null ? t('report.noData') : `${shown(c.current)}${suffix}`}</dd>
    {c.trend && <dd className="text-xs text-gray-700 flex items-center gap-1">
      <TrendIcon trend={c.trend} good={c.trend === goodWhen} />
      {c.trend === 'same'
        ? t(monthly ? 'report.sameAsLastMonth' : 'report.sameAsLastWeek')
        : t(monthly ? 'report.vsLastMonth' : 'report.vsLastWeek', { value: shown(c.previous) })}
    </dd>}
  </div>;
}

function Highlights({ report }: { report: Report }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const days = highlightDays(report);
  if (days.length === 0) return null;
  return <section aria-labelledby={`report-highlights-${report.id}`}>
    <h2 id={`report-highlights-${report.id}`} className="font-space text-lg font-extrabold mb-3">{t('report.highlights')}</h2>
    <ul className="grid gap-3 sm:grid-cols-2">
      {days.map((day) => <li key={day.kind} className="rounded-xl border-2 border-black bg-paper-warm p-4 flex flex-col gap-1">
        <span className="inline-flex items-center gap-1.5 font-space text-xs font-extrabold uppercase">
          {day.kind === 'best' ? <Smile className="h-4 w-4 text-green-700" aria-hidden="true" /> : <CloudRain className="h-4 w-4 text-blue-700" aria-hidden="true" />}
          {day.kind === 'best' ? t('report.bestDay') : t('report.lowestDay')}
        </span>
        <span className="font-bold">{formatDay(day.date, locale)} · {t('report.moodValue', { value: formatScore(day.mood, locale) })}</span>
        {day.entryId && <Link href={`/journal-editor?id=${day.entryId}`} className="no-print text-sm underline">
          {day.title ? t('report.openEntryTitled', { title: day.title }) : t('report.openEntry')}
        </Link>}
      </li>)}
    </ul>
  </section>;
}

function CodeList({ report, metricsKey, title, labelPrefix, monthly }: {
  report: Report; metricsKey: 'topEmotions' | 'topTopics'; title: string; labelPrefix: string; monthly: boolean;
}) {
  const { t } = useTranslation();
  const items = codeChanges(report, metricsKey);
  if (items.length === 0) return null;
  return <section aria-label={title}>
    <h2 className="font-space text-lg font-extrabold mb-3">{title}</h2>
    <ul className="flex flex-wrap gap-2">
      {items.map((item) => <li key={item.code} className="rounded-xl border border-black bg-white px-3 py-1.5 text-sm">
        <span className="font-bold">{t(`${labelPrefix}.${item.code}`, { defaultValue: item.code })}</span> × {item.count}
        <span className="ml-1 text-xs text-gray-600">({t(monthly ? 'report.prevMonthCount' : 'report.prevWeekCount', { count: item.previousCount })})</span>
      </li>)}
    </ul>
  </section>;
}
