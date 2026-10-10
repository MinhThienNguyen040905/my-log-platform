import { securedProxy } from '@/features/auth/api/secured-proxy';

type Context = { params: Promise<{ segments: string[] }> };
async function forward(request: Request, context: Context) {
  return securedProxy(request, (await context.params).segments);
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;
