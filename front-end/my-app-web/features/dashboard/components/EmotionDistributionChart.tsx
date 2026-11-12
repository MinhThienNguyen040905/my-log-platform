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
import { Sparkles } from 'lucide-react';
import type { SpectrumItem } from '../utils/spectrum';

interface EmotionDistributionChartProps {
  items: SpectrumItem[];
  emptyText: string;
  /** Formats a bar value for the axis and tooltip, e.g. "18 bài" or "45%". */
  formatValue: (value: number) => string;
  /** Upper bound of the value axis; omit to scale to the data. */
  maxValue?: number;
  /** Name of the measured quantity, shown in the tooltip. */
  valueName: string;
}

/** Horizontal bar chart for a mood/tone spectrum; labels and colours come from the caller. */
export function EmotionDistributionChart({ items, emptyText, formatValue, maxValue, valueName }: EmotionDistributionChartProps) {
  if (items.length === 0) {
    return (
      <div className="w-full h-44 flex flex-col items-center justify-center gap-2 text-center text-gray-500">
        <Sparkles className="w-6 h-6 text-gray-400" />
        <p className="font-space text-xs font-bold">{emptyText}</p>
      </div>
    );
  }

  const chartData = items.slice(0, 7).map((item) => ({ name: item.label, value: item.value, color: item.color }));

  return (
    <div className="w-full pt-2" style={{ height: Math.max(140, chartData.length * 40 + 40) }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#D9D7D2" horizontal={false} />
          <XAxis
            type="number"
            stroke="#111111"
            tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
            tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
            allowDecimals={false}
            domain={maxValue ? [0, maxValue] : [0, 'auto']}
            tickFormatter={(value: number) => formatValue(value)}
          />
          <YAxis
            type="category"
            dataKey="name"
            stroke="#111111"
            tick={{ fontSize: 11, fontFamily: 'var(--font-space-grotesk), monospace', fontWeight: 700 }}
            tickLine={{ stroke: '#111111', strokeWidth: 1.5 }}
            width={95}
          />
          <Tooltip content={<NeoChartTooltip valueFormatter={(val) => formatValue(Number(val))} />} />
          <Bar dataKey="value" name={valueName} stroke="#111111" strokeWidth={2} radius={[0, 6, 6, 0]}>
            {chartData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
