'use client';

import type { CSSProperties } from 'react';
import { Wind, Volume2, PenLine, X } from 'lucide-react';

interface CalmingToolsMenuProps {
  style: CSSProperties;
  onClose: () => void;
  onBreathing: () => void;
  onAmbient: () => void;
  onVent: () => void;
}

/**
 * Opens tools directly, without going through the companion chat: no API call, no consent needed.
 * Used from the "Công cụ dịu" toolbar button.
 */
export function CalmingToolsMenu({ style, onClose, onBreathing, onAmbient, onVent }: CalmingToolsMenuProps) {
  return (
    <div
      style={style}
      className="z-40 w-[min(260px,calc(100vw-2rem))] rounded-2xl border-2 border-black bg-paper-warm p-3 shadow-neo-lg animate-in fade-in"
      role="menu"
      aria-label="Công cụ dịu"
    >
      <div className="mb-2 flex items-center justify-between">
        <span className="font-space text-xs font-extrabold">Dùng ngay, không cần trò chuyện</span>
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng công cụ dịu"
          className="rounded-lg p-1 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
      <div className="flex flex-col gap-1.5">
        <button
          type="button"
          role="menuitem"
          onClick={onBreathing}
          className="flex items-center gap-2 rounded-xl border border-black bg-white p-2.5 text-left text-sm font-bold transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          <Wind className="h-4 w-4 shrink-0" />
          <span>Thở chậm 2 phút</span>
        </button>
        <button
          type="button"
          role="menuitem"
          onClick={onAmbient}
          className="flex items-center gap-2 rounded-xl border border-black bg-white p-2.5 text-left text-sm font-bold transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          <Volume2 className="h-4 w-4 shrink-0" />
          <span>Nghe âm thanh dịu</span>
        </button>
        <button
          type="button"
          role="menuitem"
          onClick={onVent}
          className="flex items-center gap-2 rounded-xl border border-black bg-white p-2.5 text-left text-sm font-bold transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          <PenLine className="h-4 w-4 shrink-0" />
          <span>Viết xả ra trang</span>
        </button>
      </div>
    </div>
  );
}
