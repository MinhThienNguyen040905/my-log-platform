import { notFound } from 'next/navigation';
import { DevUsersView } from '@/features/dev-users/DevUsersView';

export const dynamic = 'force-dynamic';

export default function DevUsersPage() {
  if (process.env.NODE_ENV !== 'development') notFound();
  return <DevUsersView />;
}
