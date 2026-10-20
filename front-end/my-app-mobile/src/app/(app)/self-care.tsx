import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { completeHabit, createGoal, createHabit, listGoals, undoHabit, type Goal } from '@/features/overview/api';
import { Palette, Spacing } from '@/constants/theme';

export default function SelfCareScreen() {
  const session = useProtectedProfile();
  const today = new Date().toLocaleDateString('sv-SE', { timeZone: session.profile?.timezone || 'Asia/Ho_Chi_Minh' });
  const userId = session.profile?.userId;
  const [goals, setGoals] = useState<Goal[]>([]);
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('CUSTOM');
  const [habitNames, setHabitNames] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => { setLoading(true); setError(null);
    try { setGoals(await listGoals()); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải mục tiêu.'); }
    finally { setLoading(false); }
  }, []);
  useFocusEffect(useCallback(() => { if (userId) void load(); }, [userId, load]));
  const submit = async () => { if (!title.trim()) { setError('Nhập tên mục tiêu.'); return; }
    setBusy(true); setError(null);
    try { await createGoal(category.trim(), title.trim()); setTitle(''); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tạo mục tiêu.'); }
    finally { setBusy(false); }
  };
  return <Screen><View style={styles.stack}>
    <AppButton title="Về trang chủ" variant="secondary" onPress={() => router.replace('/home')} />
    <AppText variant="eyebrow">CHĂM SÓC BẢN THÂN</AppText><AppText variant="display">Điều bạn muốn vun đắp.</AppText>
    <PaperCard style={styles.card}><AppText variant="heading">Mục tiêu mới</AppText>
      <TextField label="Tên mục tiêu" value={title} onChangeText={setTitle} maxLength={160} />
      <AppText variant="eyebrow">NHÓM MỤC TIÊU</AppText>
      <View style={styles.row}>{(['CUSTOM', 'SLEEP', 'MINDFULNESS', 'EXERCISE', 'SOCIAL'] as const).map((value) =>
        <AppButton key={value} title={value} variant={category === value ? 'primary' : 'secondary'} onPress={() => setCategory(value)} />)}</View>
      <AppButton title="Tạo mục tiêu" loading={busy} onPress={() => void submit()} /></PaperCard>
    {error ? <StatusView title="Không thể tải mục tiêu" message={error} onRetry={() => void load()} /> : null}
    {loading ? <AppText>Đang tải...</AppText> : !goals.length ? <StatusView title="Chưa có mục tiêu" /> : null}
    {goals.map((goal) => <PaperCard key={goal.id} style={styles.card}>
      <AppText variant="eyebrow">{goal.category} · {goal.status}</AppText>
      <AppText variant="heading">{goal.title}</AppText>
      {goal.description ? <AppText>{goal.description}</AppText> : null}
      <TextField label={`Thói quen cho ${goal.title}`} value={habitNames[goal.id] ?? ''}
        onChangeText={(value) => setHabitNames((current) => ({ ...current, [goal.id]: value }))} maxLength={160} />
      <AppButton title="Thêm thói quen hằng ngày" variant="secondary" disabled={busy || !habitNames[goal.id]?.trim()}
        onPress={() => void act(async () => { await createHabit(goal.id, habitNames[goal.id].trim());
          setHabitNames((current) => ({ ...current, [goal.id]: '' })); })} />
      {goal.habits.map((habit) => <View key={habit.id} style={styles.stack}>
        <AppText>{habit.title} · chuỗi {habit.streak} ngày</AppText>
        <AppButton title={habit.completedDates.includes(today) ? 'Bỏ đánh dấu hôm nay' : 'Đánh dấu hôm nay'}
          variant="secondary" disabled={busy} onPress={() => void act(async () => {
            if (habit.completedDates.includes(today)) await undoHabit(habit.id, today);
            else await completeHabit(habit.id, today);
          })} />
      </View>)}
    </PaperCard>)}
  </View></Screen>;
  async function act(operation: () => Promise<void>) { setBusy(true); setError(null);
    try { await operation(); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể cập nhật thói quen.'); }
    finally { setBusy(false); }
  }
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four },
  card: { gap: Spacing.two }, row: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.two }, error: { color: Palette.danger } });
