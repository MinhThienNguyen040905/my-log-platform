'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useJournal, SafetyModal, getRecentEntries } from '@/features/journal';
import { useAccount } from '@/features/user';
import { INSIGHT_HISTORY_RANGE, INSIGHT_MIN_JOURNAL_DAYS, MascotPanel, toMascotChoice } from '@/features/insights';
import { HeroActions } from './HeroActions';
import { MetricCard } from './MetricCard';
import { metricTrend, type MetricKey } from '../utils/metric-trend';
import { getDashboard, type Dashboard } from '../api/dashboard';
import { MoodStressTrendChart } from './MoodStressTrendChart';
import { EmotionDistributionChart } from './EmotionDistributionChart';
import { moodColor, moodSpectrum, toneSpectrum, topicLabel } from '../utils/spectrum';
import { formatDate } from '@/lib/date/format-date';
import { useQuery } from '@tanstack/react-query';
import { WashiTape, PolaroidCard } from '@/components/ui/ScrapbookDecorations';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { motion } from 'motion/react';
import {
  Flame,
  Calendar,
  Smile,
  Activity,
  Zap,
  Moon,
  Sparkles,
  HeartHandshake,
} from 'lucide-react';

export function DashboardView() {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const { streakCount, userId } = useJournal();
  const { accountProfile, userProfile } = useAccount();
  const mascotHidden = toMascotChoice(userProfile.mascot) === 'off';
  const dashboardQuery = useQuery<Dashboard>({ queryKey: ['dashboard', userId, '30d'], queryFn: () => getDashboard('30d') });
  const recentQuery = useQuery({ queryKey: ['journal', userId, 'recent', 2], queryFn: () => getRecentEntries(2) });
  const recentEntries = recentQuery.data ?? [];
  const dashboard = dashboardQuery.data;
  const dashboardError = dashboardQuery.isError;
  const stats = dashboard?.timeline ?? [];
  const average = (key: 'moodScore' | 'stressScore' | 'energyScore' | 'sleepMinutes') => {
    const values = stats.map((day) => day[key]).filter((value): value is number => value !== null);
    return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : null;
  };
  const mood = average('moodScore');
  const stress = average('stressScore');
  const energy = average('energyScore');
  const sleep = average('sleepMinutes');
  const greetingName = accountProfile.displayName?.trim() || accountProfile.penName?.trim() || (locale === 'en' ? 'there' : 'bạn');
  // Same rule and cached query as the Insight page: hint only until weekly reflections unlock.
  const historyQuery = useQuery<Dashboard>({ queryKey: ['dashboard', userId, INSIGHT_HISTORY_RANGE], queryFn: () => getDashboard(INSIGHT_HISTORY_RANGE) });
  const journalDays = historyQuery.data?.timeline.filter((day) => day.journalCount > 0).length ?? 0;
  const daysToUnlock = historyQuery.isSuccess ? Math.max(0, INSIGHT_MIN_JOURNAL_DAYS - journalDays) : 0;
  const trend = (key: MetricKey) => historyQuery.data
    ? metricTrend(historyQuery.data.timeline, historyQuery.data.to, key)
    : { current: null, previous: null, direction: null, good: null };
  const score = (value: number | null): string | null => value === null ? null
    : value.toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

  const [trendView, setTrendView] = useState<'both' | 'mood' | 'stress'>('mood');
  const [isSafetyModalOpen, setIsSafetyModalOpen] = useState(false);
  const [todayLabel, setTodayLabel] = useState('');

  useEffect(() => {
    const updateToday = () => {
      setTodayLabel(new Intl.DateTimeFormat(locale === 'en' ? 'en-US' : 'vi-VN', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
        timeZone: accountProfile.timezone,
      }).format(new Date()));
    };
    const initialTimer = window.setTimeout(updateToday, 0);
    const refreshTimer = window.setInterval(updateToday, 60_000);
    return () => {
      window.clearTimeout(initialTimer);
      window.clearInterval(refreshTimer);
    };
  }, [accountProfile.timezone, locale]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 selection:bg-primary-container selection:text-black">
      {/* 0. Unlock hint for new users: hidden once weekly reflections are available. */}
      {daysToUnlock > 0 && <div data-testid="insight-unlock-hint" className="p-4 bg-paper-warm border-2 border-black rounded-2xl shadow-neo-sm flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-primary-container border border-black flex items-center justify-center font-bold text-black shrink-0">
            <Sparkles className="w-5 h-5 text-black" />
          </div>
          <div>
            <span className="font-space text-xs font-extrabold uppercase text-black block">
              {t('dashboard.progress')}
            </span>
            <p className="font-sans text-xs text-gray-700 mt-0.5">
              {t('dashboard.recorded', { count: daysToUnlock })}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Link
            href="/insight"
            className="text-xs font-space font-bold underline text-black hover:text-primary flex items-center gap-1"
          >
            {t('dashboard.evidence')}
          </Link>
        </div>
      </div>}

      {/* 1. HERO: greeting, one main action for today, and the mascot's 7-day message. */}
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
        className="relative w-full"
      >
        <WashiTape color="lavender" rotate={-2} className="absolute -top-3 left-10 w-36 z-20" />

        <div className="bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg relative overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-6 border-b-2 border-dashed border-border-soft pb-4">
            <span className="px-3 py-1 bg-paper-warm text-on-surface font-space text-xs sm:text-sm font-bold border-neo-sm rounded-lg shadow-neo-sm flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-on-surface stroke-[2.5]" aria-hidden="true" />
              {todayLabel || t('dashboard.loadingDate')}
            </span>
            <span className="inline-flex items-center gap-2 bg-mood-hope-energy text-black px-3 py-1 border-neo-sm rounded-lg shadow-neo-sm">
              <Flame className="w-4 h-4 text-orange-600 fill-orange-500 shrink-0" aria-hidden="true" />
              <span className="font-space text-xs sm:text-sm font-extrabold">{t('dashboard.streak', { count: streakCount })}</span>
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            <div className={`${mascotHidden ? 'lg:col-span-12' : 'lg:col-span-8'} flex flex-col gap-5`}>
              <h1 className="font-space text-2xl sm:text-4xl lg:text-5xl font-extrabold text-on-surface tracking-tight leading-tight">
                {t('dashboard.greeting', { name: greetingName })}{' '}
                <span className="bg-primary-container px-2.5 py-0.5 border-neo-sm rounded-lg shadow-neo-sm inline-block">
                  {t('dashboard.today')}
                </span>{' '}
                {t('dashboard.feeling')}
              </h1>
              <HeroActions userId={userId} timezone={accountProfile.timezone} />
            </div>
            {!mascotHidden && <MascotPanel className="lg:col-span-4 lg:border-l-2 lg:border-dashed lg:border-border-soft lg:pl-6" />}
          </div>

          {/* Four numbers for the last 30 days, each against the 30 days before. */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-8 pt-6 border-t-2 border-on-background">
            <MetricCard label={t('dashboard.moodAverage')} value={score(mood)} unit="/10" trend={trend('moodScore')} formatPrevious={(v) => score(v) ?? ''}
              icon={<Smile className="w-5 h-5 text-green-800 stroke-[2.5]" />} iconTone="bg-mood-calm-joy/30" />
            <MetricCard label={t('dashboard.stressAverage')} value={score(stress)} unit="/10" trend={trend('stressScore')} formatPrevious={(v) => score(v) ?? ''}
              icon={<Activity className="w-5 h-5 text-red-700 stroke-[2.5]" />} iconTone="bg-mood-anxiety-stress/20" />
            <MetricCard label={t('dashboard.energyAverage')} value={score(energy)} unit="/10" trend={trend('energyScore')} formatPrevious={(v) => score(v) ?? ''}
              icon={<Zap className="w-5 h-5 text-yellow-700 fill-yellow-500 stroke-[2.5]" />} iconTone="bg-mood-hope-energy/40" />
            <MetricCard label={t('dashboard.sleepAverage')} value={sleep === null ? null : score(sleep / 60)} unit={t('dashboard.hours')} trend={trend('sleepMinutes')}
              formatPrevious={(minutes) => `${score(minutes / 60)}${t('dashboard.hours')}`}
              icon={<Moon className="w-5 h-5 text-blue-700 fill-blue-500 stroke-[2.5]" />} iconTone="bg-mood-sadness-reflect/20" />
          </div>
          {dashboardError && <p role="alert" className="mt-3 text-sm text-red-700">{t('dashboard.metricsError')} <button type="button" className="underline" onClick={() => void dashboardQuery.refetch()}>{t('common.retry')}</button></p>}
        </div>
      </motion.section>

      {/* 2. CORE COLLAGE BENTO GRID (FR-10, FR-06, FR-07) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* COLUMN 1 (7 cols): MOOD & STRESS 14-DAY DUAL TREND CHART (FR-10) */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">

            <div className="flex flex-wrap items-center justify-between gap-2 pb-4 border-b-2 border-on-background">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-primary-container border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">{t('dashboard.trends')}</h2>
              </div>
              
              {/* Trend View Filter Chips */}
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => setTrendView('both')}
                  className={`text-[10px] font-space font-bold px-2 py-0.5 rounded border transition-colors cursor-pointer ${
                    trendView === 'both' ? 'bg-black text-white' : 'bg-gray-100 hover:bg-gray-200'
                  }`}
                >
                  {t('dashboard.both')}
                </button>
                <button
                  type="button"
                  onClick={() => setTrendView('mood')}
                  className={`text-[10px] font-space font-bold px-2 py-0.5 rounded border transition-colors cursor-pointer ${
                    trendView === 'mood' ? 'bg-primary-container text-black' : 'bg-gray-100 hover:bg-gray-200'
                  }`}
                >
                  {t('dashboard.moodOnly')}
                </button>
                <button
                  type="button"
                  onClick={() => setTrendView('stress')}
                  className={`text-[10px] font-space font-bold px-2 py-0.5 rounded border transition-colors cursor-pointer ${
                    trendView === 'stress' ? 'bg-mood-anxiety-stress text-white' : 'bg-gray-100 hover:bg-gray-200'
                  }`}
                >
                  {t('dashboard.stressOnly')}
                </button>
              </div>
            </div>

            <div className="mt-4">
              <MoodStressTrendChart data={stats} trendView={trendView} />
            </div>
          </div>

          {/* CYCLIC COMPARISON & THEMES (FR-07) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-secondary-container border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">{t('dashboard.topics')}</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                {t('dashboard.frequency')}
              </span>
            </div>

            <div className="flex flex-wrap gap-2.5">
              {dashboard && dashboard.topTopics.length === 0 && <p className="text-sm">{t('dashboard.noTopics')}</p>}
              {(dashboard?.topTopics ?? []).map((item, i) => (
                <span
                  key={i}
                  className="font-space text-xs font-bold px-3 py-1.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center gap-1.5 bg-paper-warm"
                >
                  <span>{topicLabel(item.code, t)}</span>
                  <span className="text-[10px] bg-black/15 px-1.5 py-0.5 rounded-full font-mono">{item.count}</span>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* COLUMN 2 (5 cols): EMOTION SPECTRUM (FR-06) & RECENT POLAROID SHELF */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* EMOTION SPECTRUM (FR-06 & FR-DASH-04) */}
          <section aria-labelledby="dashboard-emotions-title" className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">

            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-mood-hope-energy border-neo-sm"></span>
                <h2 id="dashboard-emotions-title" className="font-space text-lg font-bold text-on-surface">{t('dashboard.emotions')}</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                {t('dashboard.last30')}
              </span>
            </div>

            <p className="font-sans text-xs text-on-surface-variant mb-1">{t('dashboard.emotionsHint')}</p>
            <EmotionDistributionChart
              items={moodSpectrum(dashboard?.topMoods ?? [], t)}
              emptyText={t('dashboard.noEmotions')}
              formatValue={(value) => t('dashboard.entryCount', { count: value })}
              listLabel={t('dashboard.emotions')}
            />
          </section>

          {/* TONES IN WRITING: primary analysis tone per entry, kept separate from the picked mood. */}
          <section aria-labelledby="dashboard-tones-title" className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-paper-warm border-neo-sm"></span>
                <h2 id="dashboard-tones-title" className="font-space text-lg font-bold text-on-surface">{t('dashboard.tones')}</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                {t('dashboard.last30')}
              </span>
            </div>
            <p className="font-sans text-xs text-on-surface-variant mb-1">{t('dashboard.tonesHint')}</p>
            <EmotionDistributionChart
              items={toneSpectrum(dashboard?.topEmotions ?? [], t)}
              emptyText={t('dashboard.noTones')}
              formatValue={(value) => `${value}%`}
              maxValue={100}
              listLabel={t('dashboard.tones')}
            />
          </section>

          {/* POLAROID SHELF (FR-12 & FR-JOURNAL-09) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-on-background mb-4">
              <h2 className="font-space text-lg font-bold text-on-surface">{t('dashboard.memories')}</h2>
              <Link href="/history-calendar" className="text-xs font-space font-bold underline hover:text-primary">
                {t('dashboard.viewAll')}
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {recentQuery.isError && <p role="alert" className="text-sm text-red-700">{t('dashboard.metricsError')} <button type="button" className="underline" onClick={() => void recentQuery.refetch()}>{t('common.retry')}</button></p>}
              {recentEntries.map((entry, index) => (
                <Link key={entry.id} href="/history-calendar">
                  <PolaroidCard
                    imageUrl={entry.photoUrl}
                    caption={entry.title}
                    accent={moodColor(entry.emotion?.replace('_', '-')) ?? undefined}
                    date={formatDate(entry.date, locale)}
                    rotate={index % 2 === 0 ? -2 : 2}
                  />
                </Link>
              ))}
            </div>
          </div>

          {/* Quick Support & Crisis Helpline Shortcut */}
          <div className="p-4 bg-mood-anxiety-stress/10 border-2 border-red-300 rounded-3xl flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <HeartHandshake className="w-5 h-5 text-mood-anxiety-stress shrink-0" />
              <div>
                <span className="font-space text-xs font-bold text-black block">{t('dashboard.supportQuestion')}</span>
                <span className="font-sans text-[11px] text-gray-600">{t('dashboard.supportDescription')}</span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setIsSafetyModalOpen(true)}
              className="text-xs font-space font-bold px-3 py-1.5 bg-white border border-black rounded-xl shadow-neo-sm hover:bg-paper-warm transition-colors cursor-pointer shrink-0"
            >
              {t('dashboard.openSupport')}
            </button>
          </div>
        </div>
      </div>

      {/* Safety Modal */}
      <SafetyModal
        isOpen={isSafetyModalOpen}
        onClose={() => setIsSafetyModalOpen(false)}
      />
    </div>
  );
}
