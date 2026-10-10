import { accountProxy } from '@/features/user/api/proxy';

export async function DELETE(request: Request, context: { params: Promise<{ sessionId: string }> }) {
  const { sessionId } = await context.params;
  return accountProxy(request, `/me/sessions/${encodeURIComponent(sessionId)}`);
}
