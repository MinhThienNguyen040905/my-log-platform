'use client';

import { CheckCircle2, PlusCircle } from 'lucide-react';
import type { CompanionHabitSuggestion } from '../../types/companion.types';

interface CompanionHabitCardProps {
  suggestion: CompanionHabitSuggestion;
  onAccept: (suggestion: CompanionHabitSuggestion) => void;
  onDismiss: () => void;
}

/**
 * Thẻ gợi ý thói quen xuất hiện sau lượt `TOOL_COMPLETED` hoặc `CLOSING` (tối đa 1 thẻ mỗi ngày,
 * không bao giờ hiện khi cảm xúc là `very_bad`). Chỉ ghi dữ liệu khi người dùng chủ động bấm.
 */
export function CompanionHabitCard({ suggestion, onAccept, onDismiss }: CompanionHabitCardProps) {
  const isLogExisting = suggestion.type === 'LOG_EXISTING';

  return (
    <div
      role="region"
      aria-label="Gợi ý thói quen tự chăm sóc"
      className="mx-3 mb-2 rounded-2xl border-2 border-black bg-amber-50 p-3 text-xs shadow-neo-xs"
    >
      <p className="font-space text-[11px] font-extrabold text-black">Nhịp sống ổn định lâu dài</p>
      <p className="mt-1 leading-relaxed text-gray-800">{suggestion.text}</p>
      <div className="mt-2.5 flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => onAccept(suggestion)}
          aria-label={isLogExisting ? 'Ghi nhận hoàn thành thói quen hôm nay' : 'Xem và tạo thói quen từ mẫu'}
          className="inline-flex items-center gap-1 rounded-xl border border-black bg-primary-container px-2.5 py-1.5 font-space text-[11px] font-extrabold text-black shadow-neo-xs transition-all hover:-translate-y-0.5 hover:bg-lime-200"
        >
          {isLogExisting ? <CheckCircle2 className="h-3.5 w-3.5" /> : <PlusCircle className="h-3.5 w-3.5" />}
          <span>{isLogExisting ? 'Ghi nhận hôm nay' : 'Xem & tạo thói quen'}</span>
        </button>
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Không cảm ơn, tạm ẩn gợi ý thói quen 7 ngày"
          className="rounded-xl border border-black bg-white px-2.5 py-1.5 font-space text-[11px] font-bold text-gray-700 transition-colors hover:bg-gray-100"
        >
          Không, cảm ơn
        </button>
      </div>
    </div>
  );
}
