import { RegisterForm } from '@/features/auth';

export const metadata = {
  title: 'Đăng ký tài khoản - MyLog',
  description: 'Tạo tài khoản và bắt đầu viết những trang nhật ký đầu tiên cùng MyLog.',
};

export default function RegisterPage() {
  return <RegisterForm />;
}
