import { ForgotPasswordForm } from '@/features/auth';

export const metadata = {
  title: 'Quên mật khẩu - MyLog',
  description: 'Khôi phục chìa khóa vào cuốn sổ tay cá nhân của bạn.',
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}

