import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { JournalCard } from '@/features/journal/JournalCard';
import { listEntries, type JournalSummary } from '@/features/journal/api';
import { Spacing } from '@/constants/theme';

export default function HistoryScreen() {
  useProtectedProfile();
  const [date, setDate] = useState('');
  const [items, setItems] = useState<JournalSummary[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const load = async (next?: string) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) { setError('Nhập ngày theo dạng YYYY-MM-DD.'); return; }
    setLoading(true); setError(null);
    try { const page = await listEntries(next, { from: date, to: date });
      setItems((previous) => next ? [...previous, ...page.items.filter((item) => !previous.some((old) => old.id === item.id))] : page.items);
      setCursor(page.nextCursor);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải lịch sử.'); }
    finally { setLoading(false); }
  };
  return <Screen><View style={styles.stack}>
    <AppButton title="Về trang chủ" variant="secondary" onPress={() => router.replace('/home')} />
    <AppText variant="eyebrow">LỊCH SỬ</AppText><AppText variant="display">Một ngày trong sổ.</AppText>
    <TextField label="Ngày (YYYY-MM-DD)" value={date} onChangeText={setDate} placeholder="2026-10-13" />
    <AppButton title="Xem ngày này" onPress={() => void load()} loading={loading} />
    {error ? <StatusView title="Chưa thể xem ngày" message={error} onRetry={() => void load()} /> : null}
    {!error && !loading && date && !items.length ? <StatusView title="Không có trang trong ngày này" /> : null}
    {items.map((entry) => <JournalCard key={entry.id} entry={entry}
      onPress={() => router.push({ pathname: '/journal-entry/[id]', params: { id: entry.id } })} />)}
    {cursor ? <AppButton title="Xem thêm" variant="secondary" onPress={() => void load(cursor)} /> : null}
  </View></Screen>;
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four } });
