'use client';

interface CompanionConsentCardProps {
  onGrant: () => void;
  onDeclineForNow: () => void;
}

/** "Để sau": chỉ còn công cụ dịu, ô nhắn bị khóa cho tới khi người dùng đồng ý — xem CompanionPanel. */
export function CompanionConsentCard({ onGrant, onDeclineForNow }: CompanionConsentCardProps) {
  return (
    <div className="m-3 rounded-xl border-2 border-black bg-amber-50 p-3 text-xs leading-relaxed shadow-neo-xs">
      <p className="font-space font-bold text-black">Cho phép trợ lý đọc bài bạn đang viết?</p>
      <p className="mt-1 text-gray-700">
        Để trò chuyện và gợi ý đúng với bài bạn đang viết, trợ lý cần đọc tin nhắn và nội dung bài. Không lưu lại sau khi đóng.
      </p>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={onGrant}
          aria-label="Đồng ý cho phép trợ lý đọc bài đang viết"
          className="rounded-lg border border-black bg-primary-container px-2.5 py-1 font-space text-[11px] font-bold transition-all hover:-translate-y-0.5 hover:shadow-neo-xs"
        >
          Đồng ý
        </button>
        <button
          type="button"
          onClick={onDeclineForNow}
          aria-label="Để sau, chỉ dùng công cụ dịu"
          className="rounded-lg border border-black bg-white px-2.5 py-1 font-space text-[11px] font-bold transition-all hover:-translate-y-0.5 hover:shadow-neo-xs"
        >
          Để sau (chỉ dùng công cụ)
        </button>
      </div>
    </div>
  );
}
