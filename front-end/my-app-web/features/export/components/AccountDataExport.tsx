'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authorizeExportDownload, createExport, downloadExport, getExport, type ExportJob } from '../api/exports';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';

export function AccountDataExport({ userId }: { userId: string }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const client = useQueryClient();
  const [jobId, setJobId] = useState<string | null>(null);
  const [restoring, setRestoring] = useState(true);
  const [pollUntil, setPollUntil] = useState(0);
  const [format, setFormat] = useState<'CSV' | 'PDF'>('CSV');
  const [password, setPassword] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const storageKey = `mylog-export-job:${userId}`;

  useEffect(() => {
    let id: string | null = null;
    try { id = window.sessionStorage.getItem(storageKey); } catch { /* Storage is optional. */ }
    queueMicrotask(() => { setJobId(id); setPollUntil(Date.now() + 120_000); setRestoring(false); });
  }, [storageKey]);

  const jobQuery = useQuery({
    queryKey: ['export', userId, jobId],
    queryFn: () => getExport(jobId!),
    enabled: !!jobId && !restoring,
    gcTime: 0,
    retry: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return jobId && Date.now() < pollUntil && status !== 'READY' && status !== 'EXPIRED' ? 5000 : false;
    },
  });
  const job = jobQuery.data;
  const create = useMutation({ mutationFn: createExport, onSuccess: (created: ExportJob) => {
    client.setQueryData(['export', userId, created.id], created);
    setJobId(created.id);
    setPollUntil(Date.now() + 120_000);
    try { window.sessionStorage.setItem(storageKey, created.id); } catch { /* Job remains usable here. */ }
    setMessage(t('export.requested'));
  } });
  const busy = create.isPending || downloading;
  const activeJob = restoring || (!!jobId && (!job || job.status !== 'EXPIRED'));

  const requestExport = async () => {
    if (busy || activeJob) return;
    setError(null); setMessage(null);
    try { await create.mutateAsync(format); }
    catch (cause) { setError(locale === 'en' ? t('export.createError') : cause instanceof Error ? cause.message : t('export.createError')); }
  };

  const refresh = async () => {
    if (!jobId || busy) return;
    setError(null);
    setPollUntil(Date.now() + 120_000);
    const result = await jobQuery.refetch();
    if (result.error) setError(locale === 'en' ? t('export.checkError') : result.error instanceof Error ? result.error.message : t('export.checkError'));
  };

  const saveFile = async () => {
    if (!job || job.status !== 'READY' || !password || busy) return;
    setDownloading(true); setError(null); setMessage(null);
    try {
      const grant = await authorizeExportDownload(job.id, password);
      setPassword('');
      const response = await downloadExport(job.id, grant);
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement('a');
      link.href = url;
      link.download = `mylog-export.${job.format.toLowerCase()}`;
      document.body.appendChild(link);
      link.click(); link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      setMessage(t('export.downloaded'));
    } catch (cause) {
      setPassword('');
      setError(locale === 'en' ? t('export.downloadError') : cause instanceof Error ? cause.message : t('export.downloadError'));
    } finally { setDownloading(false); }
  };

  return <section className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm space-y-4">
    <div><h2 className="font-space text-lg font-extrabold">{t('export.title')}</h2>
      <p className="text-sm text-gray-600">{t('export.description')}</p></div>
    <div className="flex flex-wrap items-center gap-3">
      <label htmlFor="account-export-format" className="text-sm font-bold">{t('export.format')}</label>
      <select id="account-export-format" value={activeJob && job ? job.format : format} disabled={busy || activeJob} onChange={(event) => setFormat(event.target.value as 'CSV' | 'PDF')} className="rounded-lg border border-black bg-white px-3 py-2 text-sm"><option value="CSV">CSV</option><option value="PDF">PDF</option></select>
      <button type="button" disabled={busy || activeJob} onClick={() => void requestExport()} className="rounded-lg border-2 border-black bg-primary-container px-4 py-2 text-sm font-bold disabled:opacity-50">{t('export.create')}</button>
    </div>
    {jobQuery.isPending && jobId && <p role="status" className="text-sm">{t('export.checking')}</p>}
    {job && <div className="space-y-3 rounded-xl border border-black/30 bg-paper-warm p-4 text-sm">
      <p role="status">{t('export.status')} <strong>{job.status === 'READY' ? t('export.ready') : job.status === 'EXPIRED' ? t('export.expired') : job.status === 'FAILED' ? t('export.failed') : t('export.processing')}</strong></p>
      {job.expiresAt && <p>{t('export.expires')} {new Date(job.expiresAt).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN')}</p>}
      {job.status !== 'EXPIRED' && <button type="button" disabled={busy || jobQuery.isFetching} onClick={() => void refresh()} className="text-sm font-bold underline disabled:opacity-50">{t('export.refresh')}</button>}
      {job.status === 'READY' && <div className="flex flex-wrap items-end gap-3"><label className="flex flex-col gap-1 font-bold">{t('export.password')}
        <input type="password" autoComplete="current-password" value={password} disabled={busy} onChange={(event) => setPassword(event.target.value)} className="rounded-lg border border-black bg-white px-3 py-2" /></label>
        <button type="button" disabled={busy || !password} onClick={() => void saveFile()} className="rounded-lg border-2 border-black bg-primary-container px-4 py-2 font-bold disabled:opacity-50">{t('export.download')}</button></div>}
    </div>}
    {(error || jobQuery.error) && <p role="alert" className="text-sm text-red-700">{error || (locale === 'en' ? t('export.checkError') : jobQuery.error instanceof Error ? jobQuery.error.message : t('export.checkError'))}</p>}
    {message && <p role="status" className="text-sm text-green-800">{message}</p>}
  </section>;
}
