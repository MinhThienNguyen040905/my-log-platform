'use client';

import React from 'react';
import { Sparkles } from 'lucide-react';
import type { SpectrumItem } from '../utils/spectrum';

interface EmotionDistributionChartProps {
  items: SpectrumItem[];
  emptyText: string;
  /** Formats a value for the bar label, e.g. "18 bài" or "45%". */
  formatValue: (value: number) => string;
  /** Value of a full-width bar; omit to scale to the largest value. */
  maxValue?: number;
  /** Accessible name of the list. */
  listLabel: string;
}

/**
 * Labelled horizontal bars as a plain list: name, bar, value on one row, largest first, empty categories hidden.
 * The name sits next to its bar, so colour is never needed to tell categories apart and screen readers read
 * the same text sighted users see.
 */
export function EmotionDistributionChart({ items, emptyText, formatValue, maxValue, listLabel }: EmotionDistributionChartProps) {
  const rows = items.filter((item) => item.value > 0).sort((a, b) => b.value - a.value);
  if (rows.length === 0) {
    return (
      <div className="w-full h-32 flex flex-col items-center justify-center gap-2 text-center text-gray-500">
        <Sparkles className="w-6 h-6 text-gray-400" aria-hidden="true" />
        <p className="font-space text-xs font-bold">{emptyText}</p>
      </div>
    );
  }
  const scale = maxValue ?? Math.max(...rows.map((row) => row.value));

  return (
    <ul aria-label={listLabel} className="flex flex-col gap-2.5 pt-2">
      {rows.map((row) => (
        <li key={row.key} className="grid grid-cols-[6.5rem_1fr_auto] items-center gap-3 font-space text-xs">
          <span className="truncate font-bold text-on-surface">{row.label}</span>
          <span className="h-4 rounded-r-md" aria-hidden="true">
            <span
              className="block h-full rounded-r-md border-2 border-black"
              style={{ width: `${Math.max(3, (row.value / scale) * 100)}%`, backgroundColor: row.color }}
            />
          </span>
          <span className="font-bold text-on-surface tabular-nums">{formatValue(row.value)}</span>
        </li>
      ))}
    </ul>
  );
}
