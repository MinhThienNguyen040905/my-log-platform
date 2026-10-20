import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, TextField } from '@/components/ui';
import { registerAccount } from '@/features/auth/api';
import { rememberPendingEmail } from '@/features/auth/pending-verification';
import { Palette, Spacing } from '@/constants/theme';

export default function RegisterScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [accepted, setAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = async () => {
    if (!email.includes('@') || password.length < 12 || password.length > 128 || password !== confirm || !accepted) {
      setError('Kiểm tra email, mật khẩu 12–128 ký tự, xác nhận mật khẩu và điều khoản.'); return;
    }
    setBusy(true); setError(null);
    try {
      await registerAccount({ email: email.trim(), password,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh' });
      rememberPendingEmail(email);
      router.replace('/verify-code');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể đăng ký.'); }
    finally { setBusy(false); }
  };
  return <Screen>
    <View style={styles.header}>
      <AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">MYLOG · ĐĂNG KÝ</AppText>
      <AppText variant="display">Bắt đầu trang mới.</AppText>
    </View>
    <PaperCard style={styles.form}>
      <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoComplete="email" />
      <TextField label="Mật khẩu" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" />
      <TextField label="Nhập lại mật khẩu" value={confirm} onChangeText={setConfirm} secureTextEntry />
      <Pressable accessibilityRole="checkbox" accessibilityState={{ checked: accepted }} onPress={() => setAccepted(!accepted)} style={styles.consent}>
        <View style={[styles.box, accepted && styles.checked]} />
        <AppText>Tôi đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư phiên bản v1.</AppText>
      </Pressable>
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Tạo tài khoản" loading={busy} onPress={() => void submit()} />
    </PaperCard>
  </Screen>;
}

const styles = StyleSheet.create({
  header: { gap: Spacing.three, paddingVertical: Spacing.four }, form: { gap: Spacing.three, marginBottom: Spacing.four },
  consent: { flexDirection: 'row', alignItems: 'center', gap: Spacing.two, minHeight: 48 },
  box: { width: 24, height: 24, borderWidth: 2, borderColor: Palette.ink, backgroundColor: Palette.surface },
  checked: { backgroundColor: Palette.accent }, error: { color: Palette.danger },
});
