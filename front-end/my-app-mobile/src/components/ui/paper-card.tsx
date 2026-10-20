import { StyleSheet, View, type ViewProps } from 'react-native';
import { Palette, Radius, Spacing } from '@/constants/theme';

export function PaperCard({ style, ...props }: ViewProps) {
  return <View {...props} style={[styles.card, style]} />;
}

const styles = StyleSheet.create({
  card: { backgroundColor: Palette.paper, borderWidth: 2, borderColor: Palette.ink,
    borderRadius: Radius.card, padding: Spacing.three, gap: Spacing.two,
    shadowColor: Palette.ink, shadowOffset: { width: 4, height: 4 }, shadowOpacity: 1, shadowRadius: 0, elevation: 4 },
});
