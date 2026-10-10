import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';

export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    const response = await authBackend('/auth/register', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: await request.text(),
    });
    return response.ok
      ? Response.json(await response.json(), { status: response.status })
      : forwardProblem(response);
  } catch { return new Response(null, { status: 503 }); }
}
