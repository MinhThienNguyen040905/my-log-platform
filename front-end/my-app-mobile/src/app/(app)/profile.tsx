import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { getConsents, revokeConsent, updateProfile, type Consent } from '@/features/user/api';
import { Palette, Spacing } from '@/constants/theme';

export default function ProfileScreen() {
  const session = useProtectedProfile();
  const profile = session.profile;
  const userId = profile?.userId;
  const profileDisplayName = profile?.displayName;
  const profilePenName = profile?.penName;
  const [displayName, setDisplayName] = useState(profile?.displayName ?? '');
  const [penName, setPenName] = useState(profile?.penName ?? '');
  const [consents, setConsents] = useState<Consent[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    const timer = setTimeout(() => { setDisplayName(profileDisplayName ?? ''); setPenName(profilePenName ?? ''); }, 0);
    return () => clearTimeout(timer);
  }, [userId, profileDisplayName, profilePenName]);
  useEffect(() => {
    if (!userId) return;
    let active = true;
    void getConsents().then((items) => { if (active) setConsents(items); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Không thể tải quyền đồng ý.'); });
    return () => { active = false; };
  }, [userId]);
  const save = async () => {
    if (!profile) return;
    setBusy(true); setError(null);
    try {
      const latest = await updateProfile(profile, { displayName: displayName.trim(), penName: penName.trim() });
      session.setProfile(latest);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể lưu hồ sơ.'); }
    finally { setBusy(false); }
  };
  const signOut = async () => {
    setBusy(true); setError(null);
    try { await session.signOut(); router.replace('/'); }
    catch { router.replace('/'); }
    finally { setBusy(false); }
  };
  const revoke = async (consent: Consent) => {
    setBusy(true); setError(null);
    try { await revokeConsent(consent); setConsents(await getConsents()); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể thu hồi quyền.'); }
    finally { setBusy(false); }
  };
  if (!profile) return <Screen><StatusView title="Đang tải hồ sơ" message={session.error ?? undefined}
    onRetry={() => void session.reload()} /></Screen>;
  return <Screen>
    <View style={styles.header}><AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">TÀI KHOẢN</AppText><AppText variant="display">Hồ sơ của bạn.</AppText></View>
    <PaperCard style={styles.card}>
      <TextField label="Tên hiển thị" value={displayName} onChangeText={setDisplayName} maxLength={120} />
      <TextField label="Bút danh" value={penName} onChangeText={setPenName} maxLength={120} />
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Lưu hồ sơ" loading={busy} onPress={() => void save()} />
    </PaperCard>
    <PaperCard style={styles.card}>
      <AppText variant="heading">Quyền đồng ý</AppText>
      {consents ? consents.map((item) => <View key={item.type} style={styles.consent}>
        <AppText>{item.type}: {item.granted ? 'Đã đồng ý' : 'Chưa cấp'}</AppText>
        {item.granted && !['TERMS', 'PRIVACY'].includes(item.type) ? <AppButton title={`Thu hồi ${item.type}`}
          variant="secondary" disabled={busy} onPress={() => void revoke(item)} /> : null}
      </View>)
        : <AppText>Đang tải...</AppText>}
      <AppText variant="caption">Quyền tùy chọn chưa cấp không thể bật trong ứng dụng khi tài liệu đồng ý chưa được công bố.</AppText>
    </PaperCard>
    <View style={styles.actions}>
      <AppButton title="Phiên và quyền dữ liệu" variant="secondary" onPress={() => router.push('/settings')} />
      <AppButton title="Đăng xuất" variant="secondary" disabled={busy} onPress={() => void signOut()} />
    </View>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  card: { gap: Spacing.three, marginBottom: Spacing.four }, consent: { gap: Spacing.two }, actions: { marginBottom: Spacing.four },
  error: { color: Palette.danger } });
