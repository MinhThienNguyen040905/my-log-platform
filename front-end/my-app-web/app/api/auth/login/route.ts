import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';
import { saveTokens } from '@/features/auth/api/session';
import { deviceNameFrom } from '@/features/auth/utils/device-name';

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    const { email, password, rememberMe } = await request.json();
    const response = await authBackend('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, deviceName: deviceNameFrom(request.headers.get('user-agent')) }),
    });
    if (!response.ok) return forwardProblem(response);
    const tokens = await response.json();
    await saveTokens(tokens, rememberMe === true);
    return Response.json({ userId: tokens.userId }, { headers: { 'Cache-Control': 'no-store' } });
  } catch { return new Response(null, { status: 503 }); }
}
