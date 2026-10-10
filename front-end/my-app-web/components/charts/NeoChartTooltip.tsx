'use client';

import React from 'react';

export interface NeoChartTooltipProps {
  active?: boolean;
  payload?: Array<{
    name?: string;
    value?: number | string | null;
    color?: string;
    dataKey?: string;
    unit?: string;
  }>;
  label?: string;
  valueFormatter?: (value: number | string, name?: string) => string;
}

export function NeoChartTooltip({
  active,
  payload,
  label,
  valueFormatter,
}: NeoChartTooltipProps) {
  if (!active || !payload || !payload.length) return null;

  return (
    <div className="bg-white border-neo-sm rounded-xl p-3 shadow-neo-sm font-space min-w-[140px] pointer-events-none z-50">
      {label && (
        <div className="text-[11px] font-extrabold uppercase tracking-wider text-gray-600 pb-1.5 border-b border-black/20 mb-2">
          {label}
        </div>
      )}
      <div className="flex flex-col gap-1.5">
        {payload.map((entry, index) => {
          if (entry.value === null || entry.value === undefined) return null;
          const displayValue = valueFormatter
            ? valueFormatter(entry.value, entry.name)
            : `${entry.value}${entry.unit ?? ''}`;

          return (
            <div key={`item-${index}`} className="flex items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-1.5">
                <span
                  className="w-2.5 h-2.5 rounded-full border border-black shrink-0"
                  style={{ backgroundColor: entry.color || '#B7FF32' }}
                />
                <span className="font-bold text-on-surface truncate max-w-[110px]">
                  {entry.name || 'Chỉ số'}
                </span>
              </div>
              <span className="font-mono font-extrabold text-black">
                {displayValue}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

