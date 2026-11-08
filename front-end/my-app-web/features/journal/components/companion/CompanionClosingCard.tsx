'use client';

import { Check } from 'lucide-react';

interface CompanionClosingCardProps {
  rating: number | null;
  onRate: (value: number) => void;
  hasMessages: boolean;
  onInsertConversation: () => void;
  onJustClose: () => void;
}

/** Điểm 1–5 chỉ để người dùng tự nhìn lại — không gửi đi, không lưu ở đâu cả. */
export function CompanionClosingCard({ rating, onRate, hasMessages, onInsertConversation, onJustClose }: CompanionClosingCardProps) {
  return (
    <div className="m-3 flex flex-col gap-2.5 rounded-2xl border-2 border-black bg-lime-50 p-3 text-xs shadow-neo-xs">
      <p className="font-space font-extrabold text-black">Giờ trong lòng bạn thấy thế nào?</p>
      <div className="flex items-center justify-center gap-5">
        {[1, 2, 3, 4, 5].map((score) => (
          <button
            key={score}
            type="button"
            onClick={() => onRate(score)}
            aria-label={`Đánh giá tâm trạng mức ${score} trên 5`}
            aria-pressed={rating === score}
            className={`flex h-8 w-8 items-center justify-center rounded-xl border border-black font-space text-xs font-extrabold transition-all ${
              rating === score ? 'scale-105 bg-primary-container shadow-neo-xs' : 'bg-white hover:bg-lime-100'
            }`}
          >
            {score}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap items-center gap-2 pt-1">
        {hasMessages && (
          <button
            type="button"
            onClick={onInsertConversation}
            aria-label="Đưa cuộc trò chuyện vào bài nhật ký"
            className="inline-flex flex-1 items-center justify-center gap-1 rounded-xl border border-black bg-primary-container px-2.5 py-1.5 font-space text-[11px] font-extrabold shadow-neo-xs hover:bg-lime-200"
          >
            <Check className="h-3.5 w-3.5" />
            <span>Đưa vào bài nhật ký</span>
          </button>
        )}
        <button
          type="button"
          onClick={onJustClose}
          aria-label="Kết thúc cuộc trò chuyện, không đưa vào bài"
          className="rounded-xl border border-black bg-white px-2.5 py-1.5 font-space text-[11px] font-bold hover:bg-gray-100"
        >
          Kết thúc, không lưu
        </button>
      </div>
    </div>
  );
}
