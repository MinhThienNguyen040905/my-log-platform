'use client';

import { useEffect } from 'react';
import { Disc3, Wind, X } from 'lucide-react';
import type { AmbientPlayer } from '../../hooks/useAmbientPlayer';
import type { BreathPhase } from '../../hooks/useBreathingTimer';
import { VinylPlayer } from '../ambient-player/VinylPlayer';
import { BreathingExercise } from './BreathingExercise';

export type CalmingTab = 'BREATHING' | 'MUSIC';

interface CalmingToolsPanelProps {
  tab: CalmingTab;
  onTabChange: (tab: CalmingTab) => void;
  onClose: () => void;
  breathing: { running: boolean; phase: BreathPhase; count: number; label: string };
  onStartBreathing: () => void;
  onStopBreathing: () => void;
  player: AmbientPlayer;
}

const TABS: { id: CalmingTab; label: string; icon: typeof Wind }[] = [
  { id: 'BREATHING', label: 'Thở 2 phút', icon: Wind },
  { id: 'MUSIC', label: 'Nhạc dịu', icon: Disc3 },
];

/**
 * Panel Công cụ dịu: cùng khung và cách xếp chỗ với panel Trợ lý (bottom sheet dưới 640px, panel nổi
 * bên phải tới 1279px, cột thứ hai của trang viết từ 1280px). Không gọi mạng, không cần consent.
 */
export function CalmingToolsPanel({
  tab, onTabChange, onClose, breathing, onStartBreathing, onStopBreathing, player,
}: CalmingToolsPanelProps) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      role="complementary"
      aria-label="Công cụ dịu"
      className="fixed inset-x-3 bottom-[calc(env(safe-area-inset-bottom)+1rem)] z-40 flex h-[min(78dvh,680px)] flex-col overflow-hidden rounded-3xl border-2 border-black bg-paper-warm shadow-neo-lg animate-in fade-in slide-in-from-bottom-4 sm:inset-x-auto sm:bottom-4 sm:left-auto sm:right-4 sm:top-20 sm:h-auto sm:w-[380px] sm:rounded-2xl sm:slide-in-from-bottom-0 xl:sticky xl:top-20 xl:bottom-auto xl:right-auto xl:z-auto xl:h-[min(760px,calc(100dvh-6rem))] xl:w-auto xl:self-start xl:shadow-neo"
    >
      <div className="flex items-center justify-between border-b border-black/15 p-3">
        <div className="flex items-center gap-2">
          <span className="inline-flex h-7 w-7 items-center justify-center rounded-xl border border-black bg-sky-100 shadow-neo-xs">
            <Wind className="h-4 w-4 text-black" />
          </span>
          <div>
            <h3 className="font-space text-xs font-extrabold leading-tight text-black">Công cụ dịu</h3>
            <p className="text-[10px] font-medium text-gray-600">Dùng ngay, không cần trò chuyện · Chạy trên máy bạn</p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng công cụ dịu"
          className="rounded-lg p-1 text-gray-700 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div role="tablist" aria-label="Chọn công cụ dịu" className="grid grid-cols-2 gap-2 border-b border-black/15 p-3">
        {TABS.map(({ id, label, icon: Icon }) => {
          const selected = tab === id;
          return (
            <button
              key={id}
              type="button"
              role="tab"
              id={`calming-tab-${id}`}
              aria-selected={selected}
              aria-controls={`calming-tabpanel-${id}`}
              onClick={() => onTabChange(id)}
              className={`inline-flex items-center justify-center gap-1.5 rounded-xl border-2 border-black px-3 py-2 font-space text-xs font-extrabold transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                selected ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'
              }`}
            >
              <Icon className="h-4 w-4" aria-hidden="true" />
              {label}
            </button>
          );
        })}
      </div>

      <div
        role="tabpanel"
        id={`calming-tabpanel-${tab}`}
        aria-labelledby={`calming-tab-${tab}`}
        className="flex min-h-0 flex-1 flex-col overflow-y-auto"
      >
        {tab === 'BREATHING' ? (
          <BreathingExercise
            running={breathing.running}
            phase={breathing.phase}
            count={breathing.count}
            label={breathing.running ? breathing.label : '2:00'}
            onStart={onStartBreathing}
            onStop={onStopBreathing}
          />
        ) : (
          <VinylPlayer player={player} />
        )}
      </div>
    </div>
  );
}
