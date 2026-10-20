import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { completeOnboarding, getConsents, revokeConsent, updateProfile, type Consent } from '@/features/user/api';
import { Palette, Spacing } from '@/constants/theme';

const goalOptions = [
  { id: 'REFLECTION', label: 'Nhìn lại bản thân' }, { id: 'SLEEP', label: 'Chăm sóc giấc ngủ' },
  { id: 'MINDFULNESS', label: 'Quan sát cảm xúc' }, { id: 'EXERCISE', label: 'Duy trì vận động' },
  { id: 'SOCIAL', label: 'Kết nối với mọi người' },
];
const optionalTypes = ['AI_PROCESSING', 'ANALYTICS', 'MODEL_TRAINING'];

export default function OnboardingScreen() {
  const session = useProtectedProfile(false);
  const profile = session.profile;
  const userId = profile?.userId;
  const profileDisplayName = profile?.displayName;
  const profilePenName = profile?.penName;
  const profileGoals = profile?.onboardingGoals;
  const [displayName, setDisplayName] = useState(profile?.displayName ?? '');
  const [penName, setPenName] = useState(profile?.penName ?? '');
  const [goals, setGoals] = useState<string[]>(profile?.onboardingGoals ?? []);
  const [consents, setConsents] = useState<Consent[] | null>(null);
  const [revocations, setRevocations] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    const timer = setTimeout(() => {
      setDisplayName(profileDisplayName ?? ''); setPenName(profilePenName ?? ''); setGoals(profileGoals ?? []);
    }, 0);
    return () => clearTimeout(timer);
  }, [userId, profileDisplayName, profilePenName, profileGoals]);
  useEffect(() => {
    if (!userId) return;
    let active = true;
    void getConsents().then((data) => { if (active) setConsents(data); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Không thể tải quyền đồng ý.'); });
    return () => { active = false; };
  }, [userId]);
  const toggle = (id: string) => setGoals((current) => current.includes(id) ? current.filter((value) => value !== id) : [...current, id]);
  const submit = async () => {
    if (!profile || !consents) return;
    if (displayName.length > 120 || penName.length > 120) { setError('Tên tối đa 120 ký tự.'); return; }
    setBusy(true); setError(null);
    try {
      let current = profile;
      if (displayName.trim() !== (profile.displayName ?? '') || penName.trim() !== (profile.penName ?? '')
        || goals.join('|') !== profile.onboardingGoals.join('|')) {
        current = await updateProfile(profile, { displayName: displayName.trim(), penName: penName.trim(), onboardingGoals: goals });
        session.setProfile(current);
      }
      for (const type of revocations) {
        const consent = consents.find((item) => item.type === type && item.granted);
        if (consent) await revokeConsent(consent);
      }
      const completed = await completeOnboarding(current);
      session.setProfile(completed);
      router.replace('/home');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể hoàn tất.'); }
    finally { setBusy(false); }
  };
  if (!profile) return <Screen><StatusView title="Đang tải hồ sơ" message={session.error ?? undefined} onRetry={() => void session.reload()} /></Screen>;
  return <Screen>
    <View style={styles.header}><AppText variant="eyebrow">BƯỚC ĐẦU VỚI MYLOG</AppText>
      <AppText variant="display">Đây là sổ của bạn.</AppText>
      <AppText>Chọn cách bạn muốn dùng nhật ký. Bạn có thể chỉnh lại sau.</AppText></View>
    <PaperCard style={styles.form}>
      <TextField label="Tên hiển thị" value={displayName} onChangeText={setDisplayName} maxLength={120} />
      <TextField label="Bút danh" value={penName} onChangeText={setPenName} maxLength={120} />
      <AppText variant="heading">Điều bạn quan tâm</AppText>
      {goalOptions.map((goal) => <Pressable key={goal.id} accessibilityRole="checkbox"
        accessibilityState={{ checked: goals.includes(goal.id) }} onPress={() => toggle(goal.id)} style={styles.row}>
        <AppText>{goals.includes(goal.id) ? '✓ ' : '○ '}{goal.label}</AppText>
      </Pressable>)}
      <AppText variant="heading">Quyền tùy chọn</AppText>
      {!consents ? <AppText>Đang tải trạng thái quyền đồng ý...</AppText> : optionalTypes.map((type) => {
        const item = consents.find((value) => value.type === type);
        return <Pressable key={type} disabled={!item?.granted} onPress={() => setRevocations((current) => current.includes(type)
          ? current.filter((value) => value !== type) : [...current, type])} style={styles.row}>
          <AppText>{type}: {item?.granted ? (revocations.includes(type) ? 'Sẽ thu hồi' : 'Đã đồng ý · chạm để thu hồi') : 'Chưa cấp; chưa thể bật tại đây'}</AppText>
        </Pressable>;
      })}
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Hoàn tất" disabled={!consents} loading={busy} onPress={() => void submit()} />
    </PaperCard>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  form: { gap: Spacing.three, marginBottom: Spacing.four }, row: { minHeight: 44, justifyContent: 'center' },
  error: { color: Palette.danger } });
