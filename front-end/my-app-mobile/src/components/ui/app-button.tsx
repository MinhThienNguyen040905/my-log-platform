import { Pressable, StyleSheet, type PressableProps } from 'react-native';
import { AppText } from './app-text';
import { Palette, Radius, Spacing } from '@/constants/theme';

type Props = PressableProps & { title: string; variant?: 'primary' | 'secondary'; loading?: boolean };
export function AppButton({ title, variant = 'primary', loading = false, disabled, style, ...props }: Props) {
  const inactive = disabled || loading;
  return <Pressable
    {...props}
    accessibilityRole="button"
    accessibilityLabel={title}
    accessibilityState={{ disabled: inactive, busy: loading }}
    disabled={inactive}
    style={(state) => [styles.base, variant === 'primary' ? styles.primary : styles.secondary,
      state.pressed && !inactive && styles.pressed, inactive && styles.inactive,
      typeof style === 'function' ? style(state) : style]}
  >
    <AppText variant="eyebrow" style={styles.label}>{loading ? 'ĐANG XỬ LÝ…' : title}</AppText>
  </Pressable>;
}

const styles = StyleSheet.create({
  base: { minHeight: 52, paddingHorizontal: Spacing.three, paddingVertical: Spacing.two,
    alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: Palette.ink,
    borderRadius: Radius.control, marginRight: 4, marginBottom: 4 },
  primary: { backgroundColor: Palette.accent, shadowColor: Palette.ink, shadowOffset: { width: 4, height: 4 }, shadowOpacity: 1, shadowRadius: 0, elevation: 4 },
  secondary: { backgroundColor: Palette.surface },
  pressed: { transform: [{ translateX: 2 }, { translateY: 2 }], elevation: 0, shadowOpacity: 0 },
  inactive: { opacity: 0.5 },
  label: { textAlign: 'center', letterSpacing: 0.5 },
});
