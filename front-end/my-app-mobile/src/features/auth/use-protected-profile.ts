import { useEffect } from 'react';
import { router } from 'expo-router';
import { useSession } from '@/providers/SessionProvider';

export function useProtectedProfile(requireOnboarding = true) {
  const session = useSession();
  useEffect(() => {
    if (session.status === 'guest') router.replace('/login');
    else if (session.status === 'authenticated' && session.profile) {
      if (requireOnboarding && !session.profile.onboardingCompletedAt) router.replace('/onboarding');
      if (!requireOnboarding && session.profile.onboardingCompletedAt) router.replace('/home');
    }
  }, [session.status, session.profile, requireOnboarding]);
  return session;
}
