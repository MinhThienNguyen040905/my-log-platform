'use client';

import { Wind } from 'lucide-react';
import type { BreathPhase } from '../../hooks/useBreathingTimer';

interface BreathingWidgetProps {
  phase: BreathPhase;
  label: string;
}

const SCALE_BY_PHASE: Record<BreathPhase, string> = {
  'Hít vào chậm...': 'scale-110 bg-lime-100',
  'Giữ nhẹ...': 'scale-105 bg-amber-100',
  'Thở ra từ từ...': 'scale-90 bg-blue-100',
};

/** Presentational only; {@link useBreathingTimer} in the parent drives the countdown. */
export function BreathingWidget({ phase, label }: BreathingWidgetProps) {
  return (
    <div className="flex items-center gap-3">
      <span
        aria-hidden="true"
        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-black transition-transform duration-[3000ms] ease-in-out ${SCALE_BY_PHASE[phase]}`}
      >
        <Wind className="h-4 w-4" />
      </span>
      <div className="flex flex-col leading-tight">
        <span className="font-space text-xs font-extrabold">Thở chậm cùng mình</span>
        <span className="text-xs text-gray-600">{phase}</span>
      </div>
      <span className="ml-auto font-space text-sm font-extrabold tabular-nums">{label}</span>
    </div>
  );
}
