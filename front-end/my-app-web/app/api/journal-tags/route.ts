import { journalProxy } from '@/features/journal/api/proxy';

export function GET(request: Request) { return journalProxy(request, '/journal-tags'); }
export function POST(request: Request) { return journalProxy(request, '/journal-tags'); }
