import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { listInsights, listReports, type Insight, type Report } from '@/features/overview/api';
import { Spacing } from '@/constants/theme';

export default function InsightsScreen() {
  const session = useProtectedProfile();
  const userId = session.profile?.userId;
  const [insights, setInsights] = useState<Insight[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [insightCursor, setInsightCursor] = useState<string | null>(null);
  const [reportCursor, setReportCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async (kind?: 'insights' | 'reports', cursor?: string) => { setLoading(true); setError(null);
    try {
      if (!kind || kind === 'insights') { const page = await listInsights(kind ? cursor : undefined);
        setInsights((current) => kind ? [...current, ...page.items.filter((item) => !current.some((old) => old.id === item.id))] : page.items);
        setInsightCursor(page.nextCursor); }
      if (!kind || kind === 'reports') { const page = await listReports(kind ? cursor : undefined);
        setReports((current) => kind ? [...current, ...page.items.filter((item) => !current.some((old) => old.id === item.id))] : page.items);
        setReportCursor(page.nextCursor); }
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải phân tích.'); }
    finally { setLoading(false); }
  }, []);
  useFocusEffect(useCallback(() => { if (userId) void load(); }, [userId, load]));
  return <Screen><View style={styles.stack}>
    <AppButton title="Về trang chủ" variant="secondary" onPress={() => router.replace('/home')} />
    <AppText variant="eyebrow">GÓC NHÌN LẠI</AppText><AppText variant="display">Điều dữ liệu kể lại.</AppText>
    <AppText variant="caption">Chỉ hiển thị phân tích và báo cáo do máy chủ trả về.</AppText>
    {error ? <StatusView title="Không thể tải phân tích" message={error} onRetry={() => void load()} /> : null}
    {loading && !insights.length && !reports.length ? <AppText>Đang tải...</AppText> : null}
    <AppText variant="heading">Góc nhìn</AppText>
    {!loading && !insights.length ? <StatusView title="Chưa có góc nhìn" message="Thông tin sẽ xuất hiện khi có dữ liệu phù hợp." /> : null}
    {insights.map((item) => <PaperCard key={item.id} style={styles.card}>
      <AppText variant="eyebrow">{item.from} – {item.to}</AppText><AppText variant="heading">{item.type}</AppText>
      <AppText>{item.narrative || `Xu hướng: ${item.direction}`}</AppText><AppText variant="caption">Cỡ mẫu: {item.sampleSize}</AppText>
    </PaperCard>)}
    {insightCursor ? <AppButton title="Xem thêm góc nhìn" variant="secondary" onPress={() => void load('insights', insightCursor)} /> : null}
    <AppText variant="heading">Báo cáo</AppText>
    {!loading && !reports.length ? <StatusView title="Chưa có báo cáo" /> : null}
    {reports.map((item) => <PaperCard key={item.id} style={styles.card}>
      <AppText variant="eyebrow">{item.from} – {item.to}</AppText><AppText variant="heading">{item.type}</AppText>
      <AppText>Trạng thái: {item.status}</AppText>{item.narrative ? <AppText>{item.narrative}</AppText> : null}
      <AppText variant="caption">Cỡ mẫu: {item.sampleSize}</AppText>
      <AppButton title={`Mở báo cáo ${item.type}`} variant="secondary"
        onPress={() => router.push({ pathname: '/report/[id]', params: { id: item.id } })} />
    </PaperCard>)}
    {reportCursor ? <AppButton title="Xem thêm báo cáo" variant="secondary" onPress={() => void load('reports', reportCursor)} /> : null}
  </View></Screen>;
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four }, card: { gap: Spacing.two } });
