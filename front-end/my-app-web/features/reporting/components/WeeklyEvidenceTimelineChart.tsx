'use client';

import React from 'react';
import {
  ResponsiveContainer,
  ComposedChart,
  Area,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { NeoChartTooltip } from '@/components/charts/NeoChartTooltip';
import type { ReportEvidence } from '../api/reports';
import { useTranslation } from 'react-i18next';
import { Activity } from 'lucide-react';

interface WeeklyEvidenceTimelineChartProps {
  evidence: ReportEvidence[];
}

export function WeeklyEvidenceTimelineChart({ evidence }: WeeklyEvidenceTimelineChartProps) {
  const { t } = useTranslation();

  if (!evidence || evidence.length === 0) return null;

  // Aggregate evidence by date
  const dateMap = new Map<string, {
    date: string;
    shortDate: string;
    mood: number | null;
    stress: number | null;
    energy: number | null;
    sleepMinutes: number | null;
  }>();

  for (const item of evidence) {
    if (!dateMap.has(item.date)) {
      dateMap.set(item.date, {
        date: item.date,
        shortDate: item.date.slice(5),
        mood: null,
        stress: null,
        energy: null,
        sleepMinutes: null,
      });
    }
    const entry = dateMap.get(item.date)!;
    if (item.metric === 'MOOD_SCORE') entry.mood = item.value;
    else if (item.metric === 'STRESS_SCORE') entry.stress = item.value;
    else if (item.metric === 'ENERGY_SCORE') entry.energy = item.value;
    else if (item.metric === 'SLEEP_MINUTES') entry.sleepMinutes = item.value;
  }

  const chartData = Array.from(dateMap.values()).sort((a, b) => a.date.localeCompare(b.date));

  return (
    <div className="my-6 p-4 sm:p-5 bg-paper-warm/50 border-neo-sm rounded-2xl">
      <div className="flex items-center justify-between pb-3 border-b border-black/20 mb-3">
        <div className="flex items-center gap-2">
          <Activity className="w-4 h-4 text-primary" />
          <h3 className="font-space text-xs font-bold uppercase tracking-wider text-black">
            Biến thiên cảm xúc trong tuần
          </h3>
        </div>

        {/* Legend */}
        <div className="flex flex-wrap items-center gap-3 text-[11px] font-space font-bold">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#70E000] border border-black"></span>
            <span>{t('report.mood')}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#FF6B6B] border border-black"></span>
            <span>{t('report.stress')}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#FFD166] border border-black"></span>
            <span>{t('report.energy')}</span>
          </div>
        </div>
      </div>

      <div className="w-full h-56 sm:h-64">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={chartData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
            <defs>
              <linearGradient id="reportMoodGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#70E000" stopOpacity={0.6} />
                <stop offset="95%" stopColor="#70E000" stopOpacity={0.05} />
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

            <Area
              type="monotone"
              dataKey="mood"
              name={t('report.mood')}
              stroke="#111111"
              strokeWidth={2.5}
              fill="url(#reportMoodGrad)"
              connectNulls
              dot={{ r: 3.5, stroke: '#111111', strokeWidth: 2, fill: '#70E000' }}
              activeDot={{ r: 6, stroke: '#111111', strokeWidth: 2, fill: '#B7FF32' }}
            />

            <Line
              type="monotone"
              dataKey="stress"
              name={t('report.stress')}
              stroke="#FF6B6B"
              strokeWidth={2.5}
              connectNulls
              dot={{ r: 3.5, stroke: '#111111', strokeWidth: 2, fill: '#FF6B6B' }}
              activeDot={{ r: 6, stroke: '#111111', strokeWidth: 2, fill: '#FFA8A8' }}
            />

            <Line
              type="monotone"
              dataKey="energy"
              name={t('report.energy')}
              stroke="#D4A017"
              strokeWidth={2.5}
              connectNulls
              dot={{ r: 3.5, stroke: '#111111', strokeWidth: 2, fill: '#FFD166' }}
              activeDot={{ r: 6, stroke: '#111111', strokeWidth: 2, fill: '#FFE29A' }}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

