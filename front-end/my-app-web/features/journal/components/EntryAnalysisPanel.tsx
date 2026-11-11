'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bot } from 'lucide-react';
import { ApiError } from '@/lib/api/client';
import { getJournalAnalysis, retryJournalAnalysis } from '../api/analysis';
import { useJournal } from '../context/JournalContext';
import {
  ANALYSIS_EMOTION_LABELS,
  ANALYSIS_POLL_DELAYS_MS,
  ANALYSIS_TOPIC_LABELS,
  canRetryAnalysis,
  isAnalysisWaiting,
} from '../utils/analysis-status';
import { SafetyModal } from './SafetyModal';

type Props = { entryId: string; contentVersion?: number; riskLevel?: string };

function retryMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 409) {
    return 'Chưa thể phân tích lại. Kiểm tra quyền đồng ý xử lý AI trong Hồ sơ & cài đặt, hoặc thử lại sau.';
  }
  if (error instanceof ApiError && error.status === 429) return 'Bạn vừa yêu cầu phân tích lại. Vui lòng đợi một lúc rồi thử lại.';
  return 'Chưa thể yêu cầu phân tích lại. Vui lòng thử sau.';
}

/**
 * "Gợi ý nhìn lại" của một bài viết, đọc từ `GET /journal-entries/{id}/analysis`.
 * Chỉ hiện phần dành cho người dùng (lời gợi ý, cảm xúc, chủ đề) — không hiện điểm số, ID hay mã trạng thái.
 * Khi đang chờ thì hỏi lại với thời gian giãn dần và dừng sau khoảng 2 phút.
 */
export function EntryAnalysisPanel({ entryId, contentVersion, riskLevel }: Props) {
  const { userId } = useJournal();
  const queryClient = useQueryClient();
  const [safetyOpen, setSafetyOpen] = useState(false);
  const queryKey = ['analysis', userId, entryId, contentVersion ?? null] as const;
  const analysis = useQuery({
    queryKey,
    queryFn: () => getJournalAnalysis(entryId),
    gcTime: 0,
    retry: false,
    refetchInterval: (query) => {
      if (!isAnalysisWaiting(query.state.data?.status)) return false;
      const attempt = query.state.dataUpdateCount - 1;
      return attempt < ANALYSIS_POLL_DELAYS_MS.length ? ANALYSIS_POLL_DELAYS_MS[attempt] : false;
    },
  });
  const retry = useMutation({
    mutationFn: () => retryJournalAnalysis(entryId),
    retry: false,
    onSuccess: () => queryClient.resetQueries({ queryKey }),
  });

  const data = analysis.data;
  const status = data?.status;
  const updates = queryClient.getQueryState(queryKey)?.dataUpdateCount ?? 0;
  const timedOut = isAnalysisWaiting(status) && updates > ANALYSIS_POLL_DELAYS_MS.length;
  const highRisk = riskLevel === 'HIGH' || riskLevel === 'CRITICAL';
  const emotions = (data?.emotions ?? []).map((code) => ANALYSIS_EMOTION_LABELS[code]).filter(Boolean);
  const topics = (data?.topics ?? []).map((code) => ANALYSIS_TOPIC_LABELS[code]).filter(Boolean);

  return (
    <section aria-labelledby={`analysis-${entryId}`} className="p-5 bg-surface-card rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-3">
      <h3 id={`analysis-${entryId}`} className="flex items-center gap-2 border-b border-black pb-2 font-space text-sm font-extrabold uppercase text-black">
        <Bot className="w-5 h-5" aria-hidden="true" />
        Gợi ý nhìn lại
      </h3>

      {analysis.isPending && <p role="status" className="text-sm">Đang tải gợi ý...</p>}
      {analysis.isError && (
        <p role="alert" className="text-sm text-red-700">
          Chưa thể tải gợi ý.{' '}
          <button type="button" className="underline" onClick={() => void analysis.refetch()}>Tải lại</button>
        </p>
      )}

      {data && (
        status === 'ANALYZED' && data.reflection ? (
          <div className="flex flex-col gap-3 text-sm">
            <p className="rounded-xl border border-black bg-paper-warm p-4 leading-relaxed whitespace-pre-wrap">{data.reflection}</p>
            {emotions.length > 0 && <Chips label="Cảm xúc" items={emotions} />}
            {topics.length > 0 && <Chips label="Chủ đề" items={topics} />}
            <p className="text-xs text-gray-600">Đây là gợi ý để tự suy ngẫm, không phải chẩn đoán.</p>
          </div>
        ) : status === 'ANALYZED' ? (
          <p role="status" className="text-sm">Bài viết này chưa có gợi ý nhìn lại.</p>
        ) : isAnalysisWaiting(status) ? (
          timedOut
            ? <p role="status" className="text-sm">Gợi ý đang mất nhiều thời gian hơn bình thường. Hãy quay lại sau ít phút.</p>
            : <p role="status" className="text-sm">{status === 'ANALYSIS_OUTDATED' ? 'Bạn vừa sửa bài, gợi ý đang được cập nhật…' : 'Đang đọc trang viết của bạn…'}</p>
        ) : status === 'BLOCKED_BY_SAFETY' ? (
          <div className="flex flex-col gap-2 text-sm">
            <p>Trang này không có gợi ý tự động. Nếu bạn đang thấy khó khăn, bạn có thể xem các cách tìm hỗ trợ.</p>
            {highRisk && (
              <button type="button" onClick={() => setSafetyOpen(true)} className="self-start rounded-xl border-2 border-black bg-white px-3 py-1.5 font-space text-xs font-bold shadow-neo-sm cursor-pointer">
                Xem hỗ trợ an toàn
              </button>
            )}
          </div>
        ) : status === 'ANALYSIS_FAILED' ? (
          <p className="text-sm">Chưa tạo được gợi ý cho trang này.</p>
        ) : (
          <p className="text-sm">Trang này chưa được phân tích.</p>
        )
      )}

      {data && canRetryAnalysis(status) && (
        <button
          type="button"
          disabled={retry.isPending}
          onClick={() => retry.mutate()}
          className="self-start rounded-xl border-2 border-black bg-white px-3 py-1.5 font-space text-xs font-bold shadow-neo-sm cursor-pointer disabled:opacity-50"
        >
          {retry.isPending ? 'Đang gửi...' : 'Phân tích lại'}
        </button>
      )}
      {retry.isError && <p role="alert" className="text-sm text-red-700">{retryMessage(retry.error)}</p>}

      <SafetyModal isOpen={safetyOpen} onClose={() => setSafetyOpen(false)} />
    </section>
  );
}

function Chips({ label, items }: { label: string; items: string[] }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="font-space text-xs font-bold uppercase text-gray-500">{label}</span>
      <ul className="flex flex-wrap gap-2" aria-label={label}>
        {items.map((item) => (
          <li key={item} className="rounded-xl border border-black bg-white px-2.5 py-1 font-space text-xs font-bold">{item}</li>
        ))}
      </ul>
    </div>
  );
}
