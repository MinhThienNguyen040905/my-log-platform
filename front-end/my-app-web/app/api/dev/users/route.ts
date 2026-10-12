import { sameOrigin } from '@/features/auth/api/backend';

export async function POST(request: Request) {
  if (process.env.NODE_ENV !== 'development') return new Response(null, { status: 404 });
  if (!sameOrigin(request)) return new Response(null, { status: 403 });

  try {
    const { key, before } = await request.json();
    if (typeof key !== 'string' || key.length < 16 || (before !== undefined && typeof before !== 'string')) {
      return new Response(null, { status: 400 });
    }
    const url = new URL('/internal/dev/users', process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080');
    if (before) url.searchParams.set('before', before);
    const response = await fetch(url, {
      headers: { 'X-Dev-Users-Key': key },
      cache: 'no-store',
    });
    if (!response.ok) return new Response(null, { status: response.status, headers: { 'Cache-Control': 'no-store' } });
    return new Response(await response.text(), {
      headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer' },
    });
  } catch {
    return new Response(null, { status: 503, headers: { 'Cache-Control': 'no-store' } });
  }
}
