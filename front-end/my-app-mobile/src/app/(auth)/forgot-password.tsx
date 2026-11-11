import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, TextField } from '@/components/ui';
import { readResetEmail, rememberResetEmail, requestPasswordReset, resetErrorMessage } from '@/features/auth/password-reset';
import { Palette, Spacing } from '@/constants/theme';

export default function ForgotPasswordScreen() {
  const [email, setEmail] = useState(readResetEmail());
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!email.includes('@')) { setError('Nhập email hợp lệ.'); return; }
    setBusy(true); setError(null);
    try {
      await requestPasswordReset(email.trim());
      rememberResetEmail(email);
      setSent(true);
    } catch (cause) { setError(resetErrorMessage(cause)); }
    finally { setBusy(false); }
  };

  return <Screen>
    <View style={styles.header}>
      <AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">MYLOG · QUÊN MẬT KHẨU</AppText>
      <AppText variant="display">Đặt lại mật khẩu.</AppText>
      <AppText>Nhập email bạn dùng để đăng nhập. Nếu email có trong hệ thống, MyLog sẽ gửi mã 8 chữ số.</AppText>
    </View>
    <PaperCard style={styles.form}>
      {sent ? <>
        <AppText accessibilityRole="alert">
          Nếu email này có trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi. Mã có hiệu lực 15 phút.
        </AppText>
        <AppButton title="Nhập mã 8 chữ số" onPress={() => router.push('/reset-password')} />
        <AppButton title="Gửi lại yêu cầu" variant="secondary" onPress={() => setSent(false)} />
      </> : <>
        <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address"
          autoCapitalize="none" autoComplete="email" placeholder="ten@example.com" />
        {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
        <AppButton title="Gửi hướng dẫn" loading={busy} onPress={() => void submit()} />
        <AppButton title="Tôi đã có mã" variant="secondary" disabled={busy}
          onPress={() => { if (email.includes('@')) rememberResetEmail(email); router.push('/reset-password'); }} />
      </>}
    </PaperCard>
  </Screen>;
}

const styles = StyleSheet.create({
  header: { gap: Spacing.three, paddingVertical: Spacing.four },
  form: { gap: Spacing.three, marginBottom: Spacing.four },
  error: { color: Palette.danger },
});
