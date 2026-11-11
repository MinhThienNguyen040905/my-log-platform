/**
 * `AnalysisResponse.status` = `journal_entries.analysis_status` (backend migration V5):
 * NOT_REQUESTED, PENDING, ANALYZING, ANALYZED, ANALYSIS_FAILED, ANALYSIS_OUTDATED, BLOCKED_BY_SAFETY.
 */
const WAITING = new Set(['PENDING', 'ANALYZING', 'ANALYSIS_OUTDATED']);
export const isAnalysisWaiting = (status: string | null | undefined) => !!status && WAITING.has(status);
export const canRetryAnalysis = (status: string | null | undefined) => status === 'ANALYSIS_FAILED' || status === 'NOT_REQUESTED';

/** Backoff while waiting: 2s → 4s → 8s → 16s → 30s…, then stop (about 2 minutes in total). */
export const ANALYSIS_POLL_DELAYS_MS = [2000, 4000, 8000, 16000, 30000, 30000, 30000];

/** Labels for the emotion/topic codes the backend accepts (AnalysisOutputValidator). Unknown codes are hidden. */
export const ANALYSIS_EMOTION_LABELS: Record<string, string> = {
  JOY: 'Vui', CALM: 'Bình yên', HOPE: 'Hy vọng', SADNESS: 'Buồn', ANXIETY: 'Lo âu',
  ANGER: 'Tức giận', FEAR: 'Sợ hãi', MIXED: 'Đan xen', NEUTRAL: 'Trung tính',
};
export const ANALYSIS_TOPIC_LABELS: Record<string, string> = {
  WORK: 'Công việc', RELATIONSHIPS: 'Các mối quan hệ', HEALTH: 'Sức khỏe', SLEEP: 'Giấc ngủ',
  SELF_CARE: 'Chăm sóc bản thân', OTHER: 'Khác',
};
