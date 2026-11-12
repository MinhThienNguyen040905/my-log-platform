'use client';

import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MessageSquarePlus, Send } from 'lucide-react';
import { listMyFeedback, submitFeedback } from '../api/feedback';

const CATEGORIES = [
  { value: 'IDEA', label: 'Đề xuất cải tiến' },
  { value: 'BUG', label: 'Báo lỗi' },
  { value: 'OTHER', label: 'Góp ý khác' },
] as const;
const CATEGORY_LABELS: Record<string, string> = Object.fromEntries(CATEGORIES.map((c) => [c.value, c.label]));

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Mới tiếp nhận',
  IN_PROGRESS: 'Đang xử lý',
  RESOLVED: 'Đã giải quyết',
  CLOSED: 'Đã đóng',
};

/** Send feedback and follow what happened to it; the list comes from the user's own feedback, no ticket ids. */
export function UserFeedbackCard({ userId }: { userId: string }) {
  const queryClient = useQueryClient();
  const [category, setCategory] = useState<string>('IDEA');
  const [message, setMessage] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const queryKey = ['feedback', userId, 'mine'] as const;
  const mine = useQuery({ queryKey, queryFn: () => listMyFeedback() });
  const send = useMutation({
    mutationFn: () => submitFeedback(category, message.trim()),
    onSuccess: () => { setMessage(''); void queryClient.invalidateQueries({ queryKey }); },
  });

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!message.trim()) { setFormError('Vui lòng nhập nội dung góp ý.'); return; }
    setFormError(null);
    send.mutate();
  };

  const items = mine.data ?? [];
  return (
    <section aria-label="Gửi góp ý sản phẩm" className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-5">
      <div className="flex items-center gap-2 border-b border-black/15 pb-3">
        <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-xs">
          <MessageSquarePlus className="w-4 h-4 text-black" aria-hidden="true" />
        </div>
        <div>
          <h2 className="font-space text-lg font-extrabold text-black">Góp ý &amp; báo lỗi</h2>
          <p className="text-xs text-gray-600">Gửi đề xuất hoặc báo lỗi cho đội ngũ phát triển. Đừng dán nội dung nhật ký riêng tư vào đây.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3 text-sm">
        <label className="flex flex-col gap-1 font-space font-bold text-black sm:max-w-xs">
          <span className="text-xs">Loại góp ý</span>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            aria-label="Phân loại góp ý"
            className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 text-sm font-bold text-black"
          >
            {CATEGORIES.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1 font-space font-bold text-black">
          <span className="text-xs">Nội dung</span>
          <textarea
            rows={3}
            maxLength={4000}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Mô tả đề xuất hoặc vấn đề bạn gặp..."
            aria-label="Nội dung góp ý"
            className="rounded-xl border-2 border-black bg-paper-warm/30 p-3 text-sm font-normal text-black focus:bg-white focus:outline-none"
          />
          <span className="self-end font-mono text-[11px] font-normal text-gray-500">{message.length}/4000</span>
        </label>
        {(formError || send.isError) && (
          <p role="alert" className="text-sm font-bold text-red-700">
            {formError || (send.error instanceof Error ? send.error.message : 'Chưa thể gửi góp ý lúc này. Vui lòng thử lại.')}
          </p>
        )}
        {send.isSuccess && !message && (
          <p role="status" className="text-sm font-bold text-emerald-800 bg-emerald-50 border border-emerald-300 rounded-xl px-3 py-2">
            Đã gửi góp ý thành công. Bạn có thể theo dõi trạng thái ngay bên dưới.
          </p>
        )}
        <div>
          <button
            type="submit"
            disabled={send.isPending}
            className="inline-flex items-center gap-1.5 rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-sm font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer disabled:opacity-50"
          >
            <Send className="w-4 h-4" aria-hidden="true" />
            <span>{send.isPending ? 'Đang gửi...' : 'Gửi góp ý'}</span>
          </button>
        </div>
      </form>

      <div className="border-t border-black/15 pt-4 flex flex-col gap-3">
        <h3 className="font-space text-sm font-extrabold text-black">Góp ý của bạn</h3>
        {mine.isPending && <p role="status" className="text-sm text-gray-600">Đang tải...</p>}
        {mine.isError && (
          <p role="alert" className="text-sm text-red-700">
            Không tải được góp ý đã gửi. <button type="button" className="underline" onClick={() => void mine.refetch()}>Thử lại</button>
          </p>
        )}
        {mine.isSuccess && items.length === 0 && <p className="text-sm text-gray-600">Bạn chưa gửi góp ý nào.</p>}
        {items.length > 0 && (
          <ul className="flex flex-col gap-2">
            {items.map((item) => (
              <li key={item.id} className="rounded-xl border border-black/30 bg-paper-warm p-3 flex flex-col gap-1.5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-space text-xs font-extrabold text-black">
                    {CATEGORY_LABELS[item.category] ?? item.category} · {new Date(item.createdAt).toLocaleDateString('vi-VN')}
                  </span>
                  <span className="px-2 py-0.5 rounded border border-black bg-white font-space text-[11px] font-bold">
                    {STATUS_LABELS[item.status] ?? item.status}
                  </span>
                </div>
                <p className="text-sm text-gray-800 whitespace-pre-wrap line-clamp-3">{item.message}</p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
