import { listDevUsers } from '@/features/dev-users/server';

export async function GET(request: Request) {
  if (process.env.NODE_ENV !== 'development') return new Response(null, { status: 404 });
  const result = await listDevUsers(new URL(request.url).searchParams.get('before') ?? undefined);
  if (result.status !== 200) return new Response(null, { status: result.status, headers: { 'Cache-Control': 'no-store' } });
  return Response.json(result.accounts, { headers: { 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer' } });
}
