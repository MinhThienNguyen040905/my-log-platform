'use client';

import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { usePathname, useRouter } from 'next/navigation';
import { Navbar } from '@/components/layout/Navbar';
import { JournalProvider, useJournal, clearJournalDrafts } from '@/features/journal';
import type { Profile } from '@/features/user';
import { ApiError } from '@/lib/api/client';
import { useAppLanguage } from './AppLanguageProvider';
import { isAppLocale } from '@/lib/i18n';
import { useTranslation } from 'react-i18next';

export function AuthGate({ children, showNavbar = true }: { children: React.ReactNode; showNavbar?: boolean }) {
  return <AuthGateContent showNavbar={showNavbar}>{children}</AuthGateContent>;
}

async function getSession(): Promise<Profile> {
  const response = await fetch('/api/auth/session', { cache: 'no-store' });
  if (response.status === 401) throw new ApiError(401, null, 'Phiên đăng nhập đã hết hạn.');
  if (!response.ok) throw new Error('Không thể kiểm tra phiên đăng nhập.');
  return response.json() as Promise<Profile>;
}

function AuthGateContent({ children, showNavbar }: { children: React.ReactNode; showNavbar: boolean }) {
  const { t } = useTranslation();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, setLocale } = useAppLanguage();
  const session = useQuery({ queryKey: ['auth', 'session'], queryFn: getSession, gcTime: 0 });
  const profile = session.data;

  useEffect(() => {
    if (profile && isAppLocale(profile.locale) && profile.locale !== locale) void setLocale(profile.locale);
  }, [profile, locale, setLocale]);

  useEffect(() => {
    if (session.error instanceof ApiError && session.error.status === 401) {
      clearJournalDrafts();
      queryClient.clear();
      router.replace('/auth/login');
    }
  }, [session.error, queryClient, router]);

  const logout = async () => {
    const response = await fetch('/api/auth/logout', { method: 'POST' });
    if (!response.ok) throw new Error(t('common.logoutError'));
    clearJournalDrafts();
    queryClient.clear();
    router.replace('/auth/login');
    router.refresh();
  };

  if (session.isError && !(session.error instanceof ApiError && session.error.status === 401)) return <main className="p-8 text-center">{t('common.sessionError')} <button type="button" onClick={() => void session.refetch()} className="underline">{t('common.retry')}</button></main>;
  if (!profile) return <main className="p-8 text-center" role="status">{t('common.sessionLoading')}</main>;

  return <JournalProvider key={profile.userId} userId={profile.userId} profile={profile}>
    <AuthenticatedContent onLogout={logout} showNavbar={showNavbar}>{children}</AuthenticatedContent>
  </JournalProvider>;
}

function AuthenticatedContent({ children, onLogout, showNavbar }: {
  children: React.ReactNode;
  onLogout: () => Promise<void>;
  showNavbar: boolean;
}) {
  const { t } = useTranslation();
  const { userProfile, streakCount, updateProfile, hasUnsavedJournalChanges } = useJournal();
  const pathname = usePathname();
  const router = useRouter();
  const needsOnboarding = !userProfile.isOnboarded && pathname !== '/onboarding';

  useEffect(() => {
    if (needsOnboarding) router.replace('/onboarding');
  }, [needsOnboarding, router]);

  if (needsOnboarding) return <main className="p-8 text-center" role="status">{t('common.onboardingLoading')}</main>;
  return <>
    {showNavbar && <Navbar onLogout={onLogout} penName={userProfile.penName} userProfile={userProfile} streakCount={streakCount} updateProfile={async (data) => { await updateProfile(data); }} hasUnsavedJournalChanges={hasUnsavedJournalChanges} />}
    {children}
  </>;
}
