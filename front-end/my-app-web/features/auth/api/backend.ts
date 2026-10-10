import 'server-only';

const backendUrl = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';

export async function authBackend(path: string, init: RequestInit = {}) {
  return fetch(`${backendUrl}/api/v1${path}`, { ...init, cache: 'no-store' });
}

export async function forwardProblem(response: Response) {
  const body = await response.text();
  return new Response(body, {
    status: response.status,
    headers: { 'Content-Type': response.headers.get('content-type') ?? 'application/problem+json' },
  });
}

export function sameOrigin(request: Request) {
  const origin = request.headers.get('origin');
  return origin === new URL(request.url).origin;
}
