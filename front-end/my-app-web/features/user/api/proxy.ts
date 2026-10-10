import 'server-only';
import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';
import { accessToken, clearTokens, refreshSession } from '@/features/auth/api/session';

export async function accountProxy(request: Request, path: string) {
  if (request.method !== 'GET' && !sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    let token = await accessToken() ?? await refreshSession();
    if (!token) return new Response(null, { status: 401 });
    const body = request.method === 'PATCH' || request.method === 'PUT' ? await request.text() : undefined;
    const send = (bearer: string) => authBackend(path, {
      method: request.method,
      headers: {
        Authorization: `Bearer ${bearer}`,
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(request.headers.has('if-match') ? { 'If-Match': request.headers.get('if-match')! } : {}),
      },
      ...(body !== undefined ? { body } : {}),
    });
    let response = await send(token);
    if (response.status === 401) {
      token = await refreshSession();
      if (!token) return new Response(null, { status: 401 });
      response = await send(token);
    }
    if (response.status === 401) await clearTokens();
    if (!response.ok) return forwardProblem(response);
    return new Response(response.status === 204 ? null : await response.text(), {
      status: response.status,
      headers: {
        'Cache-Control': 'no-store',
        ...(response.status !== 204 ? { 'Content-Type': 'application/json' } : {}),
        ...(response.headers.has('etag') ? { ETag: response.headers.get('etag')! } : {}),
      },
    });
  } catch { return new Response(null, { status: 503 }); }
}
