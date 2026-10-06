import { LoginForm } from '@/features/auth';

export const metadata = {
  title: 'Đăng nhập - MyLog',
  description: 'Mở cuốn sổ tay cá nhân và tiếp tục hành trình chăm sóc tinh thần của bạn.',
};

export default function LoginPage() {
  return <LoginForm />;
}
