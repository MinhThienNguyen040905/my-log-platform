import { authBackend, sameOrigin } from '@/features/auth/api/backend';
import { accessToken, clearTokens, refreshSession } from '@/features/auth/api/session';

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    const token = await accessToken() ?? await refreshSession();
    if (token) {
      await authBackend('/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
    }
    await clearTokens();
    return new Response(null, { status: 204 });
  } catch {
    await clearTokens();
    return new Response(null, { status: 204 });
  }
}
