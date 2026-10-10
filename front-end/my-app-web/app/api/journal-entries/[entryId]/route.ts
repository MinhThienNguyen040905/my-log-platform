import { journalProxy } from '@/features/journal/api/proxy';

type Params = { params: Promise<{ entryId: string }> };
const path = async (context: Params) => `/journal-entries/${encodeURIComponent((await context.params).entryId)}`;

export async function GET(request: Request, context: Params) { return journalProxy(request, await path(context)); }
export async function PATCH(request: Request, context: Params) { return journalProxy(request, await path(context)); }
export async function DELETE(request: Request, context: Params) { return journalProxy(request, await path(context)); }
