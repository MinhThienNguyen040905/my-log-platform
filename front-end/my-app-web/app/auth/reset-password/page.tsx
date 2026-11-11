import { ResetPasswordForm } from '@/features/auth';

export const metadata = {
  title: 'Đặt lại mật khẩu - MyLog',
  description: 'Đặt mật khẩu mới cho tài khoản MyLog.',
};

// Không chuyển hướng khi đã có phiên: người dùng có thể đang đăng nhập ở tab khác
// mà vẫn cần đặt lại mật khẩu từ liên kết trong email.
export default function ResetPasswordPage() {
  return <ResetPasswordForm />;
}
