'use client';

import { Music, X } from 'lucide-react';
import type { AmbientTrack } from '../../utils/ambient/ambient-tracks';

/**
 * Gợi ý nhạc nền theo cảm xúc vừa chọn. Chỉ là lời mời — không tự phát; bấm vào mới phát.
 */
export function AmbientSuggestionChip({ track, onPlay, onDismiss }: { track: AmbientTrack; onPlay: () => void; onDismiss: () => void }) {
  return (
    <div className="flex w-fit max-w-full items-center gap-1 rounded-xl border-2 border-black bg-sky-50 py-1 pl-1 pr-1.5 shadow-neo-xs animate-in fade-in">
      <button
        type="button"
        onClick={onPlay}
        className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 font-space text-xs font-bold hover:bg-white cursor-pointer"
      >
        <Music className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
        <span className="truncate">Gợi ý nghe: {track.title}</span>
      </button>
      <button type="button" onClick={onDismiss} aria-label="Ẩn gợi ý nhạc nền" className="rounded-lg p-1 hover:bg-white cursor-pointer">
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
