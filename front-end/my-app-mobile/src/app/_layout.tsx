import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Palette } from '@/constants/theme';
import { SessionProvider } from '@/providers/SessionProvider';

export default function RootLayout() {
  return <SessionProvider>
    <StatusBar style="dark" />
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: Palette.canvas } }} />
  </SessionProvider>;
}
