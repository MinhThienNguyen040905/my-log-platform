import { StyleSheet, Text, type TextProps } from 'react-native';
import { Palette } from '@/constants/theme';

type Variant = 'display' | 'heading' | 'body' | 'eyebrow' | 'caption';
export function AppText({ variant = 'body', style, ...props }: TextProps & { variant?: Variant }) {
  return <Text {...props} style={[styles.base, styles[variant], style]} />;
}

const styles = StyleSheet.create({
  base: { color: Palette.ink, includeFontPadding: false },
  display: { fontSize: 42, lineHeight: 46, fontWeight: '900', letterSpacing: -1.8 },
  heading: { fontSize: 27, lineHeight: 34, fontWeight: '800', letterSpacing: -0.5 },
  body: { fontSize: 17, lineHeight: 26, fontWeight: '400' },
  eyebrow: { fontSize: 12, lineHeight: 17, fontWeight: '800', letterSpacing: 1.1 },
  caption: { fontSize: 13, lineHeight: 19, color: Palette.secondary },
});
