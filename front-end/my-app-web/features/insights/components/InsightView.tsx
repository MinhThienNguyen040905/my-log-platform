'use client';

import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useJournal } from '@/features/journal';
import { useSelfCareGoals } from '@/features/selfcare';
import { listInsights } from '../api/insights';
import { listReports } from '@/features/reporting';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useTranslation } from 'react-i18next';
import { motion } from 'motion/react';
import {
  Sparkles,
  CheckSquare,
  ShieldCheck,
  Sprout,
  BarChart3,
  Layers,
  Lightbulb,
  FileText,
} from 'lucide-react';

export function InsightView() {
  const { t } = useTranslation();
  const { entries, userId } = useJournal();
  const { goals, loading: goalsLoading, error: goalsError, toggleGoal, refetch: refetchGoals } = useSelfCareGoals();

  const [activeTab, setActiveTab] = useState<'weekly-report' | 'evidence-matrix'>('weekly-report');
  const insightsQuery = useQuery({ queryKey: ['insights', userId, 20], queryFn: () => listInsights({ limit: 20 }) });
  const reportsQuery = useQuery({ queryKey: ['reports', userId, 'WEEKLY', 20], queryFn: () => listReports({ type: 'WEEKLY', limit: 20 }) });
  const insights = insightsQuery.data?.items ?? [];
  const reports = reportsQuery.data?.items ?? [];
  const insightsLoading = insightsQuery.isPending;
  const reportsLoading = reportsQuery.isPending;
  const insightsError = insightsQuery.isError;
  const reportsError = reportsQuery.isError;
  const reportWeek = useMemo(() => {
    const today = new Date();
    const utc = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()));
    utc.setUTCDate(utc.getUTCDate() + 4 - (utc.getUTCDay() || 7));
    const year = utc.getUTCFullYear();
    const week = Math.ceil((((utc.getTime() - Date.UTC(year, 0, 1)) / 86400000) + 1) / 7);
    return t('insight.week', { week, year });
  }, [t]);

  // [FR-INSIGHT-01] & [BR-08]: Minimum 3 distinct days required for insight engine
  const distinctDays = useMemo(() => {
    return new Set(entries.map((e) => e.date)).size;
  }, [entries]);

  const effectiveDistinctDays = distinctDays;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 selection:bg-primary-container selection:text-black">
      {/* Top Header & Tab Switcher */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div>
          <div className="inline-flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 bg-paper-warm border border-black rounded text-[11px] font-space font-extrabold uppercase">
              FR-REPORT & FR-INSIGHT
            </span>
            <span className="text-xs font-space text-gray-500 font-bold">{t('insight.period')} {reportWeek}</span>
          </div>
          <h1 className="font-space text-2xl sm:text-4xl font-extrabold text-on-surface">
            {t('insight.title')}
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            {t('insight.description')}
          </p>
        </div>

        {/* Action Controls & Tab Navigation Switcher */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 p-1.5 bg-surface-card rounded-2xl border-neo shadow-neo-sm">
            <button
              type="button"
              onClick={() => setActiveTab('weekly-report')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                activeTab === 'weekly-report'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm font-extrabold'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              {t('insight.weeklyTab')}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('evidence-matrix')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                activeTab === 'evidence-matrix'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm font-extrabold'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              {t('insight.evidenceTab')}
            </button>
          </div>
        </div>
      </div>

      {/* [BR-08] & [FR-INSIGHT-01]: Insufficient Data Empty State */}
      {effectiveDistinctDays < 3 && reports.length === 0 && insights.length === 0 && !reportsLoading && !insightsLoading && !reportsError && !insightsError ? (
        <div className="bg-surface-card border-neo rounded-3xl p-8 sm:p-12 shadow-neo relative flex flex-col items-center text-center gap-6 max-w-3xl mx-auto my-6">
          <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-12 w-36" />
          <div className="w-20 h-20 rounded-3xl bg-paper-warm border-2 border-black flex items-center justify-center shadow-neo">
            <Sprout className="w-10 h-10 text-green-700 stroke-[2.3]" />
          </div>

          <div className="flex flex-col gap-2">
            <span className="px-3 py-1 bg-amber-200 text-black border border-black rounded-lg font-space text-xs font-extrabold uppercase tracking-wide self-center shadow-neo-sm">
              {t('insight.minimum')}
            </span>
            <h2 className="font-space text-2xl sm:text-3xl font-extrabold text-on-surface">
              {t('insight.minimumTitle')}
            </h2>
            <p className="font-sans text-sm text-gray-700 max-w-lg leading-relaxed">
              {t('insight.minimumDescription')}
            </p>
          </div>

          {/* 3-day Progress visualization */}
          <div className="w-full max-w-md bg-paper-warm p-4 rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-3">
            <div className="flex items-center justify-between text-xs font-space font-bold">
              <span>{t('insight.progress')}</span>
              <span className="font-extrabold font-mono text-sm">{t('insight.dayProgress', { count: effectiveDistinctDays, percent: Math.round((effectiveDistinctDays / 3) * 100) })}</span>
            </div>
            <div className="w-full h-3.5 bg-white rounded-full border-2 border-black overflow-hidden p-0.5">
              <div
                style={{ width: `${Math.min(100, (effectiveDistinctDays / 3) * 100)}%` }}
                className="h-full bg-primary-container rounded-full transition-all duration-500 border border-black"
              />
            </div>

            <div className="grid grid-cols-3 gap-2 pt-1">
              {[1, 2, 3].map((d) => {
                const isDone = d <= effectiveDistinctDays;
                return (
                  <div
                    key={d}
                    className={`p-2 rounded-xl border flex flex-col items-center justify-center gap-1 text-xs font-space font-bold ${
                      isDone
                        ? 'bg-primary-container border-black text-black shadow-neo-sm'
                        : 'bg-white border-dashed border-gray-400 text-gray-400'
                    }`}
                  >
                    <span className="text-[10px] uppercase font-extrabold">{t('insight.day', { day: d })}</span>
                    <span className="text-xs">{isDone ? t('insight.done') : t('insight.missing')}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <Link href="/journal-editor">
            <NeoButton size="md" className="font-space font-extrabold" icon={<Sparkles className="w-4 h-4" />}>
              {t('insight.write')}
            </NeoButton>
          </Link>
        </div>
      ) : (
        <>

      {/* ========================================================== */}
      {/* TAB 1: WEEKLY SNAPSHOT REPORT (FR-REPORT-01..05)          */}
      {/* ========================================================== */}
      {activeTab === 'weekly-report' && (
        <motion.section
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className="bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo flex flex-col gap-5"
        >
          <div className="flex items-center gap-3 border-b-2 border-black pb-3">
            <BarChart3 className="w-6 h-6 text-primary" />
            <h2 className="font-space text-xl font-extrabold">{t('insight.reports')}</h2>
          </div>
          {reportsLoading && <p role="status">{t('status.loadingReport')}</p>}
          {reportsError && <p role="alert">{t('insight.reportError')} <button type="button" className="underline" onClick={() => void reportsQuery.refetch()}>{t('common.retry')}</button></p>}
          {!reportsLoading && !reportsError && reports.length === 0 && (
            <p>{t('insight.noReports')}</p>
          )}
          {reports.map((report) => (
            <article key={report.id} className="p-4 bg-paper-warm rounded-2xl border-neo-sm flex flex-col gap-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-space text-sm font-bold">{report.type} · {report.from} – {report.to}</h3>
                <span className="font-mono text-xs">{report.status}</span>
              </div>
              <p className="text-xs">{t('insight.sample', { count: report.sampleSize })}</p>
              {report.narrative && <p className="font-serif text-sm leading-relaxed">{report.narrative}</p>}
              {report.status === 'READY' && <Link href={`/reports/${report.id}/print`} className="self-start text-sm font-bold underline">{t('insight.reportAction')}</Link>}
            </article>
          ))}
        </motion.section>
      )}

      {/* ========================================================== */}
      {/* ========================================================== */}
      {/* TAB 2: EVIDENCE MATRIX & HABITS (FR-INSIGHT-01..07)        */}
      {/* ========================================================== */}
      {activeTab === 'evidence-matrix' && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start"
        >
          {/* LEFT COLUMN: Evidence-Based Insights (7 cols) */}
          <div className="lg:col-span-7 flex flex-col gap-6">
            {/* EVIDENCE MATRIX HEADER CARD */}
            <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
              <WashiTape color="peach" rotate={2} className="absolute -top-3 right-8 w-28" />

              <div className="flex items-center justify-between pb-3 border-b-2 border-black mb-4">
                <div className="flex items-center gap-2">
                  <span className="w-3.5 h-3.5 rounded-full bg-primary-container border-neo-sm"></span>
                  <h2 className="font-space text-lg font-extrabold text-on-surface">
                    {t('insight.evidence')}
                  </h2>
                </div>
                <span className="font-space text-[10px] bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                  {t('insight.evidenceOnly')}
                </span>
              </div>

              {/* No Causation Disclaimer Alert (FR-INSIGHT-03) */}
              <div className="p-3 bg-blue-50 border border-blue-300 rounded-xl text-xs font-sans text-blue-900 mb-4 flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-700 shrink-0 mt-0.5" />
                <span>
                  <strong>{t('insight.science')}</strong> {t('insight.scienceDescription')}
                </span>
              </div>

              {/* Evidence Cards List */}
              <div className="flex flex-col gap-4">
                {insightsLoading && <p role="status">{t('status.loadingAnalysis')}</p>}
                {insightsError && <p role="alert">{t('insight.insightError')} <button type="button" className="underline" onClick={() => void insightsQuery.refetch()}>{t('common.retry')}</button></p>}
                {!insightsLoading && !insightsError && insights.length === 0 && (
                  <p>{t('insight.noInsights')}</p>
                )}
                {insights.map((insight) => (
                  <article key={insight.id} className="p-4 bg-paper-warm rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-3">
                    <h3 className="font-space text-sm font-bold">{insight.type}</h3>
                    <p className="font-sans text-sm">{insight.narrative}</p>
                    <p className="font-mono text-xs">{t('insight.sample', { count: insight.sampleSize })} · {t('insight.range')} {insight.from} – {insight.to}</p>
                  </article>
                ))}
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: Micro-Action & Wellness Habit Checklist (5 cols) */}
          <div className="lg:col-span-5 flex flex-col gap-6">
            <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
              <WashiTape color="lavender" rotate={-2} className="absolute -top-3 left-6 w-28" />

              <div className="flex items-center justify-between pb-3 border-b-2 border-black mb-4">
                <div className="flex items-center gap-2">
                  <Sprout className="w-5 h-5 text-primary" />
                  <h2 className="font-space text-lg font-extrabold text-on-surface">
                    {t('insight.goals')}
                  </h2>
                </div>
                <span className="font-space text-xs font-bold text-gray-500">
                  {t('insight.achieved', { done: goals.filter((g) => g.status === 'COMPLETED').length, total: goals.length })}
                </span>
              </div>

              <div className="flex flex-col gap-3">
                {goalsLoading && <p role="status" className="text-sm">{t('status.loadingGoals')}</p>}
                {goalsError && <p role="alert" className="text-sm text-red-700">{t('status.goalsError')} <button type="button" className="underline" onClick={() => void refetchGoals()}>{t('common.retry')}</button></p>}
                {!goalsLoading && !goalsError && goals.length === 0 && <p className="text-sm">{t('status.noGoals')}</p>}
                {goals.map((goal) => (
                  <button
                    key={goal.id}
                    type="button"
                    onClick={() => void toggleGoal(goal)}
                    className={`w-full text-left p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-start gap-3 transition-all cursor-pointer ${
                      goal.status === 'COMPLETED' ? 'bg-paper-warm opacity-85' : 'bg-surface-container-lowest hover:bg-white'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded border-2 border-black flex items-center justify-center shrink-0 mt-0.5 ${
                        goal.status === 'COMPLETED' ? 'bg-primary-container' : 'bg-white'
                      }`}
                    >
                      {goal.status === 'COMPLETED' && <CheckSquare className="w-3.5 h-3.5 text-black" />}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <h4
                          className={`font-space text-xs sm:text-sm font-bold text-black ${
                            goal.status === 'COMPLETED' ? 'line-through opacity-70' : ''
                          }`}
                        >
                          {goal.title}
                        </h4>
                        <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/10 font-bold">
                          {goal.status}
                        </span>
                      </div>
                      <p className="font-sans text-xs text-gray-600 mt-1">
                        {goal.description}
                      </p>
                    </div>
                  </button>
                ))}
              </div>

              <div className="mt-5 p-3.5 bg-paper-warm rounded-2xl border-neo-sm">
                <span className="font-space text-xs font-extrabold uppercase tracking-wider text-black mb-1 flex items-center gap-1.5">
                  <Lightbulb className="w-3.5 h-3.5 text-amber-600 fill-amber-300 stroke-[2.3]" />
                  {t('insight.microAction')}
                </span>
                <p className="font-sans text-xs text-gray-700 leading-relaxed">
                  {t('insight.microActionDescription')}
                </p>
              </div>
            </div>
          </div>
        </motion.div>
      )}
      </>
      )}
    </div>
  );
}
