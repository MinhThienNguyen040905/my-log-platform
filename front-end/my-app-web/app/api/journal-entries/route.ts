import { journalProxy } from '@/features/journal/api/proxy';

export function GET(request: Request) {
  return journalProxy(request, `/journal-entries${new URL(request.url).search}`);
}

export function POST(request: Request) {
  return journalProxy(request, '/journal-entries');
}
