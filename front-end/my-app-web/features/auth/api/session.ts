import 'server-only';
import { cookies } from 'next/headers';
import { createHash } from 'node:crypto';
import { authBackend } from './backend';

type Tokens = { userId: string; accessToken: string; refreshToken: string; expiresInSeconds: number };
const ACCESS = 'mylog_access';
const REFRESH = 'mylog_refresh';
const REMEMBER = 'mylog_remember';
const refreshes = new Map<string, Promise<Tokens | null>>();

export async function saveTokens(tokens: Tokens, remember: boolean) {
  const jar = await cookies();
  const options = { httpOnly: true, secure: process.env.NODE_ENV === 'production', sameSite: 'lax' as const, path: '/' };
  jar.set(ACCESS, tokens.accessToken, { ...options, maxAge: tokens.expiresInSeconds });
  jar.set(REFRESH, tokens.refreshToken, { ...options, ...(remember ? { maxAge: 30 * 24 * 60 * 60 } : {}) });
  jar.set(REMEMBER, remember ? '1' : '0', options);
}

export async function clearTokens() {
  const jar = await cookies();
  jar.delete(ACCESS);
  jar.delete(REFRESH);
  jar.delete(REMEMBER);
}

export async function accessToken() {
  return (await cookies()).get(ACCESS)?.value;
}

export async function refreshSession() {
  const jar = await cookies();
  const oldToken = jar.get(REFRESH)?.value;
  if (!oldToken) return null;
  const key = createHash('sha256').update(oldToken).digest('hex');
  let pending = refreshes.get(key);
  if (!pending) {
    pending = (async () => {
      try {
        const response = await authBackend('/auth/refresh', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: oldToken }),
        });
        return response.ok ? await response.json() as Tokens : null;
      } catch { return null; }
    })();
    refreshes.set(key, pending);
    // Keep the rotated result briefly for requests sent with the same old cookie.
    void pending.finally(() => setTimeout(() => refreshes.delete(key), 5000));
  }
  const tokens = await pending;
  if (tokens) await saveTokens(tokens, jar.get(REMEMBER)?.value === '1');
  else await clearTokens();
  return tokens?.accessToken ?? null;
}
