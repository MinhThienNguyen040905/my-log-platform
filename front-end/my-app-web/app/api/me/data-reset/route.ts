import { accountProxy } from '@/features/user/api/proxy';

/** "Start a new journey": deletes the user's journal data, keeps the account. */
export function POST(request: Request) { return accountProxy(request, '/me/data:reset'); }
