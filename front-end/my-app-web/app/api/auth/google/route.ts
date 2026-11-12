import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';
import { saveTokens } from '@/features/auth/api/session';
import { deviceNameFrom } from '@/features/auth/utils/device-name';
import { cookies } from 'next/headers';

const OAUTH_STATE_COOKIE = 'mylog_oauth_state';

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });

  let body: { code?: string; state?: string; timezone?: string; locale?: string; rememberMe?: boolean };
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: 'Invalid JSON body' }, { status: 400 });
  }

  if (!body.code || typeof body.code !== 'string') {
    return Response.json({ error: 'Missing or invalid authorization code' }, { status: 400 });
  }

  // Chống Login CSRF bằng cách so khớp OAuth state
  const jar = await cookies();
  const savedState = jar.get(OAUTH_STATE_COOKIE)?.value;
  if (!body.state || !savedState || body.state !== savedState) {
    return Response.json({ error: 'Invalid OAuth state' }, { status: 403 });
  }
  jar.delete(OAUTH_STATE_COOKIE);

  try {
    const response = await authBackend('/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code: body.code,
        timezone: body.timezone,
        locale: body.locale,
        deviceName: deviceNameFrom(request.headers.get('user-agent')),
      }),
    });

    if (!response.ok) return forwardProblem(response);

    const tokens = await response.json();
    await saveTokens(tokens, body.rememberMe === true);
    return Response.json({ userId: tokens.userId }, { headers: { 'Cache-Control': 'no-store' } });
  } catch {
    return new Response(null, { status: 502 });
  }
}

