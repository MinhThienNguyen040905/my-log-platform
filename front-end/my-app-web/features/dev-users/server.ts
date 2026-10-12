import 'server-only';

import type { DevAccount } from './types';

export async function listDevUsers(before?: string): Promise<{ status: number; accounts: DevAccount[] }> {
  if (process.env.NODE_ENV !== 'development') return { status: 404, accounts: [] };
  const key = process.env.MYLOG_DEV_USERS_KEY;
  if (!key || key.length < 16) return { status: 503, accounts: [] };

  try {
    const url = new URL('/internal/dev/users', process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080');
    if (before) url.searchParams.set('before', before);
    const response = await fetch(url, { headers: { 'X-Dev-Users-Key': key }, cache: 'no-store' });
    return { status: response.status, accounts: response.ok ? await response.json() as DevAccount[] : [] };
  } catch {
    return { status: 503, accounts: [] };
  }
}
