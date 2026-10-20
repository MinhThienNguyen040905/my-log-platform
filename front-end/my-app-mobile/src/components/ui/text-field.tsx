import { StyleSheet, TextInput, View, type TextInputProps } from 'react-native';
import { AppText } from './app-text';
import { Palette, Radius, Spacing } from '@/constants/theme';

type Props = TextInputProps & { label: string; error?: string };
export function TextField({ label, error, style, ...props }: Props) {
  return <View style={styles.group}>
    <AppText variant="eyebrow">{label}</AppText>
    <TextInput {...props} accessibilityLabel={label} placeholderTextColor={Palette.secondary}
      style={[styles.input, error ? styles.invalid : undefined, style]} />
    {error ? <AppText variant="caption" accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
  </View>;
}

const styles = StyleSheet.create({
  group: { gap: Spacing.two },
  input: { minHeight: 52, backgroundColor: Palette.surface, borderWidth: 2, borderColor: Palette.ink,
    borderRadius: Radius.control, paddingHorizontal: Spacing.three, paddingVertical: Spacing.two,
    color: Palette.ink, fontSize: 16 },
  invalid: { borderColor: Palette.danger },
  error: { color: Palette.danger },
});
