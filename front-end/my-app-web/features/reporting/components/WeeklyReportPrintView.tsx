'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { RefreshCw } from 'lucide-react';
import { getRegenerationAllowance, getReport, regenerateReport } from '../api/reports';
import { downloadReportCsv } from '../utils/download-report-csv';
import { formatDay } from '../utils/report-highlights';
import { ReportDetailContent } from './ReportDetailContent';
import { useTranslation } from 'react-i18next';
import { formatDateRange } from '@/lib/date/format-date';
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
  // One regeneration per user per week: the button unlocks again next Monday (profile timezone).
  const allowanceQuery = useQuery({ queryKey: ['report-regeneration', userId], queryFn: getRegenerationAllowance });
  const regenerate = useMutation({
    mutationFn: () => regenerateReport(id),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: ['reports'] });
      void queryClient.invalidateQueries({ queryKey: ['report-regeneration', userId] });
      router.push(`/reports/${created.id}/print`);
    },
    onError: () => { void queryClient.invalidateQueries({ queryKey: ['report-regeneration', userId] }); },
  });
  const regenerationUsed = allowanceQuery.data?.available === false;
  const reopensOn = allowanceQuery.data ? formatDay(allowanceQuery.data.availableFrom, locale) : '';
  const report = reportQuery.data ?? null;
  const monthly = report?.type === 'MONTHLY';
  const error = reportQuery.error instanceof Error ? (locale === 'en' ? t('status.reportError') : reportQuery.error.message) : null;

  return <div className="mx-auto max-w-4xl px-4 py-8">
    <div className="no-print mb-6 flex flex-wrap items-center justify-between gap-3">
      <Link href="/insight" className="text-sm font-bold underline">{t('report.back')}</Link>
      {report?.status === 'READY' && <div className="flex flex-wrap gap-2">
        <button type="button" onClick={() => regenerate.mutate()}
          disabled={regenerate.isPending || !allowanceQuery.data || regenerationUsed}
          aria-describedby={regenerationUsed ? 'report-regenerate-used' : undefined}
          className="inline-flex items-center gap-1.5 rounded-lg border-2 border-black bg-white px-4 py-2 text-sm font-bold disabled:opacity-60 disabled:cursor-not-allowed">
          <RefreshCw className={`h-4 w-4 ${regenerate.isPending ? 'animate-spin' : ''}`} aria-hidden="true" />{t('report.regenerate')}
        </button>
        <button type="button" onClick={() => downloadReportCsv(report)} className="rounded-lg border-2 border-black bg-white px-4 py-2 text-sm font-bold">{t('report.csv')}</button>
        <button type="button" onClick={() => window.print()} className="rounded-lg border-2 border-black bg-primary-container px-4 py-2 text-sm font-bold">{t('report.print')}</button>
      </div>}
    </div>
    {report?.status === 'READY' && regenerationUsed && (
      <p id="report-regenerate-used" className="no-print mb-4 text-sm text-gray-700">{t('report.regenerateUsed', { date: reopensOn })}</p>
    )}
    {regenerate.isError && !regenerationUsed && <p role="alert" className="no-print mb-4 text-sm text-red-700">{t('report.regenerateError')}</p>}
    {reportQuery.isPending && <p role="status">{t('status.loadingReport')}</p>}
    {error && <p role="alert" className="text-red-700">{error} <button type="button" className="underline" onClick={() => void reportQuery.refetch()}>{t('common.retry')}</button></p>}
    {report && <article className="print-container rounded-2xl border-2 border-black bg-white p-6 sm:p-10 flex flex-col gap-6">
      <header>
        <h1 className="font-space text-2xl font-extrabold">{monthly ? t('report.titleMonthly') : t('report.title')}</h1>
        <p className="mt-2 text-sm">{formatDateRange(report.from, report.to, locale)} · {t('report.sample')} {report.sampleSize} {t('report.days')}
          {report.version > 1 && <> · {t('report.version', { version: report.version })}</>}</p>
      </header>
      {report.status === 'READY'
        ? <ReportDetailContent report={report} />
        : <p>{report.status === 'FAILED' ? t('report.failed') : t('report.processing')}</p>}
    </article>}
  </div>;
}
