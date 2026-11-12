import { journalProxy } from '@/features/journal/api/proxy';

export function GET(request: Request) {
  return journalProxy(request, `/journal-entries/daily-counts${new URL(request.url).search}`);
}
