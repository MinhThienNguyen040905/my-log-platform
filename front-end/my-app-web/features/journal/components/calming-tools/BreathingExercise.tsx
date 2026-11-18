'use client';

import { Play, Square } from 'lucide-react';
import type { BreathPhase } from '../../hooks/useBreathingTimer';

interface BreathingExerciseProps {
  running: boolean;
  phase: BreathPhase;
  /** Giây trong pha hiện tại, 1..4. */
  count: number;
  label: string;
  onStart: () => void;
  onStop: () => void;
}

const PHASE_SHORT: Record<BreathPhase, string> = {
  'Hít vào chậm...': 'Hít vào',
  'Giữ nhẹ...': 'Giữ',
  'Thở ra từ từ...': 'Thở ra',
};

/**
 * Bong bóng phồng lên khi hít vào, phập phồng nhẹ khi giữ, xẹp xuống và rung nhẹ khi thở ra — mỗi pha
 * 4 giây. Keyframes `mylog-breath-*` nằm trong globals.css; reduced-motion bỏ chuyển động.
 */
const CIRCLE_BY_PHASE: Record<BreathPhase, string> = {
  'Hít vào chậm...': 'mylog-breath-in bg-lime-100',
  'Giữ nhẹ...': 'mylog-breath-hold bg-amber-100',
  'Thở ra từ từ...': 'mylog-breath-out bg-sky-100',
};

/** Thở chậm 2 phút theo nhịp 4-4-4, có số đếm từng giây. Chỉ hiển thị; useBreathingTimer chạy đồng hồ. */
export function BreathingExercise({ running, phase, count, label, onStart, onStop }: BreathingExerciseProps) {
  return (
    <section aria-label="Thở chậm 2 phút" className="flex flex-1 flex-col items-center gap-5 px-4 py-6">
      <p className="text-center text-xs text-gray-600">Hít vào 4 nhịp · giữ 4 nhịp · thở ra 4 nhịp. Cứ theo vòng tròn là được.</p>

      <div className="relative flex h-56 w-56 items-center justify-center">
        <span aria-hidden="true" className="absolute inset-0 rounded-full border-2 border-dashed border-black/20" />
        <span
          aria-hidden="true"
          className={`absolute inset-0 rounded-full border-2 border-black shadow-neo-sm transition-colors duration-1000 ${
            running ? CIRCLE_BY_PHASE[phase] : 'scale-[0.62] bg-white'
          }`}
        >
          {/* Vệt sáng cho giống bong bóng. */}
          <span className="absolute left-[18%] top-[14%] h-[16%] w-[26%] -rotate-[30deg] rounded-full bg-white/80" />
        </span>
        <div className="relative flex flex-col items-center leading-none">
          {running ? (
            <>
              <span key={`${phase}-${count}`} className="font-space text-6xl font-extrabold tabular-nums animate-in fade-in zoom-in-95">
                {count}
              </span>
              <span className="mt-2 font-space text-sm font-extrabold" aria-live="polite">{PHASE_SHORT[phase]}</span>
            </>
          ) : (
            <span className="font-space text-sm font-extrabold">Sẵn sàng</span>
          )}
        </div>
      </div>

      <span className="font-space text-2xl font-extrabold tabular-nums" aria-label={`Còn ${label}`}>{label}</span>

      {running ? (
        <button
          type="button"
          onClick={onStop}
          className="inline-flex items-center gap-2 rounded-xl border-2 border-black bg-white px-4 py-2 font-space text-sm font-extrabold shadow-neo-sm transition-all hover:-translate-y-0.5 hover:bg-red-50 active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          <Square className="h-4 w-4" aria-hidden="true" />
          Dừng thở
        </button>
      ) : (
        <button
          type="button"
          onClick={onStart}
          className="inline-flex items-center gap-2 rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-sm font-extrabold shadow-neo-sm transition-all hover:-translate-y-0.5 active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          <Play className="h-4 w-4" aria-hidden="true" />
          Bắt đầu thở
        </button>
      )}
    </section>
  );
}
