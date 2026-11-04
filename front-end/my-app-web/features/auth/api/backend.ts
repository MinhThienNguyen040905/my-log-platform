import 'server-only';
import { headers } from 'next/headers';

const backendUrl = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';

export async function authBackend(path: string, init: RequestInit = {}) {
  const headerList = await headers();
  const rawForwarded = headerList.get('x-forwarded-for') || headerList.get('x-real-ip');
  const clientIp = rawForwarded ? rawForwarded.split(',')[0].trim() : '';

  const mergedHeaders = new Headers(init.headers);
  if (clientIp) {
    mergedHeaders.set('X-Forwarded-For', clientIp);
  }

  return fetch(`${backendUrl}/api/v1${path}`, {
    ...init,
    headers: mergedHeaders,
    cache: 'no-store',
  });
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
