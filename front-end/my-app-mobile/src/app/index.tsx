import { router, useFocusEffect } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { useCallback } from 'react';
import { AppButton, AppText, Screen } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';
import { useSession } from '@/providers/SessionProvider';

export default function WelcomeScreen() {
  const session = useSession();
  useFocusEffect(useCallback(() => {
    if (session.status === 'authenticated' && session.profile) {
      router.replace(session.profile.onboardingCompletedAt ? '/home' : '/onboarding');
    }
  }, [session.status, session.profile]));
  if (session.status === 'loading') return <Screen><AppText>Đang kiểm tra phiên...</AppText></Screen>;
  if (session.status === 'error') return <Screen><AppText>{session.error}</AppText>
    <AppButton title="Thử lại" onPress={() => void session.reload()} /></Screen>;
  return <Screen>
    <View style={styles.brand}><AppText variant="eyebrow">MYLOG · NHẬT KÝ CỦA BẠN</AppText></View>
    <View style={styles.hero}>
      <View style={styles.paper}><AppText variant="display">Viết điều bạn muốn nhớ.</AppText></View>
      <AppText variant="body" style={styles.intro}>Một khoảng riêng để ghi lại ngày hôm nay, theo nhịp của bạn.</AppText>
    </View>
    <View style={styles.actions}>
      <AppButton title="Đăng nhập" onPress={() => router.push('/login')} />
      <AppButton title="Xem giao diện mẫu" variant="secondary" onPress={() => router.push('/journal-preview')} />
      <AppText variant="caption">Giao diện nhật ký mẫu dùng nội dung minh họa; dữ liệu tài khoản được lấy từ backend sau khi đăng nhập.</AppText>
    </View>
  </Screen>;
}

const styles = StyleSheet.create({
  brand: { paddingTop: Spacing.four },
  hero: { flex: 1, justifyContent: 'center', gap: Spacing.four },
  paper: { backgroundColor: Palette.paper, borderWidth: 2, borderColor: Palette.ink, borderRadius: 8, padding: Spacing.four, transform: [{ rotate: '-2deg' }] },
  intro: { maxWidth: 300 },
  actions: { gap: Spacing.three, paddingBottom: Spacing.four },
});
