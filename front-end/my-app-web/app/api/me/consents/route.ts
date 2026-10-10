import { accountProxy } from '@/features/user/api/proxy';

export function GET(request: Request) { return accountProxy(request, '/me/consents'); }
