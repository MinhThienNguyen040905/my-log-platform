import { RegisterForm } from '@/features/auth';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

export const metadata = {
  title: 'Đăng ký tài khoản - MyLog',
  description: 'Tạo tài khoản và bắt đầu viết những trang nhật ký đầu tiên cùng MyLog.',
};

export default async function RegisterPage() {
  if ((await cookies()).has('mylog_refresh')) redirect('/dashboard');
  return <RegisterForm />;
}
