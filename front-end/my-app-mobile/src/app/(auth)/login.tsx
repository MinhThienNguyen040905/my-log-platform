import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, TextField } from '@/components/ui';
import { useSession } from '@/providers/SessionProvider';
import { Spacing } from '@/constants/theme';

export default function LoginScreen() {
  const { signIn } = useSession();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = async () => {
    if (!email.includes('@') || !password) { setError('Nhập email và mật khẩu hợp lệ.'); return; }
    setBusy(true); setError(null);
    try {
      const profile = await signIn(email.trim(), password);
      router.replace(profile.onboardingCompletedAt ? '/home' : '/onboarding');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể đăng nhập.'); }
    finally { setBusy(false); }
  };
  return <Screen>
    <View style={styles.header}>
      <AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">MYLOG · ĐĂNG NHẬP</AppText>
      <AppText variant="display">Mở sổ của bạn.</AppText>
    </View>
    <PaperCard style={styles.form}>
      <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address"
        autoCapitalize="none" autoComplete="email" placeholder="ten@example.com" />
      <TextField label="Mật khẩu" value={password} onChangeText={setPassword}
        secureTextEntry autoComplete="password" placeholder="Nhập mật khẩu" />
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Đăng nhập" loading={busy} onPress={() => void submit()} />
      <AppButton title="Tạo tài khoản" variant="secondary" onPress={() => router.push('/register')} />
    </PaperCard>
  </Screen>;
}

const styles = StyleSheet.create({
  header: { gap: Spacing.three, paddingVertical: Spacing.four },
  form: { gap: Spacing.three, marginBottom: Spacing.four },
  error: { color: '#B42318' },
});
