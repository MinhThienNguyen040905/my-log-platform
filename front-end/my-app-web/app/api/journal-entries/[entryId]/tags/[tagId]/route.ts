import { journalProxy } from '@/features/journal/api/proxy';

type Params = { params: Promise<{ entryId: string; tagId: string }> };
const path = async (context: Params) => {
  const { entryId, tagId } = await context.params;
  return `/journal-entries/${encodeURIComponent(entryId)}/tags/${encodeURIComponent(tagId)}`;
};
export async function PUT(request: Request, context: Params) { return journalProxy(request, await path(context)); }
export async function DELETE(request: Request, context: Params) { return journalProxy(request, await path(context)); }
