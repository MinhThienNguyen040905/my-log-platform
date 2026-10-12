import { notFound } from 'next/navigation';
import { DevUsersView } from '@/features/dev-users/DevUsersView';
import { listDevUsers } from '@/features/dev-users/server';

export const dynamic = 'force-dynamic';

export default async function DevUsersPage() {
  if (process.env.NODE_ENV !== 'development') notFound();
  const result = await listDevUsers();
  return <DevUsersView initialAccounts={result.accounts} initialStatus={result.status} />;
}
