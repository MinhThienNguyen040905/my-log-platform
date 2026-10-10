import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { AuthGate } from '@/app/_components/AuthGate';

export default async function AppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  if (!(await cookies()).has('mylog_refresh')) redirect('/auth/login');
  return <AuthGate><main className='flex-1 pt-20 w-full'>{children}</main></AuthGate>;
}
