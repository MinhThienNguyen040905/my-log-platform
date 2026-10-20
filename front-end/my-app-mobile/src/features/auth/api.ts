import { apiRequest, jsonBody, saveSession, clearSession, type AuthTokens } from '@/lib/api/client';

export async function registerAccount(input: { email: string; password: string; timezone: string }) {
  await apiRequest('/auth/register', { method: 'POST', ...jsonBody({ ...input, locale: 'vi',
    termsVersion: 'v1', privacyVersion: 'v1', acceptTerms: true, acceptPrivacy: true }) }, false);
}
export async function verifyCode(email: string, code: string) {
  await apiRequest('/auth/email-verifications:confirm-code', { method: 'POST', ...jsonBody({ email, code }) }, false);
}
export async function resendCode(email: string) {
  await apiRequest('/auth/email-verifications', { method: 'POST', ...jsonBody({ email }) }, false);
}
export async function login(email: string, password: string) {
  const response = await apiRequest('/auth/login', { method: 'POST',
    ...jsonBody({ email, password, deviceName: 'MyLog Mobile' }) }, false);
  await saveSession(await response.json() as AuthTokens);
}
export async function logout() {
  try { await apiRequest('/auth/logout', { method: 'POST' }); }
  finally { await clearSession(); }
}
