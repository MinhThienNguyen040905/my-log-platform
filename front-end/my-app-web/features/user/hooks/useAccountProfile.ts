'use client';

import { useCallback, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';
import type { UserProfile } from '@/types';
import { getProfile, updateProfile as saveProfile, completeOnboarding as finishOnboarding, type Profile } from '../api/client';

export function useAccountProfile(profile: Profile) {
  const queryClient = useQueryClient();
  const { setLocale } = useAppLanguage();
  const profileVersion = useRef(profile.version);
  const [accountProfile, setAccountProfile] = useState(profile);
  const [userProfile, setUserProfile] = useState<UserProfile>({
    name: profile.displayName || profile.penName || 'Bạn',
    penName: profile.penName || profile.displayName || 'Bạn',
    email: '',
    plan: 'FREE',
    timezone: profile.timezone,
    avatarUrl: '/avatar.png',
    language: profile.locale === 'en' ? 'en' : 'vi',
    isOnboarded: profile.onboardingCompletedAt !== null,
    journalingGoals: profile.onboardingGoals,
  });

  const updateProfile = async (data: Partial<UserProfile>) => {
    const saved = await saveProfile({
      ...(data.name !== undefined ? { displayName: data.name } : {}),
      ...(data.penName !== undefined ? { penName: data.penName } : {}),
      ...(data.timezone !== undefined ? { timezone: data.timezone } : {}),
      ...(data.language !== undefined ? { locale: data.language } : {}),
      ...(data.journalingGoals !== undefined ? { onboardingGoals: data.journalingGoals.map((goal) => goal.toUpperCase()) } : {}),
    }, profileVersion.current);
    profileVersion.current = saved.version;
    setAccountProfile(saved);
    queryClient.setQueryData(['auth', 'session'], saved);
    if (saved.locale === 'vi' || saved.locale === 'en') await setLocale(saved.locale);
    setUserProfile((previous) => ({
      ...previous,
      name: saved.displayName || 'Bạn',
      penName: saved.penName || 'Bạn',
      timezone: saved.timezone,
      language: saved.locale === 'en' ? 'en' : 'vi',
      journalingGoals: saved.onboardingGoals,
    }));
    void queryClient.invalidateQueries({ queryKey: ['dashboard', profile.userId] });
    void queryClient.invalidateQueries({ queryKey: ['reports', profile.userId] });
    void queryClient.invalidateQueries({ queryKey: ['insights', profile.userId] });
    return saved;
  };

  const reloadProfile = useCallback(async () => {
    const fresh = await getProfile();
    profileVersion.current = fresh.version;
    setAccountProfile(fresh);
    queryClient.setQueryData(['auth', 'session'], fresh);
    if (fresh.locale === 'vi' || fresh.locale === 'en') await setLocale(fresh.locale);
    setUserProfile((previous) => ({
      ...previous,
      name: fresh.displayName || fresh.penName || 'Bạn',
      penName: fresh.penName || fresh.displayName || 'Bạn',
      timezone: fresh.timezone,
      language: fresh.locale === 'en' ? 'en' : 'vi',
      journalingGoals: fresh.onboardingGoals,
      isOnboarded: fresh.onboardingCompletedAt !== null,
    }));
    return fresh;
  }, [queryClient, setLocale]);

  const completeOnboarding = async () => {
    const saved = await finishOnboarding(profileVersion.current);
    profileVersion.current = saved.version;
    setAccountProfile(saved);
    setUserProfile((previous) => ({ ...previous, isOnboarded: saved.onboardingCompletedAt !== null }));
    return saved;
  };

  return { userProfile, accountProfile, reloadProfile, updateProfile, completeOnboarding };
}
