import { LoginForm } from '@/features/auth';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

export const metadata = {
  title: 'Đăng nhập - MyLog',
  description: 'Mở cuốn sổ tay cá nhân và tiếp tục hành trình chăm sóc tinh thần của bạn.',
};

export default async function LoginPage() {
  if ((await cookies()).has('mylog_refresh')) redirect('/dashboard');
  return <LoginForm />;
}
