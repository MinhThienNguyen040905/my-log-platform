'use client';

import React, { useState } from 'react';
import { MessageSquarePlus, Search, Send } from 'lucide-react';
import { getFeedback, submitFeedback, type Feedback } from '../api/feedback';

const CATEGORIES = [
  { value: 'IDEA', label: 'Đề xuất cải tiến (IDEA)' },
  { value: 'BUG', label: 'Báo lỗi kỹ thuật (BUG)' },
  { value: 'OTHER', label: 'Góp ý khác (OTHER)' },
] as const;

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Mới tiếp nhận (OPEN)',
  IN_PROGRESS: 'Đang xử lý (IN_PROGRESS)',
  RESOLVED: 'Đã giải quyết (RESOLVED)',
  CLOSED: 'Đã đóng (CLOSED)',
};

export function UserFeedbackCard() {
  const [category, setCategory] = useState<string>('IDEA');
  const [message, setMessage] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submittedId, setSubmittedId] = useState<string | null>(null);

  const [lookupId, setLookupId] = useState<string>('');
  const [checking, setChecking] = useState(false);
  const [checkedFeedback, setCheckedFeedback] = useState<Feedback | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const clean = message.trim();
    if (!clean) {
      setSubmitError('Vui lòng nhập nội dung góp ý.');
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      const created = await submitFeedback(category, clean);
      setSubmittedId(created.id);
      setLookupId(created.id);
      setMessage('');
      const detail = await getFeedback(created.id);
      setCheckedFeedback(detail);
    } catch (err) {
      setSubmitError(
        err instanceof Error ? err.message : 'Chưa thể gửi góp ý lúc này. Vui lòng thử lại.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCheckStatus = async (event: React.FormEvent) => {
    event.preventDefault();
    const id = lookupId.trim();
    if (!id) return;
    setChecking(true);
    setLookupError(null);
    try {
      const detail = await getFeedback(id);
      setCheckedFeedback(detail);
    } catch (err) {
      setCheckedFeedback(null);
      setLookupError(
        err instanceof Error ? err.message : 'Không tìm thấy phiếu góp ý với mã này.',
      );
    } finally {
      setChecking(false);
    }
  };

  return (
    <section
      aria-label="Gửi góp ý sản phẩm"
      className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-5"
    >
      <div className="flex items-center gap-2 border-b border-black/15 pb-3">
        <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-xs">
          <MessageSquarePlus className="w-4 h-4 text-black" />
        </div>
        <div>
          <h2 className="font-space text-lg font-extrabold text-black">Góp Ý &amp; Báo Lỗi</h2>
          <p className="text-xs text-gray-600">
            Chia sẻ đề xuất hoặc báo lỗi cho đội ngũ phát triển (không đính kèm nội dung nhật ký riêng tư).
          </p>
        </div>
      </div>

      <form onSubmit={(e) => void handleSubmit(e)} className="flex flex-col gap-3 text-xs">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Phân loại góp ý</span>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              aria-label="Phân loại góp ý"
              className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 text-xs font-bold text-black"
            >
              {CATEGORIES.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="flex flex-col gap-1 font-space font-bold text-black">
          <span>Nội dung góp ý (tối đa 4.000 ký tự)</span>
          <textarea
            rows={3}
            maxLength={4000}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            required
            placeholder="Mô tả chi tiết đề xuất hoặc vấn đề bạn gặp phải..."
            aria-label="Nội dung góp ý"
            className="rounded-xl border-2 border-black bg-paper-warm/30 p-3 text-xs font-normal text-black focus:bg-white focus:outline-none"
          />
        </label>

        {submitError && (
          <p role="alert" className="text-xs font-bold text-red-700">
            {submitError}
          </p>
        )}

        {submittedId && (
          <p role="status" className="text-xs font-bold text-emerald-800 bg-emerald-50 border border-emerald-300 rounded-xl px-3 py-2">
            Đã gửi góp ý thành công! Mã phiếu của bạn: <code className="font-mono">{submittedId}</code>
          </p>
        )}

        <div>
          <button
            type="submit"
            disabled={submitting}
            className="inline-flex items-center gap-1.5 rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer disabled:opacity-50"
          >
            <Send className="w-3.5 h-3.5" />
            <span>{submitting ? 'Đang gửi...' : 'Gửi góp ý'}</span>
          </button>
        </div>
      </form>

      {/* Tra cứu trạng thái phiếu góp ý đã gửi */}
      <div className="border-t border-black/15 pt-4 flex flex-col gap-3 text-xs">
        <form onSubmit={(e) => void handleCheckStatus(e)} className="flex flex-wrap items-end gap-2">
          <label className="flex-1 min-w-[220px] flex flex-col gap-1 font-space font-bold text-black">
            <span>Kiểm tra trạng thái phiếu góp ý (theo mã UUID)</span>
            <input
              type="text"
              value={lookupId}
              onChange={(e) => setLookupId(e.target.value)}
              placeholder="Nhập mã UUID phiếu góp ý..."
              aria-label="Mã UUID phiếu góp ý"
              className="rounded-xl border border-black bg-paper-warm/30 px-3 py-2 text-xs font-mono text-black"
            />
          </label>
          <button
            type="submit"
            disabled={checking || !lookupId.trim()}
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-white px-3.5 py-2 font-space text-xs font-bold text-black hover:bg-paper-warm cursor-pointer disabled:opacity-50"
          >
            <Search className="w-3.5 h-3.5" />
            <span>{checking ? 'Đang tra cứu...' : 'Xem trạng thái'}</span>
          </button>
        </form>

        {lookupError && (
          <p role="alert" className="text-xs text-red-700 font-bold">
            {lookupError}
          </p>
        )}

        {checkedFeedback && (
          <div className="rounded-xl border border-black/30 bg-paper-warm p-3.5 flex flex-col gap-1.5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="font-space font-extrabold text-black">
                Phiếu #{checkedFeedback.id.slice(0, 8)} · {checkedFeedback.category}
              </span>
              <span className="px-2 py-0.5 rounded border border-black bg-white font-space text-[11px] font-bold">
                {STATUS_LABELS[checkedFeedback.status] ?? checkedFeedback.status}
              </span>
            </div>
            <p className="text-gray-800 whitespace-pre-wrap">{checkedFeedback.message}</p>
          </div>
        )}
      </div>
    </section>
  );
}
