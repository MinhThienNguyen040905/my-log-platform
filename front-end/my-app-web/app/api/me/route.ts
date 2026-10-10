import { accountProxy } from '@/features/user/api/proxy';

export function GET(request: Request) { return accountProxy(request, '/me'); }
export function PATCH(request: Request) { return accountProxy(request, '/me'); }
