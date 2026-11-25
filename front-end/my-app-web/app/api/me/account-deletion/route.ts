import { accountProxy } from '@/features/user/api/proxy';

/** UC-23: schedules account deletion in 7 days. The backend answers 404 while the feature is switched off. */
export function POST(request: Request) { return accountProxy(request, '/account-deletion-requests'); }
