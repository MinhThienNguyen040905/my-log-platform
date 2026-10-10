'use client';

import React from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  CartesianGrid,
} from 'recharts';
import { NeoChartTooltip } from '@/components/charts/NeoChartTooltip';
import type { TopCode } from '../api/dashboard';
import { useTranslation } from 'react-i18next';
import { Sparkles } from 'lucide-react';

const EMOTION_PALETTE = [
  '#70E000', // Calm / Joy
  '#FFD166', // Hope / Energy
  '#FF6B6B', // Anxiety / Stress
  '#4D96FF', // Sadness / Reflection
  '#BD6EFE', // Purple / Mystery
  '#FF9F1C', // Amber
];

interface EmotionDistributionChartProps {
  emotions: TopCode[];
}

export function EmotionDistributionChart({ emotions }: EmotionDistributionChartProps) {
  const { t } = useTranslation();

  if (!emotions || emotions.length === 0) {
    return (
      <div className="w-full h-44 flex flex-col items-center justify-center gap-2 text-center text-gray-500">
        <Sparkles className="w-6 h-6 text-gray-400" />
        <p className="font-space text-xs font-bold">{t('dashboard.noEmotions')}</p>
      </div>
    );
  }

  const chartData = emotions.slice(0, 6).map((item, idx) => ({
    name: item.code,
    count: item.count,
    color: EMOTION_PALETTE[idx % EMOTION_PALETTE.length],
  }));

  return (
    <div className="w-full h-52 sm:h-56 pt-2">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={chartData}
          layout="vertical"
          margin={{ top: 5, right: 20, left: 10, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#D9D7D2" horizontal={false} />
          <XAxis
            type="number"
            stroke="#111111"
            tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
            tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
            allowDecimals={false}
          />
          <YAxis
            type="category"
            dataKey="name"
            stroke="#111111"
            tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
            tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
            width={85}
          />
          <Tooltip
            content={
              <NeoChartTooltip
                valueFormatter={(val) => `${val} lần`}
              />
            }
          />
          <Bar
            dataKey="count"
            name="Số lần xuất hiện"
            stroke="#111111"
            strokeWidth={2}
            radius={[0, 6, 6, 0]}
          >
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

