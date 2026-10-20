import { router } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { Spacing } from '@/constants/theme';

export default function HomeScreen() {
  const session = useProtectedProfile();
  if (!session.profile) return <Screen><StatusView title="Đang tải hồ sơ" message={session.error ?? undefined}
    onRetry={() => void session.reload()} /></Screen>;
  return <Screen>
    <View style={styles.header}>
      <AppText variant="eyebrow">MYLOG · KHÔNG GIAN CỦA BẠN</AppText>
      <AppText variant="display">Chào {session.profile.penName || session.profile.displayName || 'bạn'}.</AppText>
      <AppText>Mỗi trang viết có thể bắt đầu từ một điều nhỏ.</AppText>
    </View>
    <PaperCard style={styles.card}>
      <AppText variant="heading">Nhật ký của bạn</AppText>
      <AppText>Viết trang mới hoặc xem những điều đã ghi lại.</AppText>
      <AppButton title="Mở nhật ký" onPress={() => router.push('/journal')} />
    </PaperCard>
    <View style={styles.actions}>
      <AppButton title="Tổng quan" variant="secondary" onPress={() => router.push('/dashboard')} />
      <AppButton title="Lịch sử theo ngày" variant="secondary" onPress={() => router.push('/history')} />
      <AppButton title="Góc nhìn và báo cáo" variant="secondary" onPress={() => router.push('/insights')} />
      <AppButton title="Chăm sóc bản thân" variant="secondary" onPress={() => router.push('/self-care')} />
      <AppButton title="Hồ sơ và cài đặt" onPress={() => router.push('/profile')} />
    </View>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  card: { marginBottom: Spacing.four }, actions: { marginBottom: Spacing.four } });
