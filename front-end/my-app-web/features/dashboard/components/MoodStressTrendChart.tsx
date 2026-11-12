'use client';

import React from 'react';
import { ResponsiveContainer, ComposedChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { NeoChartTooltip } from '@/components/charts/NeoChartTooltip';
import { useTranslation } from 'react-i18next';
import { Sparkles } from 'lucide-react';
import { movingAverage } from '../utils/metric-trend';

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
}

const AXIS_TICK = { fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 };
const SERIES = {
  mood: { line: '#111111', dot: '#70E000' },
  stress: { line: '#C62828', dot: '#FF6B6B' },
};

/**
 * Daily scores as faint dots with a bold 7-day average line on top, so the direction reads at a glance even
 * with 30 noisy days. No filled areas: two series never cover each other.
 */
export function MoodStressTrendChart({ data, trendView }: MoodStressTrendChartProps) {
  const { t } = useTranslation();
  const moodAvg = movingAverage(data.map((d) => d.moodScore));
  const stressAvg = movingAverage(data.map((d) => d.stressScore));
  const chartData = data.map((d, i) => ({
    shortDate: `${Number(d.date.slice(8, 10))}/${Number(d.date.slice(5, 7))}`,
    mood: d.moodScore,
    stress: d.stressScore,
    moodAvg: moodAvg[i] === null ? null : Math.round((moodAvg[i] as number) * 10) / 10,
    stressAvg: stressAvg[i] === null ? null : Math.round((stressAvg[i] as number) * 10) / 10,
  }));
  const hasData = chartData.some((d) => d.mood !== null || d.stress !== null);
  const showMood = trendView !== 'stress';
  const showStress = trendView !== 'mood';

  const series = (key: 'mood' | 'stress', label: string) => [
    <Line key={`${key}-daily`} type="linear" dataKey={key} name={`${label} · ${t('dashboard.daily')}`} stroke={SERIES[key].line}
      strokeOpacity={0.18} strokeWidth={1} connectNulls isAnimationActive={false}
      dot={{ r: 3, stroke: SERIES[key].line, strokeWidth: 1, fill: SERIES[key].dot, fillOpacity: 0.7 }} activeDot={{ r: 5 }} />,
    <Line key={`${key}-avg`} type="monotone" dataKey={`${key}Avg`} name={`${label} · ${t('dashboard.avg7')}`} stroke={SERIES[key].line}
      strokeWidth={3} connectNulls dot={false} activeDot={{ r: 5, stroke: SERIES[key].line, strokeWidth: 2, fill: SERIES[key].dot }} />,
  ];

  return (
    <div className="flex flex-col gap-2">
      <div className="w-full h-64 sm:h-72 bg-paper-warm/40 border-neo-sm rounded-2xl p-3 pt-6 relative">
        {!hasData ? (
          <div className="w-full h-full flex flex-col items-center justify-center gap-2 text-center text-gray-500">
            <Sparkles className="w-6 h-6 text-gray-400" />
            <p className="font-space text-xs font-bold">{t('dashboard.noTrendData')}</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData} margin={{ top: 10, right: 12, left: -22, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#D9D7D2" vertical={false} />
              <XAxis dataKey="shortDate" stroke="#111111" tick={AXIS_TICK} tickLine={{ stroke: '#111111', strokeWidth: 1.5 }} minTickGap={24} interval="preserveStartEnd" />
              <YAxis domain={[0, 10]} ticks={[0, 2, 4, 6, 8, 10]} stroke="#111111" tick={AXIS_TICK} tickLine={{ stroke: '#111111', strokeWidth: 1.5 }} />
              <Tooltip content={<NeoChartTooltip valueFormatter={(val) => `${val}/10`} />} />
              {showMood && series('mood', t('dashboard.moodAverage'))}
              {showStress && series('stress', t('dashboard.stressAverage'))}
            </ComposedChart>
          </ResponsiveContainer>
        )}
      </div>
      {hasData && (
        <p className="flex flex-wrap items-center gap-x-4 gap-y-1 font-sans text-xs text-on-surface-variant">
          <span className="inline-flex items-center gap-1.5"><span className="inline-block h-[3px] w-5 rounded bg-black" aria-hidden="true" />{t('dashboard.avg7')}</span>
          <span className="inline-flex items-center gap-1.5"><span className="inline-block h-2 w-2 rounded-full border border-black bg-mood-calm-joy/70" aria-hidden="true" />{t('dashboard.daily')}</span>
        </p>
      )}
    </div>
  );
}
