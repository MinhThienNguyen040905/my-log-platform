'use client';

import React from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { NeoChartTooltip } from '@/components/charts/NeoChartTooltip';
import { useTranslation } from 'react-i18next';
import { TrendingUp, Sparkles } from 'lucide-react';

export interface TimelinePoint {
  date: string;
  moodScore: number | null;
  stressScore: number | null;
  energyScore?: number | null;
  sleepMinutes?: number | null;
}

interface MoodStressTrendChartProps {
  data: TimelinePoint[];
  trendView: 'both' | 'mood' | 'stress';
  onTrendViewChange?: (view: 'both' | 'mood' | 'stress') => void;
  hideHeader?: boolean;
}

export function MoodStressTrendChart({
  data,
  trendView,
  onTrendViewChange,
  hideHeader = false,
}: MoodStressTrendChartProps) {
  const { t } = useTranslation();

  const formattedData = data.map((d) => ({
    ...d,
    shortDate: d.date.slice(5),
    mood: d.moodScore,
    stress: d.stressScore,
  }));

  const hasData = formattedData.some((d) => d.mood !== null || d.stress !== null);

  return (
    <div className="flex flex-col gap-4">
      {/* Header controls & Legend */}
      {!hideHeader && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <TrendingUp className="w-4 h-4 text-primary" />
            <span className="font-space text-xs font-bold uppercase text-on-surface">
              {t('dashboard.trends')}
            </span>
          </div>

          {/* View toggles */}
          {onTrendViewChange && (
            <div className="inline-flex rounded-xl border-neo-sm p-0.5 bg-paper-warm text-xs font-space font-bold shadow-neo-sm">
              <button
                type="button"
                onClick={() => onTrendViewChange('both')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  trendView === 'both'
                    ? 'bg-primary-container text-black border border-black shadow-neo-sm'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                {t('dashboard.bothSeries')}
              </button>
              <button
                type="button"
                onClick={() => onTrendViewChange('mood')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  trendView === 'mood'
                    ? 'bg-primary-container text-black border border-black shadow-neo-sm'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                {t('dashboard.moodSeries')}
              </button>
              <button
                type="button"
                onClick={() => onTrendViewChange('stress')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  trendView === 'stress'
                    ? 'bg-mood-anxiety-stress text-black border border-black shadow-neo-sm'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                {t('dashboard.stressSeries')}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Chart container */}
      <div className="w-full h-64 sm:h-72 bg-paper-warm/40 border-neo-sm rounded-2xl p-3 pt-6 relative">
        {!hasData ? (
          <div className="w-full h-full flex flex-col items-center justify-center gap-2 text-center text-gray-500">
            <Sparkles className="w-6 h-6 text-gray-400" />
            <p className="font-space text-xs font-bold">Chưa có đủ điểm dữ liệu cảm xúc trong 14 ngày qua</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={formattedData} margin={{ top: 10, right: 12, left: -22, bottom: 0 }}>
              <defs>
                <linearGradient id="neoMoodGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#70E000" stopOpacity={0.65} />
                  <stop offset="95%" stopColor="#70E000" stopOpacity={0.05} />
                </linearGradient>
                <linearGradient id="neoStressGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#FF6B6B" stopOpacity={0.65} />
                  <stop offset="95%" stopColor="#FF6B6B" stopOpacity={0.05} />
                </linearGradient>
              </defs>

              <CartesianGrid strokeDasharray="3 3" stroke="#D9D7D2" vertical={false} />

              <XAxis
                dataKey="shortDate"
                stroke="#111111"
                tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
                tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
              />

              <YAxis
                domain={[0, 10]}
                ticks={[0, 2, 4, 6, 8, 10]}
                stroke="#111111"
                tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
                tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
              />

              <Tooltip
                content={
                  <NeoChartTooltip
                    valueFormatter={(val) => `${val}/10`}
                  />
                }
              />

              {(trendView === 'both' || trendView === 'mood') && (
                <Area
                  type="monotone"
                  dataKey="mood"
                  name={t('dashboard.moodLegend')}
                  stroke="#111111"
                  strokeWidth={2.5}
                  fill="url(#neoMoodGradient)"
                  connectNulls
                  dot={{ r: 3.5, stroke: '#111111', strokeWidth: 2, fill: '#70E000' }}
                  activeDot={{ r: 6, stroke: '#111111', strokeWidth: 2.5, fill: '#B7FF32' }}
                />
              )}

              {(trendView === 'both' || trendView === 'stress') && (
                <Area
                  type="monotone"
                  dataKey="stress"
                  name={t('dashboard.stressLegend')}
                  stroke="#111111"
                  strokeWidth={2.5}
                  fill="url(#neoStressGradient)"
                  connectNulls
                  dot={{ r: 3.5, stroke: '#111111', strokeWidth: 2, fill: '#FF6B6B' }}
                  activeDot={{ r: 6, stroke: '#111111', strokeWidth: 2.5, fill: '#FFA8A8' }}
                />
              )}
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
