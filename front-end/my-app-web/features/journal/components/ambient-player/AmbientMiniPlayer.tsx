'use client';

import { Maximize2, Pause, Play, SkipForward, X } from 'lucide-react';
import type { AmbientPlayer } from '../../hooks/useAmbientPlayer';
import { Equalizer, SpinningDisc } from './PlayerVisuals';

const ICON_BUTTON_BASE =
  'h-9 w-9 shrink-0 items-center justify-center rounded-full transition-colors hover:bg-lime-100 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black';
const ICON_BUTTON = `inline-flex ${ICON_BUTTON_BASE}`;

/**
 * Thanh nhạc nhỏ cố định ở đáy màn hình khi đã đóng panel Công cụ dịu nhưng nhạc vẫn mở, để tạm
 * dừng/đổi bài mà không phải mở lại panel. Trên điện thoại chừa chỗ cho nút công cụ nổi bên phải.
 */
export function AmbientMiniPlayer({ player, onExpand }: { player: AmbientPlayer; onExpand: () => void }) {
  const playing = player.status === 'playing';
  return (
    <section
      aria-label="Thanh nhạc nền"
      className="fixed bottom-[calc(env(safe-area-inset-bottom)+5.5rem)] left-3 right-20 z-30 flex items-center gap-2 rounded-2xl border-2 border-black bg-paper-warm py-2 pl-2 pr-1.5 shadow-neo animate-in fade-in slide-in-from-bottom-2 md:bottom-4 md:left-1/2 md:right-auto md:w-[420px] md:-translate-x-1/2"
    >
      <SpinningDisc playing={playing} className="h-10 w-10" />
      <button type="button" onClick={onExpand} className="flex min-w-0 flex-1 flex-col text-left leading-tight cursor-pointer" title="Mở máy phát">
        <span className="truncate font-space text-xs font-extrabold">{player.track.title}</span>
        <span className="truncate text-[11px] text-gray-600">
          {player.notice ?? (playing ? 'Đang phát' : player.status === 'paused' ? 'Tạm dừng' : 'Đã dừng')}
        </span>
      </button>
      <span className="hidden sm:inline-flex"><Equalizer playing={playing} /></span>
      <button
        type="button"
        onClick={player.togglePlay}
        aria-label={playing ? 'Tạm dừng nhạc nền' : 'Phát nhạc nền'}
        className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-black bg-black text-white cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
      >
        {playing ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4 translate-x-px" />}
      </button>
      <button type="button" onClick={player.next} aria-label="Bài sau" title="Bài sau" className={`hidden sm:inline-flex ${ICON_BUTTON_BASE}`}>
        <SkipForward className="h-4 w-4" />
      </button>
      <button type="button" onClick={onExpand} aria-label="Mở máy phát nhạc nền" title="Mở máy phát" className={ICON_BUTTON}>
        <Maximize2 className="h-4 w-4" />
      </button>
      <button type="button" onClick={player.close} aria-label="Tắt và đóng nhạc nền" title="Tắt nhạc nền" className={ICON_BUTTON}>
        <X className="h-4 w-4" />
      </button>
    </section>
  );
}
