import { View, StyleSheet } from 'react-native';
import { AppButton } from './app-button';
import { AppText } from './app-text';
import { Spacing } from '@/constants/theme';

export function StatusView({ title, message, onRetry }: { title: string; message?: string; onRetry?: () => void }) {
  return <View style={styles.container} accessibilityRole="summary">
    <AppText variant="heading">{title}</AppText>
    {message ? <AppText>{message}</AppText> : null}
    {onRetry ? <AppButton title="Thử lại" variant="secondary" onPress={onRetry} /> : null}
  </View>;
}

const styles = StyleSheet.create({ container: { paddingVertical: Spacing.five, gap: Spacing.three } });
