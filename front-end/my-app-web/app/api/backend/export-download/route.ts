import { sameOrigin } from '@/features/auth/api/backend';
import { securedProxy } from '@/features/auth/api/secured-proxy';

const uuid = /^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$/;

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  let input: { id?: unknown; url?: unknown };
  try { input = await request.json(); }
  catch { return new Response(null, { status: 400 }); }
  if (typeof input.id !== 'string' || !uuid.test(input.id) || typeof input.url !== 'string') {
    return new Response(null, { status: 400 });
  }

  let grant: URL;
  try { grant = new URL(input.url, 'http://mylog.local'); }
  catch { return new Response(null, { status: 400 }); }
  if (grant.origin !== 'http://mylog.local' || grant.pathname !== `/api/v1/exports/${input.id}/file`
      || !grant.searchParams.get('expires') || !grant.searchParams.get('signature')) {
    return new Response(null, { status: 400 });
  }

  const internalUrl = new URL(`/api/backend/exports/${input.id}/file${grant.search}`, request.url);
  return securedProxy(new Request(internalUrl, { method: 'GET' }), ['exports', input.id, 'file']);
}
