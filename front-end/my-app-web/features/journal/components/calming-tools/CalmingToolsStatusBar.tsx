'use client';

import { X } from 'lucide-react';
import type { BreathPhase } from '../../hooks/useBreathingTimer';
import { BreathingWidget } from './BreathingWidget';
import { AmbientSoundWidget } from './AmbientSoundWidget';

interface CalmingToolsStatusBarProps {
  tool: 'BREATHING' | 'AMBIENT';
  breathingPhase: BreathPhase;
  breathingLabel: string;
  onStop: () => void;
}

/** Thin status bar shown above the writing area while a calming tool is running. */
export function CalmingToolsStatusBar({ tool, breathingPhase, breathingLabel, onStop }: CalmingToolsStatusBarProps) {
  return (
    <div
      role="status"
      className="flex items-center gap-3 rounded-2xl border-2 border-black bg-blue-50 px-4 py-3 shadow-neo-sm animate-in fade-in"
    >
      {tool === 'BREATHING' ? (
        <BreathingWidget phase={breathingPhase} label={breathingLabel} />
      ) : (
        <AmbientSoundWidget />
      )}
      <button
        type="button"
        onClick={onStop}
        aria-label="Dừng công cụ dịu"
        title="Dừng"
        className="ml-auto flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border border-black bg-white transition-all hover:-translate-y-0.5 hover:bg-white/80 hover:shadow-neo-xs active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
