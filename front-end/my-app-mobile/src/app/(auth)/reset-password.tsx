import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, TextField } from '@/components/ui';
import { clearResetEmail, passwordProblem, readResetEmail, resetErrorMessage, resetPasswordWithCode } from '@/features/auth/password-reset';
import { Palette, Spacing } from '@/constants/theme';

export default function ResetPasswordScreen() {
  const [email, setEmail] = useState(readResetEmail());
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!email.includes('@') || !/^[0-9]{8}$/.test(code)) { setError('Nhập email và mã 8 chữ số.'); return; }
    const problem = passwordProblem(password, confirm);
    if (problem) { setError(problem); return; }
    setBusy(true); setError(null);
    try {
      await resetPasswordWithCode(email.trim(), code, password);
      clearResetEmail();
      router.replace({ pathname: '/login', params: { notice: 'password-reset' } });
    } catch (cause) { setError(resetErrorMessage(cause)); }
    finally { setBusy(false); }
  };

  return <Screen>
    <View style={styles.header}>
      <AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">MYLOG · ĐẶT LẠI MẬT KHẨU</AppText>
      <AppText variant="display">Mật khẩu mới.</AppText>
      <AppText>Nhập email, mã 8 chữ số trong thư và mật khẩu mới. Sau khi đổi, mọi thiết bị sẽ được đăng xuất.</AppText>
    </View>
    <PaperCard style={styles.form}>
      <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address"
        autoCapitalize="none" autoComplete="email" />
      {/* Không đặt maxLength trên ô: mã dán vào có thể kèm khoảng trắng/gạch ("1234 5678"), cắt trước khi lọc sẽ mất chữ số. */}
      <TextField label="Mã 8 chữ số" value={code} onChangeText={(value) => setCode(value.replace(/\D/g, '').slice(0, 8))}
        keyboardType="number-pad" autoComplete="one-time-code" textContentType="oneTimeCode" />
      <TextField label="Mật khẩu mới" value={password} onChangeText={setPassword}
        secureTextEntry autoComplete="new-password" textContentType="newPassword" placeholder="Tối thiểu 12 ký tự" />
      <TextField label="Nhập lại mật khẩu mới" value={confirm} onChangeText={setConfirm}
        secureTextEntry autoComplete="new-password" textContentType="newPassword" />
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Đặt mật khẩu mới" loading={busy} onPress={() => void submit()} />
      <AppButton title="Gửi yêu cầu mới" variant="secondary" disabled={busy} onPress={() => router.replace('/forgot-password')} />
    </PaperCard>
  </Screen>;
}

const styles = StyleSheet.create({
  header: { gap: Spacing.three, paddingVertical: Spacing.four },
  form: { gap: Spacing.three, marginBottom: Spacing.four },
  error: { color: Palette.danger },
});
