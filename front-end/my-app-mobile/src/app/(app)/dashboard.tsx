import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { getDashboard, type Dashboard } from '@/features/overview/api';
import { Spacing } from '@/constants/theme';

export default function DashboardScreen() {
  const session = useProtectedProfile();
  const userId = session.profile?.userId;
  const [data, setData] = useState<Dashboard | null>(null);
  const [range, setRange] = useState<'7d' | '30d' | '90d'>('30d');
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => { setError(null);
    try { setData(await getDashboard(range)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải tổng quan.'); }
  }, [range]);
  useFocusEffect(useCallback(() => { if (userId) void load(); }, [userId, load]));
  return <Screen><View style={styles.stack}>
    <AppButton title="Về trang chủ" variant="secondary" onPress={() => router.replace('/home')} />
    <AppText variant="eyebrow">TỔNG QUAN</AppText><AppText variant="display">Những ngày đã qua.</AppText>
    <View style={styles.row}>{(['7d', '30d', '90d'] as const).map((value) =>
      <AppButton key={value} title={value} variant={range === value ? 'primary' : 'secondary'} onPress={() => setRange(value)} />)}</View>
    {error ? <StatusView title="Không thể tải tổng quan" message={error} onRetry={() => void load()} /> : null}
    {!error && !data ? <AppText>Đang tải...</AppText> : null}
    {data ? <>
      <PaperCard><AppText variant="heading">{data.journalEntryCount} trang nhật ký</AppText>
        <AppText>{data.from} – {data.to}</AppText><AppText>Chuỗi ngày viết hiện tại: {data.currentJournalStreak}</AppText></PaperCard>
      {data.moodSampleSize > 0 ? <PaperCard><AppText variant="heading">Cảm xúc được ghi nhận</AppText>
        <AppText>{data.moodSampleSize} mẫu có dữ liệu tâm trạng</AppText>
        {data.topEmotions.map((item) => <AppText key={item.code}>{item.code}: {item.count}</AppText>)}</PaperCard>
        : <StatusView title="Chưa đủ dữ liệu tâm trạng" message="Khi bạn ghi lại tâm trạng, thông tin sẽ xuất hiện ở đây." />}
      <PaperCard><AppText variant="heading">Theo ngày</AppText>
        {data.timeline.filter((day) => day.journalCount > 0).map((day) =>
          <AppText key={day.date}>{day.date}: {day.journalCount} trang</AppText>)}</PaperCard>
    </> : null}
  </View></Screen>;
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four }, row: { flexDirection: 'row', gap: Spacing.two } });
