'use client';

import React from 'react';
import { Lightbulb, X } from 'lucide-react';
import { getCompanionTitleSuggestions } from '../../api/companion';

interface CompanionTitleSuggestionsProps {
  getMood: () => string | null;
  onApply: (title: string) => void;
}

type LoadState = { status: 'idle' } | { status: 'loading' } | { status: 'ready'; titles: string[] } | { status: 'error' };

/**
 * Nút "Gợi ý tựa đề" luôn có trong khung chat, ở mọi phiên và mọi trạng thái (kể cả chưa consent
 * hay trợ lý tạm nghỉ). Tựa đề soạn sẵn theo cảm xúc đang chọn, lấy lúc bấm để khớp cảm xúc hiện tại.
 */
export function CompanionTitleSuggestions({ getMood, onApply }: CompanionTitleSuggestionsProps) {
  const [open, setOpen] = React.useState(false);
  const [state, setState] = React.useState<LoadState>({ status: 'idle' });

  const toggle = () => {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    setState({ status: 'loading' });
    getCompanionTitleSuggestions(getMood())
      .then((titles) => setState({ status: 'ready', titles }))
      .catch(() => setState({ status: 'error' }));
  };

  return (
    <div className="border-b border-black/15 px-3 py-2">
      <button
        type="button"
        onClick={toggle}
        aria-expanded={open}
        className="inline-flex items-center gap-1.5 rounded-lg border border-black bg-white px-2.5 py-1 font-space text-[11px] font-bold transition-all hover:-translate-y-0.5 hover:bg-amber-50 hover:shadow-neo-xs focus-visible:outline-2 focus-visible:outline-black"
      >
        {open ? <X className="h-3.5 w-3.5" /> : <Lightbulb className="h-3.5 w-3.5 text-amber-600" />}
        <span>{open ? 'Đóng gợi ý tựa đề' : 'Gợi ý tựa đề'}</span>
      </button>

      {open && (
        <div className="mt-2 flex flex-col gap-1.5" role="list" aria-label="Tựa đề gợi ý">
          {state.status === 'loading' && <p className="text-[11px] text-gray-600">Đang tải gợi ý...</p>}
          {state.status === 'error' && (
            <p role="alert" className="text-[11px] text-gray-600">Chưa tải được gợi ý tựa đề. Thử lại sau nhé.</p>
          )}
          {state.status === 'ready' &&
            state.titles.map((title) => (
              <button
                key={title}
                type="button"
                role="listitem"
                onClick={() => {
                  onApply(title);
                  setOpen(false);
                }}
                className="rounded-xl border border-black bg-white px-2.5 py-1.5 text-left text-xs leading-snug transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-xs focus-visible:outline-2 focus-visible:outline-black"
              >
                {title}
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
