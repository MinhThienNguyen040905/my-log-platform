import { accountProxy } from '@/features/user/api/proxy';

export function POST(request: Request) { return accountProxy(request, '/me/onboarding:complete'); }
