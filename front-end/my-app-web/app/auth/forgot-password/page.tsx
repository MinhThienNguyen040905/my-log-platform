import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { ForgotPasswordForm } from '@/features/auth';

export const metadata = {
  title: 'Quên mật khẩu - MyLog',
  description: 'Gửi hướng dẫn đặt lại mật khẩu tài khoản MyLog.',
};

export default async function ForgotPasswordPage() {
  if ((await cookies()).has('mylog_refresh')) redirect('/dashboard');
  return <ForgotPasswordForm />;
}
