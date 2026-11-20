'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Brain, Check, EyeOff, Info, ListChecks, Power, Quote, RefreshCw, X } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { formatDate, formatDateRange } from '@/lib/date/format-date';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { getConsents, setConsent } from '@/features/user';
import {
  THINKING_PATTERNS_CONSENT, hideThinkingPattern, listThinkingPatterns, refreshThinkingPatterns, sendPatternFeedback,
  startPatternPlan, type NotReallyReason, type ThinkingPattern,
} from '../api/thinking-patterns';

const REASONS: NotReallyReason[] = ['NOT_LIKE_ME', 'DONT_WANT_TO_SEE', 'WRONG_QUOTE'];

/**
 * "Điều mình nhận ra": thinking-pattern cards (THINKING_PATTERN_RECOGNITION_PLAN.md §9). Off until the user opts
 * in; every card asks the user to confirm instead of asserting, and can be hidden without a second question.
 */
export function ThinkingPatternsPanel({ userId }: { userId: string | null }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const patternsKey = ['thinkingPatterns', userId] as const;
  const consents = useQuery({ queryKey: ['thinkingPatterns', userId, 'consents'], queryFn: getConsents });
  const consent = consents.data?.find((c) => c.type === THINKING_PATTERNS_CONSENT);
  const enabled = consent?.granted === true;
  const aiEnabled = consents.data?.find((c) => c.type === 'AI_PROCESSING')?.granted === true;
  const patterns = useQuery({ queryKey: patternsKey, queryFn: listThinkingPatterns, enabled });

  const decide = useMutation({
    mutationFn: (granted: boolean) => setConsent(THINKING_PATTERNS_CONSENT, consent?.documentVersion ?? 'v1', granted),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['thinkingPatterns', userId] }),
  });
  const refresh = useMutation({
    mutationFn: refreshThinkingPatterns,
    onSuccess: (items) => queryClient.setQueryData(patternsKey, items),
  });
  const items = patterns.data ?? [];

  return (
    <section className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative flex flex-col gap-4" aria-labelledby="thinking-patterns-title">
      <WashiTape color="lime" rotate={2} className="absolute -top-3 right-8 w-24" />
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b-2 border-black">
        <div className="flex items-center gap-2">
          <Brain className="w-5 h-5 text-primary" aria-hidden="true" />
          <h2 id="thinking-patterns-title" className="font-space text-lg font-extrabold text-on-surface">{t('thinking.title')}</h2>
        </div>
        {enabled && (
          <button type="button" onClick={() => refresh.mutate()} disabled={refresh.isPending}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-black bg-primary-container shadow-neo-sm font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer">
            <RefreshCw className={`w-3.5 h-3.5 ${refresh.isPending ? 'animate-spin' : ''}`} aria-hidden="true" />
            {refresh.isPending ? t('thinking.refreshing') : t('thinking.refresh')}
          </button>
        )}
      </div>
      <p className="font-sans text-xs text-on-surface-variant">{t('thinking.description')}</p>

      {consents.isPending && <p role="status" className="text-sm">{t('common.loading')}</p>}
      {consents.isError && (
        <p role="alert" className="text-sm">{t('thinking.loadError')}{' '}
          <button type="button" className="underline" onClick={() => void consents.refetch()}>{t('common.retry')}</button>
        </p>
      )}

      {consents.isSuccess && !enabled && (
        <div className="p-4 bg-paper-warm rounded-2xl border border-dashed border-black flex flex-col gap-3 text-sm">
          <strong className="font-space">{t('thinking.introTitle')}</strong>
          <p>{t('thinking.introBody')}</p>
          {!aiEnabled && <p className="text-xs text-on-surface-variant flex gap-1.5"><Info className="w-4 h-4 shrink-0" aria-hidden="true" />{t('thinking.needsAi')}</p>}
          {decide.isError && <p role="alert" className="text-xs text-red-700">{t('thinking.enableError')}</p>}
          <button type="button" onClick={() => decide.mutate(true)} disabled={decide.isPending}
            className="self-start inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-black bg-primary-container shadow-neo-sm font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer">
            <Power className="w-3.5 h-3.5" aria-hidden="true" />
            {decide.isPending ? t('thinking.enabling') : t('thinking.enable')}
          </button>
        </div>
      )}

      {enabled && (
        <>
          {refresh.isError && <p role="alert" className="text-sm text-red-700">{t('thinking.refreshError')}</p>}
          {patterns.isPending && <p role="status" className="text-sm">{t('common.loading')}</p>}
          {patterns.isError && (
            <p role="alert" className="text-sm">{t('thinking.loadError')}{' '}
              <button type="button" className="underline" onClick={() => void patterns.refetch()}>{t('common.retry')}</button>
            </p>
          )}
          {patterns.isSuccess && items.length === 0 && (
            <div className="p-4 bg-paper-warm rounded-2xl border border-dashed border-black text-sm flex flex-col gap-1">
              <strong className="font-space">{t('thinking.emptyTitle')}</strong>
              <span>{t('thinking.emptyDescription')}</span>
            </div>
          )}
          {items.map((pattern) => <PatternCard key={pattern.id} pattern={pattern} userId={userId} />)}
          <button type="button" onClick={() => decide.mutate(false)} disabled={decide.isPending}
            className="self-start inline-flex items-center gap-1 text-xs underline text-on-surface-variant disabled:opacity-60 cursor-pointer">
            <Power className="w-3.5 h-3.5" aria-hidden="true" />{t('thinking.disable')}
          </button>
        </>
      )}
    </section>
  );
}

