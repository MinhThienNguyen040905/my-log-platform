import { accountProxy } from '@/features/user/api/proxy';

export async function PUT(request: Request, context: { params: Promise<{ type: string }> }) {
  const { type } = await context.params;
  return accountProxy(request, `/me/consents/${encodeURIComponent(type)}`);
}
