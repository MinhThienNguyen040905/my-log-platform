'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowDownRight, ArrowRight, ArrowUpRight, RefreshCw, Smile, CloudRain } from 'lucide-react';
import { getReport, regenerateReport, type Report } from '../api/reports';
import { downloadReportCsv } from '../utils/download-report-csv';
import { codeChanges, compareMetric, formatDay, formatScore, highlightDays, type Trend } from '../utils/report-highlights';
import { WeeklyEvidenceTimelineChart } from './WeeklyEvidenceTimelineChart';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';

/** Detail page of a weekly or monthly reflection ("Nhìn lại"); also the print/PDF view. */
export function WeeklyReportPrintView({ id }: { id: string }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const { userId } = useJournal();
  const router = useRouter();
  const queryClient = useQueryClient();
  const reportQuery = useQuery({
    queryKey: ['report', userId, id],
    queryFn: () => getReport(id),
    refetchInterval: (query) => query.state.data && ['PENDING', 'PROCESSING'].includes(query.state.data.status) ? 5000 : false,
  });
  const regenerate = useMutation({
    mutationFn: () => regenerateReport(id),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: ['reports'] });
      router.push(`/reports/${created.id}/print`);
    },
  });
  const report = reportQuery.data ?? null;
  const monthly = report?.type === 'MONTHLY';
  const error = reportQuery.error instanceof Error ? (locale === 'en' ? t('status.reportError') : reportQuery.error.message) : null;

  return <div className="mx-auto max-w-4xl px-4 py-8">
    <div className="no-print mb-6 flex flex-wrap items-center justify-between gap-3">
      <Link href="/insight" className="text-sm font-bold underline">{t('report.back')}</Link>
      {report?.status === 'READY' && <div className="flex flex-wrap gap-2">
        <button type="button" onClick={() => regenerate.mutate()} disabled={regenerate.isPending}
          className="inline-flex items-center gap-1.5 rounded-lg border-2 border-black bg-white px-4 py-2 text-sm font-bold disabled:opacity-60">
          <RefreshCw className={`h-4 w-4 ${regenerate.isPending ? 'animate-spin' : ''}`} aria-hidden="true" />{t('report.regenerate')}
        </button>
        <button type="button" onClick={() => downloadReportCsv(report)} className="rounded-lg border-2 border-black bg-white px-4 py-2 text-sm font-bold">{t('report.csv')}</button>
        <button type="button" onClick={() => window.print()} className="rounded-lg border-2 border-black bg-primary-container px-4 py-2 text-sm font-bold">{t('report.print')}</button>
      </div>}
    </div>
    {regenerate.isError && <p role="alert" className="no-print mb-4 text-sm text-red-700">{t('report.regenerateError')}</p>}
    {reportQuery.isPending && <p role="status">{t('status.loadingReport')}</p>}
    {error && <p role="alert" className="text-red-700">{error} <button type="button" className="underline" onClick={() => void reportQuery.refetch()}>{t('common.retry')}</button></p>}
    {report && <article className="print-container rounded-2xl border-2 border-black bg-white p-6 sm:p-10 flex flex-col gap-6">
      <header>
        <h1 className="font-space text-2xl font-extrabold">{monthly ? t('report.titleMonthly') : t('report.title')}</h1>
        <p className="mt-2 text-sm">{t('report.from')} {report.from} {t('report.to')} {report.to} · {t('report.sample')} {report.sampleSize} {t('report.days')}
          {report.version > 1 && <> · {t('report.version', { version: report.version })}</>}</p>
      </header>
      {report.status === 'READY' ? <>
        {report.narrative && <section aria-labelledby="report-summary">
          <h2 id="report-summary" className="sr-only">{t('report.summary')}</h2>
          <p className="font-serif text-base leading-relaxed">{report.narrative}</p>
          {report.generatedBy === 'AI_ASSISTED' && <p className="mt-1 text-xs text-gray-600">{t('report.aiWording')}</p>}
        </section>}

        <section aria-labelledby="report-period">
          <h2 id="report-period" className="font-space text-lg font-extrabold mb-3">{monthly ? t('report.yourMonth') : t('report.yourWeek')}</h2>
          <dl className="grid grid-cols-2 gap-4 border-y border-black/30 py-4 text-sm sm:grid-cols-4">
            <Metric report={report} label={t('report.mood')} metricKey="averageMood" suffix="/10" goodWhen="up" monthly={monthly} />
            <Metric report={report} label={t('report.stress')} metricKey="averageStress" suffix="/10" goodWhen="down" monthly={monthly} />
            <Metric report={report} label={t('report.energy')} metricKey="averageEnergy" suffix="/10" goodWhen="up" monthly={monthly} />
            <Metric report={report} label={t('report.sleep')} metricKey="averageSleepMinutes" suffix={t('report.minutes')} goodWhen="up" monthly={monthly} threshold={15} />
          </dl>
          <p className="mt-2 text-xs text-gray-600">{t('report.journalDays', { days: Number(report.metrics.journalDays ?? 0), total: Number(report.metrics.periodDays ?? 0), streak: Number(report.metrics.longestJournalStreak ?? 0) })}</p>
        </section>

        <Highlights report={report} />
        <CodeList report={report} metricsKey="topEmotions" title={t('report.emotions')} labelPrefix="report.emotionLabels" monthly={monthly} />
        <CodeList report={report} metricsKey="topTopics" title={t('report.topics')} labelPrefix="report.topicLabels" monthly={monthly} />

        {report.evidence && report.evidence.length > 0 && <WeeklyEvidenceTimelineChart evidence={report.evidence} />}
        <p className="text-xs text-gray-600">{t('report.disclaimer')}</p>
      </> : <p>{report.status === 'FAILED' ? t('report.failed') : t('report.processing')}</p>}
    </article>}
  </div>;
}

function TrendIcon({ trend, good }: { trend: Trend; good: boolean }) {
  const className = `inline h-4 w-4 ${trend === 'same' ? 'text-gray-500' : good ? 'text-green-700' : 'text-amber-700'}`;
  if (trend === 'up') return <ArrowUpRight className={className} aria-hidden="true" />;
  if (trend === 'down') return <ArrowDownRight className={className} aria-hidden="true" />;
  return <ArrowRight className={className} aria-hidden="true" />;
}

function Metric({ report, label, metricKey, suffix, goodWhen, monthly, threshold }: {
  report: Report; label: string; metricKey: string; suffix: string; goodWhen: 'up' | 'down'; monthly: boolean; threshold?: number;
}) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const c = compareMetric(report, metricKey, threshold);
  return <div>
    <dt>{label}</dt>
    <dd className="font-bold">{c.current === null ? t('report.noData') : `${formatScore(c.current, locale)}${suffix}`}</dd>
    {c.trend && <dd className="text-xs text-gray-700 flex items-center gap-1">
      <TrendIcon trend={c.trend} good={c.trend === goodWhen} />
      {c.trend === 'same'
        ? t(monthly ? 'report.sameAsLastMonth' : 'report.sameAsLastWeek')
        : t(monthly ? 'report.vsLastMonth' : 'report.vsLastWeek', { value: formatScore(c.previous, locale) })}
    </dd>}
  </div>;
}

function Highlights({ report }: { report: Report }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const days = highlightDays(report);
  if (days.length === 0) return null;
  return <section aria-labelledby="report-highlights">
    <h2 id="report-highlights" className="font-space text-lg font-extrabold mb-3">{t('report.highlights')}</h2>
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