function PatternCard({ pattern, userId }: { pattern: ThinkingPattern; userId: string | null }) {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const queryClient = useQueryClient();
  const [choosingReason, setChoosingReason] = useState(false);
  const refetch = () => queryClient.invalidateQueries({ queryKey: ['thinkingPatterns', userId] });
  const feedback = useMutation({
    mutationFn: ({ value, reason }: { value: 'CONFIRMED' | 'NOT_REALLY'; reason?: NotReallyReason }) =>
      sendPatternFeedback(pattern.id, value, reason),
    onSuccess: refetch,
  });
  const hide = useMutation({ mutationFn: () => hideThinkingPattern(pattern.id), onSuccess: refetch });
  const plan = useMutation({
    mutationFn: () => startPatternPlan(pattern.id),
    onSuccess: () => Promise.all([refetch(), queryClient.invalidateQueries({ queryKey: ['selfCare', userId] })]),
  });
  const busy = feedback.isPending || hide.isPending;
  const confirmed = pattern.feedback === 'CONFIRMED';

  return (
    <article className="p-4 bg-paper-warm rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-space text-sm font-bold">{t('thinking.cardTitle', { name: pattern.name })}</h3>
        {!pattern.contentReviewed && (
          <span className="px-2 py-0.5 rounded border border-black bg-white font-space text-[10px] font-bold" title={t('thinking.draftHint')}>
            {t('thinking.draftBadge')}
          </span>
        )}
      </div>

      {pattern.quotes.length > 0 && (
        <figure className="flex flex-col gap-1.5">
          <figcaption className="text-[11px] font-space font-bold">{t('thinking.quotesLabel')}</figcaption>
          {pattern.quotes.map((quote) => (
            <blockquote key={`${quote.date}-${quote.text}`} className="bg-white rounded-xl border border-black px-3 py-2 text-sm italic flex gap-2">
              <Quote className="w-3.5 h-3.5 shrink-0 mt-0.5" aria-hidden="true" />
              <span>{quote.text} <span className="not-italic text-xs text-gray-600">({formatDate(quote.date, locale)})</span></span>
            </blockquote>
          ))}
        </figure>
      )}

      <p className="font-sans text-sm leading-relaxed">{pattern.narrative}</p>
      <p className="text-xs text-gray-600">
        {t('thinking.basis', { entries: pattern.entryCount, weeks: pattern.weekCount, range: formatDateRange(pattern.from, pattern.to, locale) })}
      </p>

      {!confirmed && !choosingReason && (
        <div className="flex flex-wrap gap-2">
          <button type="button" disabled={busy} onClick={() => feedback.mutate({ value: 'CONFIRMED' })}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-black bg-primary-container shadow-neo-sm font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer">
            <Check className="w-3.5 h-3.5" aria-hidden="true" />{t('thinking.confirm')}
          </button>
          <button type="button" disabled={busy} onClick={() => setChoosingReason(true)}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-black bg-white font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer">
            <X className="w-3.5 h-3.5" aria-hidden="true" />{t('thinking.notReally')}
          </button>
          <button type="button" disabled={busy} onClick={() => hide.mutate()}
            className="inline-flex items-center gap-1 text-xs font-bold underline disabled:opacity-60 cursor-pointer">
            <EyeOff className="w-3.5 h-3.5" aria-hidden="true" />{t('thinking.hide')}
          </button>
        </div>
      )}

      {choosingReason && (
        <fieldset className="flex flex-col gap-2" disabled={busy}>
          <legend className="text-xs font-bold mb-1">{t('thinking.reasonTitle')}</legend>
          <div className="flex flex-wrap gap-2">
            {REASONS.map((reason) => (
              <button key={reason} type="button" onClick={() => feedback.mutate({ value: 'NOT_REALLY', reason })}
                className="px-3 py-1.5 rounded-xl border border-black bg-white text-xs font-bold cursor-pointer disabled:opacity-60">
                {t(`thinking.reasons.${reason}`)}
              </button>
            ))}
            <button type="button" onClick={() => feedback.mutate({ value: 'NOT_REALLY' })}
              className="px-3 py-1.5 text-xs underline cursor-pointer disabled:opacity-60">{t('thinking.skipReason')}</button>
          </div>
        </fieldset>
      )}

      {(feedback.isError || hide.isError) && <p role="alert" className="text-xs text-red-700">{t('thinking.feedbackError')}</p>}

      {confirmed && (
        <div className="bg-white rounded-xl border border-black p-3 flex flex-col gap-2">
          <p className="font-space text-xs font-bold flex items-center gap-1.5">
            <ListChecks className="w-4 h-4" aria-hidden="true" />{t('thinking.stepsTitle')}
          </p>
          <ol className="flex flex-col gap-1.5 text-sm">
            {pattern.steps.map((step, index) => (
              <li key={step}><strong className="font-space text-xs">{t('thinking.week', { n: index + 1 })}:</strong> {step}</li>
            ))}
          </ol>
          {pattern.planGoalId ? (
            <p className="text-xs text-green-800 font-bold">{t('thinking.planRunning')}</p>
          ) : (
            <button type="button" onClick={() => plan.mutate()} disabled={plan.isPending}
              className="self-start inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-black bg-primary-container shadow-neo-sm font-space text-xs font-extrabold disabled:opacity-60 cursor-pointer">
              {plan.isPending ? t('thinking.startingPlan') : t('thinking.startPlan')}
            </button>
          )}
          {plan.isError && <p role="alert" className="text-xs text-red-700">{t('thinking.planError')}</p>}
        </div>
      )}
    </article>
  );
}
