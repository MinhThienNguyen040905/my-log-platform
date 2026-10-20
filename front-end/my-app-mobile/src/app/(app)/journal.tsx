import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, Screen, StatusView } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { JournalCard } from '@/features/journal/JournalCard';
import { listEntries, type JournalSummary } from '@/features/journal/api';
import { Palette, Spacing } from '@/constants/theme';

export default function JournalScreen() {
  const session = useProtectedProfile();
  const userId = session.profile?.userId;
  const [items, setItems] = useState<JournalSummary[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async (next?: string) => {
    setLoading(true); setError(null);
    try {
      const page = await listEntries(next);
      setItems((previous) => next ? [...previous.filter((item) => !page.items.some((value) => value.id === item.id)), ...page.items] : page.items);
      setCursor(page.nextCursor);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải nhật ký.'); }
    finally { setLoading(false); }
  }, []);
  useFocusEffect(useCallback(() => { if (userId) void load(); }, [userId, load]));
  return <Screen>
    <View style={styles.header}>
      <AppButton title="Về trang chủ" variant="secondary" onPress={() => router.replace('/home')} />
      <AppText variant="eyebrow">NHẬT KÝ CỦA BẠN</AppText>
      <AppText variant="display">Những trang đã viết.</AppText>
      <AppButton title="Viết trang mới" onPress={() => router.push('/journal-editor')} />
    </View>
    {error ? <StatusView title="Không thể tải nhật ký" message={error} onRetry={() => void load()} /> : null}
    {!error && !loading && !items.length ? <StatusView title="Chưa có trang nào" message="Bắt đầu bằng một điều bạn muốn nhớ." /> : null}
    <View style={styles.list}>{items.map((entry) => <JournalCard key={entry.id} entry={entry}
      onPress={() => router.push({ pathname: '/journal-entry/[id]', params: { id: entry.id } })} />)}</View>
    {loading ? <AppText>Đang tải...</AppText> : cursor ? <AppButton title="Xem thêm" variant="secondary" onPress={() => void load(cursor)} /> : null}
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  list: { gap: Spacing.three, paddingBottom: Spacing.four }, error: { color: Palette.danger } });
