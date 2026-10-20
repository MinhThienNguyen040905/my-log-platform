import { apiRequest, jsonBody } from '@/lib/api/client';

export type Profile = { userId: string; displayName: string | null; penName: string | null;
  onboardingGoals: string[]; timezone: string; locale: string; onboardingCompletedAt: string | null; version: number };
export type Consent = { type: string; documentVersion: string; granted: boolean; decidedAt: string };

export async function getProfile(): Promise<Profile> {
  return (await apiRequest('/me')).json() as Promise<Profile>;
}
export async function updateProfile(profile: Profile, changes: Partial<Pick<Profile, 'displayName' | 'penName' | 'onboardingGoals' | 'timezone' | 'locale'>>): Promise<Profile> {
  return (await apiRequest('/me', { method: 'PATCH',
    headers: { 'If-Match': `"${profile.version}"` }, ...jsonBody(changes) })).json() as Promise<Profile>;
}
export async function completeOnboarding(profile: Profile): Promise<Profile> {
  return (await apiRequest('/me/onboarding:complete', { method: 'POST', headers: { 'If-Match': `"${profile.version}"` } })).json() as Promise<Profile>;
}
export async function getConsents(): Promise<Consent[]> {
  return (await apiRequest('/me/consents')).json() as Promise<Consent[]>;
}
export async function revokeConsent(consent: Consent) {
  await apiRequest(`/me/consents/${encodeURIComponent(consent.type)}`, { method: 'PUT',
    ...jsonBody({ documentVersion: consent.documentVersion, granted: false }) });
}
