'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Sparkles, X } from 'lucide-react';
import { getJournalAnalysis, retryJournalAnalysis } from '../api/analysis';
import { useJournal } from '../context/JournalContext';

type Props = {
  isOpen: boolean;
  onClose: () => void;
  entryId: string | null;
  contentVersion: number | null;
};

export function AiReflectionDrawer({ isOpen, onClose, entryId, contentVersion }: Props) {
  const { userId } = useJournal();
  const queryClient = useQueryClient();
  const queryKey = ['analysis', userId, entryId, contentVersion] as const;
  const analysis = useQuery({
    queryKey,
    queryFn: () => getJournalAnalysis(entryId!),
    enabled: isOpen && !!entryId && contentVersion !== null,
    gcTime: 0,
    retry: false,
    refetchInterval: (query) =>
      query.state.data && ['PENDING', 'ANALYZING', 'ANALYSIS_OUTDATED'].includes(query.state.data.status)
        ? 5000
        : false,
  });
  const retry = useMutation({
    mutationFn: () => retryJournalAnalysis(entryId!),
    retry: false,
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  if (!isOpen) return null;

  const data = analysis.data;
  const isCurrent = data?.contentVersion === contentVersion;
  const status = data?.status;

  return (
    <>
      <div onClick={onClose} className="fixed inset-0 z-40 bg-black/40 lg:hidden" aria-hidden="true" />
      <aside className="fixed top-20 right-0 z-50 flex h-[calc(100vh-80px)] w-full flex-col gap-5 overflow-y-auto border-l-2 border-black bg-surface-card p-6 shadow-neo-lg sm:w-[420px]" aria-label="Gợi ý nhìn lại">
        <div className="flex items-center justify-between border-b-2 border-black pb-3">
          <h3 className="flex items-center gap-2 font-space text-base font-extrabold"><Sparkles className="h-5 w-5" />Gợi ý nhìn lại</h3>
          <button type="button" onClick={onClose} aria-label="Đóng gợi ý" className="rounded-xl border border-black p-2"><X className="h-4 w-4" /></button>
        </div>

        {analysis.isPending && <p>Đang tải trạng thái phân tích...</p>}
        {analysis.isError && (
          <div role="alert" className="space-y-3">
            <p>Chưa thể tải phân tích. Vui lòng thử lại.</p>
            <button type="button" onClick={() => void analysis.refetch()} className="rounded-xl border border-black px-3 py-2 font-bold">Tải lại</button>
          </div>
        )}
        {data && !isCurrent && <p role="status">Bài viết đã có phiên bản mới. Hãy mở lại bài viết để xem phân tích phù hợp.</p>}
        {data && isCurrent && (
          <div className="space-y-4 text-sm">
            {status === 'ANALYZED' && data.reflection ? (
              <div className="rounded-2xl border border-black bg-paper-warm p-4 leading-relaxed whitespace-pre-wrap">{data.reflection}</div>
            ) : status === 'ANALYZED' ? (
              <p role="status">Phân tích đã hoàn tất. Hiện chưa có gợi ý nhìn lại cho bài viết này.</p>
            ) : status === 'PENDING' || status === 'ANALYZING' ? (
              <p role="status">Đang xử lý phân tích. Trạng thái sẽ tự cập nhật.</p>
            ) : status === 'FAILED' ? (
              <div className="space-y-3">
                <p role="status">Phân tích chưa thành công.</p>
                <button type="button" disabled={retry.isPending} onClick={() => retry.mutate()} className="rounded-xl border border-black px-3 py-2 font-bold disabled:opacity-50">{retry.isPending ? 'Đang gửi...' : 'Thử phân tích lại'}</button>
              </div>
            ) : (
              <p role="status">Chưa có gợi ý cho bài viết này. Trạng thái: {status ?? 'chưa xác định'}.</p>
            )}
            {retry.isError && <p role="alert">Chưa thể yêu cầu phân tích lại. Vui lòng thử sau.</p>}
          </div>
        )}
      </aside>
    </>
  );
}
