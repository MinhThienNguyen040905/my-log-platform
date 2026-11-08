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

/**
 * True only if the request's `Origin` header names the same host the client actually connected
 * to (`Host` header) — the standard, proxy-safe anti-CSRF check. Deliberately does NOT compare
 * against `new URL(request.url).origin`: under `next dev` that always resolves to `localhost`
 * regardless of the dev server's bind address (`--hostname 127.0.0.1` in package.json's `dev`
 * script), which made every same-origin POST from `http://127.0.0.1:3000` get rejected here before
 * ever reaching the backend — confirmed directly, same request, only the `Origin` host differed.
 * Missing `Origin` or `Host` fails closed (rejected), same as before this fix.
 */
export function sameOrigin(request: Request) {
  const origin = request.headers.get('origin');
  const host = request.headers.get('host');
  if (!origin || !host) return false;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}
