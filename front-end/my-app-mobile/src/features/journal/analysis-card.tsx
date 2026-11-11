import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';
import { canRetry, EMOTION_LABELS, isWaiting, retryAnalysis, retryErrorMessage, TOPIC_LABELS, useEntryAnalysis } from './analysis';

type Props = { entryId: string; initialStatus: string | null; onSafety: () => void; showSafety: boolean };

/**
 * Kết quả phân tích của một trang. Chỉ hiện phần dành cho người dùng (lời gợi ý nhìn lại, cảm xúc,
 * chủ đề); không hiện điểm số, ID hay metadata kỹ thuật.
 */
export function AnalysisCard({ entryId, initialStatus, onSafety, showSafety }: Props) {
  const { analysis, error, timedOut, restart } = useEntryAnalysis(entryId, initialStatus);
  const [busy, setBusy] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);
  const status = analysis?.status ?? initialStatus;

  const retry = async () => {
    setBusy(true); setRetryError(null);
    try { await retryAnalysis(entryId); restart(); }
    catch (cause) { setRetryError(retryErrorMessage(cause)); }
    finally { setBusy(false); }
  };

  const emotions = (analysis?.emotions ?? []).map((code) => EMOTION_LABELS[code]).filter(Boolean);
  const topics = (analysis?.topics ?? []).map((code) => TOPIC_LABELS[code]).filter(Boolean);

  return <PaperCard style={styles.card}>
    <AppText variant="heading">Gợi ý nhìn lại</AppText>
    {status === 'ANALYZED' && analysis?.reflection ? <>
      <AppText>{analysis.reflection}</AppText>
      {emotions.length ? <Chips label="Cảm xúc" items={emotions} /> : null}
      {topics.length ? <Chips label="Chủ đề" items={topics} /> : null}
      <AppText variant="caption">Đây là gợi ý để tự suy ngẫm, không phải chẩn đoán.</AppText>
    </> : isWaiting(status) ? (timedOut
      ? <AppText accessibilityRole="alert">Phân tích đang mất nhiều thời gian hơn bình thường. Hãy quay lại sau ít phút.</AppText>
      : <AppText accessibilityLiveRegion="polite">{status === 'ANALYSIS_OUTDATED'
          ? 'Bạn vừa sửa bài, gợi ý đang được cập nhật…' : 'Đang đọc trang viết của bạn…'}</AppText>)
    : status === 'BLOCKED_BY_SAFETY' ? <>
      <AppText>Trang này không có gợi ý tự động. Nếu bạn đang thấy khó khăn, bạn có thể xem các cách tìm hỗ trợ.</AppText>
      {showSafety ? <AppButton title="Xem hỗ trợ an toàn" variant="secondary" onPress={onSafety} /> : null}
    </> : status === 'ANALYSIS_FAILED'
      ? <AppText>Chưa tạo được gợi ý cho trang này.</AppText>
      : <AppText>Trang này chưa được phân tích.</AppText>}
    {canRetry(status) ? <AppButton title="Phân tích lại" variant="secondary" loading={busy} onPress={() => void retry()} /> : null}
    {error || retryError ? <AppText accessibilityRole="alert" style={styles.error}>{retryError ?? error}</AppText> : null}
  </PaperCard>;
}

function Chips({ label, items }: { label: string; items: string[] }) {
  return <View style={styles.chipGroup} accessibilityLabel={`${label}: ${items.join(', ')}`}>
    <AppText variant="caption">{label}</AppText>
    <View style={styles.chips}>{items.map((item) => <View key={item} style={styles.chip}><AppText>{item}</AppText></View>)}</View>
  </View>;
}

const styles = StyleSheet.create({
  card: { gap: Spacing.two, marginBottom: Spacing.four },
  chipGroup: { gap: Spacing.one },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.two },
  chip: { borderWidth: 2, borderColor: Palette.ink, borderRadius: 999, paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one, backgroundColor: Palette.surface },
  error: { color: Palette.danger },
});
