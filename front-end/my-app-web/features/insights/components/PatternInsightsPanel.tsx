'use client';

import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { CalendarDays, EyeOff, HeartPulse, Layers, Moon, RefreshCw, Sparkles, Tag, TrendingUp, Footprints, BarChart3 } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { hideInsight, refreshInsights, type Insight } from '../api/insights';
import { summarizeEvidence } from '../utils/pattern-insights';

const TYPE_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  WEEKDAY_PATTERN: CalendarDays,
  MOOD_TREND_SHIFT: TrendingUp,
  EMOTION_TOPIC_COOCCURRENCE: HeartPulse,
  TAG_MOOD_ASSOCIATION: Tag,
  ACTIVITY_MOOD_ASSOCIATION: Footprints,
  SLEEP_MOOD_ASSOCIATION: Moon,
};

type Props = { insights: Insight[]; loading: boolean; error: boolean; onRetry: () => void };

/** Last-30-day pattern insights with refresh, evidence and hide controls. */
export function PatternInsightsPanel({ insights, loading, error, onRetry }: Props) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const refresh = useMutation({
    mutationFn: refreshInsights,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['insights'] }),
  });

  return (
    <section className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative flex flex-col gap-4" aria-labelledby="pattern-insights-title">
      <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-8 w-28" />
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b-2 border-black">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 id="pattern-insights-title" className="font-space text-lg font-extrabold text-on-surface">{t('pattern.title')}</h2>
        </div>
        <button
          type="button"
          onClick={() => refresh.mutate()}
          disabled={refresh.isPending}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-black bg-primary-container shadow-neo-sm font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refresh.isPending ? 'animate-spin' : ''}`} />
          {refresh.isPending ? t('pattern.refreshing') : t('pattern.refresh')}
        </button>
      </div>
      <p className="font-sans text-xs text-on-surface-variant">{t('pattern.description')}</p>

      {refresh.isError && <p role="alert" className="text-sm text-red-700">{t('pattern.refreshError')}</p>}
      {loading && <p role="status">{t('status.loadingAnalysis')}</p>}
      {error && <p role="alert">{t('insight.insightError')} <button type="button" className="underline" onClick={onRetry}>{t('common.retry')}</button></p>}
      {!loading && !error && insights.length === 0 && (
        <div className="p-4 bg-paper-warm rounded-2xl border border-dashed border-black text-sm flex flex-col gap-1">
          <strong className="font-space">{t('pattern.emptyTitle')}</strong>
          <span>{t('pattern.emptyDescription')}</span>
        </div>
      )}
      {insights.map((insight) => <PatternCard key={insight.id} insight={insight} />)}
    </section>
  );
}

function PatternCard({ insight }: { insight: Insight }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [showEvidence, setShowEvidence] = useState(false);
  const hide = useMutation({
    mutationFn: () => hideInsight(insight.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['insights'] }),
  });
  const Icon = TYPE_ICONS[insight.type] ?? Layers;
  // Emotion/tag patterns count journal entries; the others count days.
  const sampleKey = insight.type === 'EMOTION_TOPIC_COOCCURRENCE' || insight.type === 'TAG_MOOD_ASSOCIATION'
    ? 'pattern.sampleEntries' : 'pattern.sampleDays';

  return (
    <article className="p-4 bg-paper-warm rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2">
        <h3 className="font-space text-sm font-bold flex items-center gap-2">
          <Icon className="w-4 h-4" />
          {t(`pattern.types.${insight.type}`, { defaultValue: t('pattern.types.default') })}
        </h3>
        {insight.generatedBy === 'AI_ASSISTED' && (
          <span className="px-2 py-0.5 rounded border border-black bg-white font-space text-[10px] font-bold" title={t('pattern.aiWordingHint')}>
            {t('pattern.aiWording')}
          </span>
        )}
      </div>
      <p className="font-sans text-sm leading-relaxed">{insight.narrative}</p>
      <p className="font-mono text-xs text-gray-600">
        {t(sampleKey, { count: insight.sampleSize })} · {insight.from} – {insight.to}
      </p>
      <div className="flex flex-wrap gap-3">
        <button type="button" className="inline-flex items-center gap-1 text-xs font-bold underline cursor-pointer"
          aria-expanded={showEvidence} onClick={() => setShowEvidence((v) => !v)}>
          <BarChart3 className="w-3.5 h-3.5" />
          {showEvidence ? t('pattern.hideEvidence') : t('pattern.showEvidence')}
        </button>
        <button type="button" className="inline-flex items-center gap-1 text-xs font-bold underline cursor-pointer disabled:opacity-60"
          disabled={hide.isPending} onClick={() => hide.mutate()}>
          <EyeOff className="w-3.5 h-3.5" />
          {t('pattern.hide')}
        </button>
      </div>
      {hide.isError && <p role="alert" className="text-xs text-red-700">{t('pattern.hideError')}</p>}
      {showEvidence && <EvidenceChart insight={insight} />}
    </article>
  );
}

function EvidenceChart({ insight }: { insight: Insight }) {
  const { t } = useTranslation();
  const summary = summarizeEvidence(insight.evidence);
  if (!summary) return <p className="text-xs">{t('pattern.noEvidence')}</p>;
  if (summary.kind === 'share') {
    return (
      <p className="text-xs font-sans bg-white rounded-xl border border-black p-3">
        {t('pattern.shareEvidence', { present: summary.present, total: summary.total })}
      </p>
    );
  }
  return (
    <figure className="bg-white rounded-xl border border-black p-3 flex flex-col gap-2">
      <figcaption className="text-[11px] font-space font-bold">{t(`pattern.metrics.${summary.metric}`, { defaultValue: summary.metric })}</figcaption>
      <div className="flex items-end gap-1 h-20" role="img" aria-label={summary.bars.map((b) => `${b.date}: ${b.value}`).join(', ')}>
        {summary.bars.map((bar) => (
          <div key={bar.date} className="flex-1 min-w-0 flex flex-col items-center justify-end h-full" title={`${bar.date}: ${bar.value}`}>
            <div className="w-full bg-primary-container border border-black rounded-t" style={{ height: `${bar.ratio * 100}%` }} />
          </div>
        ))}
      </div>
      <div className="flex justify-between font-mono text-[10px] text-gray-600">
        <span>{summary.bars[0]?.date}</span>
        <span>{summary.bars[summary.bars.length - 1]?.date}</span>
      </div>
    </figure>
  );
}
