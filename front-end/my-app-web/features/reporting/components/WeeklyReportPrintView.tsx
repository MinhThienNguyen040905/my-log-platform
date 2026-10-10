'use client';

import { useQuery } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import Link from 'next/link';
import { getReport, type Report } from '../api/reports';
import { downloadReportCsv } from '../utils/download-report-csv';
import { WeeklyEvidenceTimelineChart } from './WeeklyEvidenceTimelineChart';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';

function metric(report: Report, key: string, locale: string, noData: string, suffix = ''): string {
  const value = report.metrics[key];
  return typeof value === 'number' && Number.isFinite(value) ? `${new Intl.NumberFormat(locale === 'en' ? 'en-US' : 'vi-VN', { maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(value)}${suffix}` : noData;
}

export function WeeklyReportPrintView({ id }: { id: string }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const { userId } = useJournal();
  const reportQuery = useQuery({
    queryKey: ['report', userId, id],
    queryFn: () => getReport(id),
    refetchInterval: (query) => query.state.data && ['PENDING', 'PROCESSING'].includes(query.state.data.status) ? 5000 : false,
  });
  const report = reportQuery.data?.type === 'WEEKLY' ? reportQuery.data : null;
  const loading = reportQuery.isPending;
  const error = reportQuery.data && reportQuery.data.type !== 'WEEKLY'
    ? t('report.wrongType')
    : reportQuery.error instanceof Error ? (locale === 'en' ? t('status.reportError') : reportQuery.error.message) : null;

  return <div className="mx-auto max-w-4xl px-4 py-8">
    <div className="no-print mb-6 flex flex-wrap items-center justify-between gap-3">
      <Link href="/insight" className="text-sm font-bold underline">{t('report.back')}</Link>
      {report?.status === 'READY' && <div className="flex flex-wrap gap-2">
        <button type="button" onClick={() => downloadReportCsv(report)} className="rounded-lg border-2 border-black bg-white px-4 py-2 text-sm font-bold">{t('report.csv')}</button>
        <button type="button" onClick={() => window.print()} className="rounded-lg border-2 border-black bg-primary-container px-4 py-2 text-sm font-bold">{t('report.print')}</button>
      </div>}
    </div>
    {loading && <p role="status">{t('status.loadingReport')}</p>}
    {error && <p role="alert" className="text-red-700">{error} <button type="button" className="underline" onClick={() => void reportQuery.refetch()}>{t('common.retry')}</button></p>}
    {report && <article className="print-container rounded-2xl border-2 border-black bg-white p-6 sm:p-10">
      <h1 className="font-space text-2xl font-extrabold">{t('report.title')}</h1>
      <p className="mt-2 text-sm">{t('report.from')} {report.from} {t('report.to')} {report.to} · {t('report.timezone')} {report.timezone}</p>
      <p className="text-sm">{t('report.status')} {report.status} · {t('report.sample')} {report.sampleSize} {t('report.days')}</p>
      {report.status === 'READY' ? <>
        <dl className="my-6 grid grid-cols-2 gap-4 border-y border-black/30 py-4 text-sm sm:grid-cols-4">
          <div><dt>{t('report.mood')}</dt><dd className="font-bold">{metric(report, 'averageMood', locale, t('report.noData'), '/10')}</dd></div>
          <div><dt>{t('report.stress')}</dt><dd className="font-bold">{metric(report, 'averageStress', locale, t('report.noData'), '/10')}</dd></div>
          <div><dt>{t('report.energy')}</dt><dd className="font-bold">{metric(report, 'averageEnergy', locale, t('report.noData'), '/10')}</dd></div>
          <div><dt>{t('report.sleep')}</dt><dd className="font-bold">{metric(report, 'averageSleepMinutes', locale, t('report.noData'), t('report.minutes'))}</dd></div>
        </dl>
        {report.evidence && report.evidence.length > 0 && (
          <WeeklyEvidenceTimelineChart evidence={report.evidence} />
        )}
        {report.narrative && <p className="mb-5 leading-relaxed">{report.narrative}</p>}
        <p className="text-xs text-gray-600">{t('report.disclaimer')}</p>
      </> : <p className="mt-6">{report.status === 'FAILED' ? t('report.failed') : t('report.processing')}</p>}
    </article>}
  </div>;
}
