'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useJournal, SafetyModal } from '@/features/journal';
import { getDashboard, type Dashboard } from '../api/dashboard';
import { MoodStressTrendChart } from './MoodStressTrendChart';
import { EmotionDistributionChart } from './EmotionDistributionChart';
import { useQuery } from '@tanstack/react-query';
import { WashiTape, PolaroidCard } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';
import { motion } from 'motion/react';
import {
  Flame,
  Calendar,
  Edit3,
  Smile,
  Leaf,
  Zap,
  Moon,
  Lightbulb,
  Sparkles,
  HeartHandshake,
} from 'lucide-react';

export function DashboardView() {
  const { t } = useTranslation();
  const { locale } = useAppLanguage();
  const { entries, streakCount, accountProfile, userId } = useJournal();
  const dashboardQuery = useQuery<Dashboard>({ queryKey: ['dashboard', userId, '30d'], queryFn: () => getDashboard('30d') });
  const dashboard = dashboardQuery.data;
  const dashboardError = dashboardQuery.isError;
  const stats = dashboard?.timeline.slice(-14) ?? [];
  const average = (key: 'moodScore' | 'stressScore' | 'energyScore' | 'sleepMinutes') => {
    const values = stats.map((day) => day[key]).filter((value): value is number => value !== null);
    return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : null;
  };
  const mood = average('moodScore');
  const stress = average('stressScore');
  const energy = average('energyScore');
  const sleep = average('sleepMinutes');
  const greetingName = accountProfile.displayName?.trim() || accountProfile.penName?.trim() || (locale === 'en' ? 'there' : 'bạn');
  const recordedDays = new Set(entries.map((entry) => entry.date)).size;

  const [trendView, setTrendView] = useState<'both' | 'mood' | 'stress'>('both');
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
      {/* 0. MINIMUM HISTORICAL DATA THRESHOLD BANNER (FR-STAT-01) */}
      <div className="p-4 bg-paper-warm border-2 border-black rounded-2xl shadow-neo-sm flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-primary-container border border-black flex items-center justify-center font-bold text-black shrink-0">
            <Sparkles className="w-5 h-5 text-black" />
          </div>
          <div>
            <span className="font-space text-xs font-extrabold uppercase text-black block">
              {t('dashboard.progress')}
            </span>
            <p className="font-sans text-xs text-gray-700 mt-0.5">
              {t('dashboard.recorded', { count: recordedDays })}
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
      </div>

      {/* 1. HERO BANNER - Playful Neo-Brutalist Scrapbook */}
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
        className="relative w-full"
      >
        {/* Washi Tape decoration */}
        <WashiTape color="lavender" rotate={-2} className="absolute -top-3 left-10 w-36 z-20" />

        <div className="bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg relative overflow-hidden">
          {/* Badges bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 mb-6 border-b-2 border-dashed border-border-soft pb-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 bg-paper-warm text-on-surface font-space text-xs sm:text-sm font-bold border-neo-sm rounded-lg shadow-neo-sm flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-on-surface stroke-[2.5]" />
                {todayLabel || t('dashboard.loadingDate')}
              </span>
              <span className="px-3 py-1 bg-primary-container text-on-primary-container font-space text-xs sm:text-sm font-bold border-neo-sm rounded-lg shadow-neo-sm uppercase tracking-wider">
                {t('dashboard.period')}
              </span>
            </div>

            {/* Streak Badge */}
            <div className="inline-flex items-center gap-2 bg-mood-hope-energy text-black px-4 py-1.5 border-neo-sm rounded-xl shadow-neo-sm rotate-1">
              <Flame className="w-5 h-5 text-orange-600 fill-orange-500 shrink-0" />
              <span className="font-space text-sm font-extrabold">{t('dashboard.streak', { count: streakCount })}</span>
              <span className="text-[11px] bg-white px-2 py-0.5 rounded border border-black uppercase font-bold text-on-surface">
                {t('dashboard.badge')}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
            <div className="lg:col-span-8 flex flex-col gap-3">
              <h1 className="font-space text-2xl sm:text-4xl lg:text-5xl font-extrabold text-on-surface tracking-tight leading-tight">
                {t('dashboard.greeting', { name: greetingName })}{' '}
                <span className="bg-primary-container px-2.5 py-0.5 border-neo-sm rounded-lg shadow-neo-sm inline-block">
                  {t('dashboard.today')}
                </span>{' '}
                {t('dashboard.feeling')}
              </h1>
              <p className="font-sans text-sm sm:text-base text-on-surface-variant max-w-2xl leading-relaxed">
                {t('dashboard.intro')}
              </p>
            </div>

            {/* Call To Action */}
            <div className="lg:col-span-4 flex flex-col sm:flex-row lg:justify-end items-stretch sm:items-center gap-3">
              <Link href="/journal-editor" className="w-full sm:w-auto">
                <NeoButton size="lg" className="w-full bg-primary-container text-on-primary-container font-extrabold" icon={<Edit3 className="w-5 h-5 stroke-[2.5]" />}>
                  {t('dashboard.write')}
                </NeoButton>
              </Link>
            </div>
          </div>

          {/* 4 Fast Status Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-8 pt-6 border-t-2 border-on-background">
            <div className="bg-surface-container-lowest p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">{t('dashboard.moodAverage')}</span>
                <span className="font-space text-xl font-bold text-on-surface">{mood === null ? '—' : mood.toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}{mood !== null && <span className="text-xs font-normal text-on-surface-variant">/10</span>}</span>
              </div>
              <div className="w-10 h-10 rounded-xl bg-mood-calm-joy/30 border-neo-sm flex items-center justify-center shadow-neo-sm">
                <Smile className="w-5 h-5 text-green-800 stroke-[2.5]" />
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">{t('dashboard.stressAverage')}</span>
                <span className="font-space text-xl font-bold text-on-surface">{stress === null ? '—' : stress.toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}{stress !== null && <span className="text-xs font-normal text-on-surface-variant">/10</span>}</span>
              </div>
              <div className="w-10 h-10 rounded-xl bg-mood-calm-joy/30 border-neo-sm flex items-center justify-center shadow-neo-sm">
                <Leaf className="w-5 h-5 text-green-800 stroke-[2.5]" />
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">{t('dashboard.energyAverage')}</span>
                <span className="font-space text-xl font-bold text-on-surface">{energy === null ? '—' : energy.toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}{energy !== null && <span className="text-xs font-normal text-on-surface-variant">/10</span>}</span>
              </div>
              <div className="w-10 h-10 rounded-xl bg-mood-hope-energy/40 border-neo-sm flex items-center justify-center shadow-neo-sm">
                <Zap className="w-5 h-5 text-yellow-700 fill-yellow-500 stroke-[2.5]" />
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">{t('dashboard.sleepAverage')}</span>
                <span className="font-space text-xl font-bold text-on-surface">{sleep === null ? '—' : (sleep / 60).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}{sleep !== null && <span className="text-xs font-normal text-on-surface-variant">{t('dashboard.hours')}</span>}</span>
              </div>
              <div className="w-10 h-10 rounded-xl bg-mood-sadness-reflect/20 border-neo-sm flex items-center justify-center shadow-neo-sm">
                <Moon className="w-5 h-5 text-blue-700 fill-blue-500 stroke-[2.5]" />
              </div>
            </div>
          </div>
          {dashboardError && <p role="alert" className="mt-3 text-sm text-red-700">{t('dashboard.metricsError')} <button type="button" className="underline" onClick={() => void dashboardQuery.refetch()}>{t('common.retry')}</button></p>}
        </div>
      </motion.section>

      {/* 2. CORE COLLAGE BENTO GRID (FR-10, FR-06, FR-07) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* COLUMN 1 (7 cols): MOOD & STRESS 14-DAY DUAL TREND CHART (FR-10) */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
            <WashiTape color="peach" rotate={2} className="absolute -top-3 right-8 w-28" />

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

            {/* Recharts Area / Line Chart Visualizer */}
            <div className="mt-4">
              <MoodStressTrendChart
                data={stats}
                trendView={trendView}
                onTrendViewChange={setTrendView}
                hideHeader
              />
            </div>

            <p className="mt-4 text-xs font-sans text-on-surface-variant bg-surface-container-low p-3 rounded-xl border border-border-soft flex items-start gap-2">
              <Lightbulb className="w-4 h-4 text-amber-500 fill-amber-400 shrink-0 mt-0.5" />
              <span>{t('dashboard.chartHint')}</span>
            </p>
          </div>

          {/* CYCLIC COMPARISON & THEMES (FR-07) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
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
                  <span>{item.code}</span>
                  <span className="text-[10px] bg-black/15 px-1.5 py-0.5 rounded-full font-mono">{item.count}</span>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* COLUMN 2 (5 cols): EMOTION SPECTRUM (FR-06) & RECENT POLAROID SHELF */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* EMOTION SPECTRUM (FR-06 & FR-DASH-04) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
            <WashiTape color="blue" rotate={-1} className="absolute -top-3 left-6 w-24" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-mood-hope-energy border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">{t('dashboard.emotions')}</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                {t('dashboard.last30')}
              </span>
            </div>

            <EmotionDistributionChart emotions={dashboard?.topEmotions ?? []} />
          </div>

          {/* POLAROID SHELF (FR-12 & FR-JOURNAL-09) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <h2 className="font-space text-lg font-bold text-on-surface">{t('dashboard.memories')}</h2>
              <Link href="/history-calendar" className="text-xs font-space font-bold underline hover:text-primary">
                {t('dashboard.viewAll', { count: entries.length })}
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {entries.slice(0, 2).map((entry, index) => (
                <Link key={entry.id} href="/history-calendar">
                  <PolaroidCard
                    imageUrl={entry.photoUrl || 'https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80'}
                    caption={entry.title}
                    date={entry.date}
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
