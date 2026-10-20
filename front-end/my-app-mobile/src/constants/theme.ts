import { Platform } from 'react-native';

export const Palette = {
  canvas: '#F5F5F3', surface: '#FFFFFF', ink: '#111111', accent: '#B7FF32',
  paper: '#F3EBDD', muted: '#D9D7D2', secondary: '#4D4D49', danger: '#B42318',
} as const;

export const Colors = {
  light: {
    text: Palette.ink, background: Palette.canvas, backgroundElement: Palette.surface,
    backgroundSelected: Palette.paper, textSecondary: Palette.secondary,
  },
  dark: {
    text: '#F5F5F3', background: '#1B1B1A', backgroundElement: '#292927',
    backgroundSelected: '#393931', textSecondary: '#D1D1C9',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Fonts = Platform.select({
  ios: { sans: 'system-ui', serif: 'ui-serif', rounded: 'ui-rounded', mono: 'ui-monospace' },
  default: { sans: 'normal', serif: 'serif', rounded: 'normal', mono: 'monospace' },
  web: { sans: 'system-ui', serif: 'Georgia', rounded: 'system-ui', mono: 'monospace' },
});

export const Spacing = { half: 2, one: 4, two: 8, three: 16, four: 24, five: 32, six: 64 } as const;
export const Radius = { control: 14, card: 18, paper: 8 } as const;
export const Border = { width: 2, color: Palette.ink } as const;
export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;
