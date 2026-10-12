import { journalProxy } from '@/features/journal/api/proxy';

export function POST(request: Request) {
  return journalProxy(request, '/journal-writing-suggestions');
}
