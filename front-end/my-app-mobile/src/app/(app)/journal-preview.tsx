import { router } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView } from '@/components/ui';
import { Spacing } from '@/constants/theme';

const example = [
  { date: '12 THÁNG 10', title: 'Một buổi chiều bình yên', preview: 'Hôm nay mình dành ít phút để nhìn lại những điều nhỏ đã làm được.', mood: 'Bình yên' },
  { date: '10 THÁNG 10', title: 'Ghi lại một ý nghĩ', preview: 'Một trang nhật ký mẫu để xem cách bài viết xuất hiện trên điện thoại.', mood: 'Suy ngẫm' },
];

export default function JournalPreviewScreen() {
  return <Screen>
    <View style={styles.header}>
      <AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">BẢN XEM TRƯỚC · DỮ LIỆU MINH HỌA</AppText>
      <AppText variant="display">Những trang đã viết.</AppText>
      <AppText>Cách nhật ký sẽ hiển thị sau khi kết nối tài khoản và backend.</AppText>
    </View>
    <View style={styles.list}>
      {example.map((entry) => <PaperCard key={entry.title}>
        <AppText variant="eyebrow">{entry.date} · {entry.mood.toUpperCase()}</AppText>
        <AppText variant="heading">{entry.title}</AppText>
        <AppText>{entry.preview}</AppText>
      </PaperCard>)}
      <StatusView title="Bài viết của bạn sẽ ở đây" message="Danh sách này hiện chỉ dùng để xem thiết kế. Chưa tải nhật ký từ backend." />
    </View>
  </Screen>;
}

const styles = StyleSheet.create({
  header: { gap: Spacing.three, paddingVertical: Spacing.four },
  list: { gap: Spacing.four, paddingBottom: Spacing.four },
});
