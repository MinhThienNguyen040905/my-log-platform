'use client';

import { Volume2 } from 'lucide-react';

/** Presentational only; the audio itself is started/stopped by {@link useCalmingTools}. */
export function AmbientSoundWidget() {
  return (
    <div className="flex items-center gap-3">
      <span aria-hidden="true" className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-black bg-sky-100">
        <Volume2 className="h-4 w-4" />
      </span>
      <div className="flex flex-col leading-tight">
        <span className="font-space text-xs font-extrabold">Đang phát âm thanh dịu</span>
        <span className="text-xs text-gray-600">Tiếng mưa nhẹ, không cần mạng</span>
      </div>
    </div>
  );
}
