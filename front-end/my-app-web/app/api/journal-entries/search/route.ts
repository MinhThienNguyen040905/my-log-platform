import { journalProxy } from '@/features/journal/api/proxy';

export function GET(request: Request) {
  return journalProxy(request, `/journal-entries/search${new URL(request.url).search}`);
}

