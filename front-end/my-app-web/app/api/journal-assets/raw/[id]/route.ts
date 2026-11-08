import 'server-only';

const backendUrl = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';

export async function GET(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  const url = new URL(request.url);
  const expires = url.searchParams.get('expires') ?? '';
  const sig = url.searchParams.get('sig') ?? '';

  const backendRes = await fetch(
    `${backendUrl}/api/v1/journal-assets/raw/${id}?expires=${encodeURIComponent(expires)}&sig=${encodeURIComponent(sig)}`,
    { cache: 'no-store' }
  );

  if (!backendRes.ok) {
    return new Response(null, { status: backendRes.status });
  }

  const headers = new Headers();
  const contentType = backendRes.headers.get('content-type');
  if (contentType) headers.set('content-type', contentType);
  headers.set('cache-control', 'private, max-age=900');
  headers.set('x-content-type-options', 'nosniff');

  return new Response(backendRes.body, {
    status: 200,
    headers,
  });
}

