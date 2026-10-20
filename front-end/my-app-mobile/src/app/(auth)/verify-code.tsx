import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, TextField } from '@/components/ui';
import { resendCode, verifyCode } from '@/features/auth/api';
import { clearPendingEmail, readPendingEmail } from '@/features/auth/pending-verification';
import { Palette, Spacing } from '@/constants/theme';

export default function VerifyCodeScreen() {
  const [email, setEmail] = useState(readPendingEmail());
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const submit = async () => {
    if (!email.includes('@') || !/^[0-9]{8}$/.test(code)) { setError('Nhập email và mã 8 chữ số.'); return; }
    setBusy(true); setError(null); setNotice(null);
    try { await verifyCode(email.trim(), code); clearPendingEmail(); router.replace('/login'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể xác minh.'); }
    finally { setBusy(false); }
  };
  const resend = async () => {
    if (!email.includes('@')) { setError('Nhập email trước khi gửi lại mã.'); return; }
    setBusy(true); setError(null); setNotice(null);
    try { await resendCode(email.trim()); setNotice('Đã yêu cầu gửi mã mới. Kiểm tra email của bạn.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể gửi lại mã.'); }
    finally { setBusy(false); }
  };
  return <Screen>
    <View style={styles.header}><AppText variant="eyebrow">MYLOG · XÁC MINH EMAIL</AppText>
      <AppText variant="display">Kiểm tra hộp thư.</AppText>
      <AppText>Nhập mã 8 chữ số trong email. Sau khi xác minh, bạn đăng nhập riêng.</AppText></View>
    <PaperCard style={styles.form}>
      <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" />
      <TextField label="Mã xác minh" value={code} onChangeText={setCode} keyboardType="number-pad" maxLength={8} />
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      {notice ? <AppText accessibilityRole="alert">{notice}</AppText> : null}
      <AppButton title="Xác minh" loading={busy} onPress={() => void submit()} />
      <AppButton title="Gửi lại mã" variant="secondary" disabled={busy} onPress={() => void resend()} />
    </PaperCard>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  form: { gap: Spacing.three }, error: { color: Palette.danger } });
