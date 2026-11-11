import { authBackend, forwardProblem, sameOrigin } from '@/features/auth/api/backend';

/** Requests a reset email. Backend answers 202 with the same generic message whether or not the email exists. */
export async function POST(request: Request) {
  if (!sameOrigin(request)) return new Response(null, { status: 403 });
  try {
    const response = await authBackend('/auth/password-resets', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: await request.text(),
    });
    return response.ok ? new Response(null, { status: 202 }) : forwardProblem(response);
  } catch { return new Response(null, { status: 503 }); }
}
