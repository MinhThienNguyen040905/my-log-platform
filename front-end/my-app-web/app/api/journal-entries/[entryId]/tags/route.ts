import { journalProxy } from '@/features/journal/api/proxy';

export async function GET(request: Request, context: { params: Promise<{ entryId: string }> }) {
  return journalProxy(request, `/journal-entries/${encodeURIComponent((await context.params).entryId)}/tags`);
}
