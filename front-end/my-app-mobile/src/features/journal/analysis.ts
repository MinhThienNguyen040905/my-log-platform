import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, apiRequest } from '@/lib/api/client';

/** `GET /api/v1/journal-entries/{id}/analysis`. Chỉ khi `status === 'ANALYZED'` mới có nội dung. */
export type JournalAnalysis = {
  entryId: string;
  contentVersion: number;
  status: AnalysisStatus | string;
  analysisId: string | null;
  sentiment: string | null;
  sentimentScore: number | null;
  emotions: string[];
  topics: string[];
  reflection: string | null;
};

/** Giá trị hợp lệ của `journal_entries.analysis_status` (migration V5). */
export type AnalysisStatus = 'NOT_REQUESTED' | 'PENDING' | 'ANALYZING' | 'ANALYZED'
  | 'ANALYSIS_FAILED' | 'ANALYSIS_OUTDATED' | 'BLOCKED_BY_SAFETY';

const WAITING = new Set(['PENDING', 'ANALYZING', 'ANALYSIS_OUTDATED']);
export const isWaiting = (status: string | null | undefined) => !!status && WAITING.has(status);
export const canRetry = (status: string | null | undefined) => status === 'ANALYSIS_FAILED' || status === 'NOT_REQUESTED';

export async function getAnalysis(entryId: string): Promise<JournalAnalysis> {
  return (await apiRequest(`/journal-entries/${encodeURIComponent(entryId)}/analysis`)).json() as Promise<JournalAnalysis>;
}

export async function retryAnalysis(entryId: string): Promise<void> {
  await apiRequest(`/journal-entries/${encodeURIComponent(entryId)}/analysis:retry`, { method: 'POST' });
}

/** Nhãn cho mã cảm xúc / chủ đề mà backend chấp nhận (AnalysisOutputValidator). Mã lạ không hiển thị. */
export const EMOTION_LABELS: Record<string, string> = {
  JOY: 'Vui', CALM: 'Bình yên', HOPE: 'Hy vọng', SADNESS: 'Buồn', ANXIETY: 'Lo âu',
  ANGER: 'Tức giận', FEAR: 'Sợ hãi', MIXED: 'Đan xen', NEUTRAL: 'Trung tính',
};
export const TOPIC_LABELS: Record<string, string> = {
  WORK: 'Công việc', RELATIONSHIPS: 'Các mối quan hệ', HEALTH: 'Sức khỏe', SLEEP: 'Giấc ngủ',
  SELF_CARE: 'Chăm sóc bản thân', OTHER: 'Khác',
};

/** Backoff khi chờ phân tích: 2s → 4s → 8s → 16s → 30s…, dừng sau khoảng 2 phút. */
export const POLL_DELAYS_MS = [2000, 4000, 8000, 16000, 30000, 30000, 30000];

export function retryErrorMessage(cause: unknown): string {
  if (cause instanceof ApiError) {
    if (cause.status === 409) return 'Chưa thể phân tích lại. Kiểm tra quyền đồng ý xử lý AI trong Cài đặt, hoặc thử lại sau.';
    if (cause.status === 429) return 'Bạn vừa yêu cầu phân tích lại. Vui lòng đợi một lúc rồi thử lại.';
    return cause.message;
  }
  return 'Không thể yêu cầu phân tích lại.';
}

type State = { analysis: JournalAnalysis | null; error: string | null; timedOut: boolean };

/**
 * Đọc kết quả phân tích và tự hỏi lại với backoff khi đang chờ. Dừng khi có kết quả cuối, khi hết
 * khoảng 2 phút, hoặc khi màn hình đóng — không để request chạy ngầm sau khi rời màn hình.
 */
export function useEntryAnalysis(entryId: string | undefined, initialStatus: string | null | undefined) {
  const [state, setState] = useState<State>({ analysis: null, error: null, timedOut: false });
  const [round, setRound] = useState(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!entryId || !initialStatus || initialStatus === 'NOT_REQUESTED' && round === 0) return;
    let active = true;
    let attempt = 0;
    const load = async () => {
      try {
        const analysis = await getAnalysis(entryId);
        if (!active) return;
        setState({ analysis, error: null, timedOut: false });
        if (isWaiting(analysis.status)) {
          if (attempt >= POLL_DELAYS_MS.length) { setState((current) => ({ ...current, timedOut: true })); return; }
          timer.current = setTimeout(() => void load(), POLL_DELAYS_MS[attempt++]);
        }
      } catch (cause) {
        if (active) setState((current) => ({ ...current, error: cause instanceof Error ? cause.message : 'Không thể tải phân tích.' }));
      }
    };
    void load();
    return () => { active = false; if (timer.current) clearTimeout(timer.current); };
  }, [entryId, initialStatus, round]);

  const restart = useCallback(() => setRound((value) => value + 1), []);
  return { ...state, restart };
}
