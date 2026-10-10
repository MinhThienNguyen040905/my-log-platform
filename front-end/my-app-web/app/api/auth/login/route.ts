import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';
import { saveTokens } from '@/features/auth/api/session';

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    const { email, password, rememberMe } = await request.json();
    const response = await authBackend('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, deviceName: 'MyLog Web' }),
    });
    if (!response.ok) return forwardProblem(response);
    const tokens = await response.json();
    await saveTokens(tokens, rememberMe === true);
    return Response.json({ userId: tokens.userId }, { headers: { 'Cache-Control': 'no-store' } });
  } catch { return new Response(null, { status: 503 }); }
}
