'use client';

import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowDownRight, ArrowRight, ArrowUpRight } from 'lucide-react';
import type { MetricTrend } from '../utils/metric-trend';

type Props = {
  label: string; value: string | null; unit: string; icon: ReactNode; iconTone: string;
  trend: MetricTrend; formatPrevious: (value: number) => string;
};

/** One of the four hero numbers, with the change against the previous 30 days (colour says good or not). */
export function MetricCard({ label, value, unit, icon, iconTone, trend, formatPrevious }: Props) {
  const { t } = useTranslation();
  const Arrow = trend.direction === 'up' ? ArrowUpRight : trend.direction === 'down' ? ArrowDownRight : ArrowRight;
  const tone = trend.good === null ? 'text-gray-600' : trend.good ? 'text-green-700' : 'text-amber-700';
  return (
    <div className="bg-surface-container-lowest p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-start justify-between gap-2">
      <div className="min-w-0">
        <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">{label}</span>
        <span className="font-space text-xl font-bold text-on-surface">
          {value ?? '—'}{value !== null && <span className="text-xs font-normal text-on-surface-variant">{unit}</span>}
        </span>
        <span className={`mt-0.5 flex items-center gap-1 font-sans text-xs leading-tight ${tone}`}>
          {trend.direction && <Arrow className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />}
          {trend.direction === null
            ? t('dashboard.noComparison')
            : trend.direction === 'same'
              ? t('dashboard.sameAsPrevious')
              : t('dashboard.vsPrevious', { value: formatPrevious(trend.previous as number) })}
        </span>
      </div>
      <div aria-hidden="true" className={`hidden sm:flex w-10 h-10 shrink-0 rounded-xl border-neo-sm items-center justify-center shadow-neo-sm ${iconTone}`}>{icon}</div>
    </div>
  );
}
