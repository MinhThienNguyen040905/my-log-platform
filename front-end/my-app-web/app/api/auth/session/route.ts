import { authBackend, forwardProblem } from '@/features/auth/api/backend';
import { accessToken, clearTokens, refreshSession } from '@/features/auth/api/session';

export async function GET() {
  try {
    let token = await accessToken() ?? await refreshSession();
    if (!token) return new Response(null, { status: 401 });
    let response = await authBackend('/me', { headers: { Authorization: `Bearer ${token}` } });
    if (response.status === 401) {
      token = await refreshSession();
      if (!token) return new Response(null, { status: 401 });
      response = await authBackend('/me', { headers: { Authorization: `Bearer ${token}` } });
    }
    if (response.status === 401) await clearTokens();
    return response.ok
      ? new Response(await response.text(), { status: 200, headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' } })
      : forwardProblem(response);
  } catch { return new Response(null, { status: 503 }); }
}
