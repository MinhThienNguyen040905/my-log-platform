import { Pressable, StyleSheet } from 'react-native';
import { AppText, PaperCard } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import type { JournalSummary } from './api';

export function JournalCard({ entry, onPress }: { entry: JournalSummary; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityLabel={`Mở nhật ký ${entry.title || entry.localDate}`} onPress={onPress}>
    <PaperCard style={styles.card}>
      <AppText variant="eyebrow">{entry.localDate}{entry.favorite ? ' · ĐÃ LƯU' : ''}</AppText>
      <AppText variant="heading">{entry.title || 'Trang không tiêu đề'}</AppText>
      {entry.moodCode ? <AppText variant="caption">Tâm trạng: {entry.moodCode}</AppText> : null}
    </PaperCard>
  </Pressable>;
}
const styles = StyleSheet.create({ card: { gap: Spacing.two } });
