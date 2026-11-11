// Chuyển email từ trang "Quên mật khẩu" sang trang "Đặt lại mật khẩu" mà không đưa email lên URL
// (URL đi vào lịch sử trình duyệt và log). Chỉ giữ trong sessionStorage của tab, hết hạn sau 20 phút
// — dài hơn hạn 15 phút của mã đặt lại.
const key = 'mylog-pending-password-reset-email';
const lifetimeMs = 20 * 60 * 1000;

export function rememberPasswordResetEmail(email: string) {
  try {
    sessionStorage.setItem(key, JSON.stringify({ email: email.trim(), expiresAt: Date.now() + lifetimeMs }));
  } catch { /* Người dùng vẫn có thể tự nhập lại email. */ }
}

export function readPasswordResetEmail(): string {
  try {
    const value: unknown = JSON.parse(sessionStorage.getItem(key) ?? 'null');
    if (value && typeof value === 'object' && 'email' in value && 'expiresAt' in value
      && typeof value.email === 'string' && typeof value.expiresAt === 'number'
      && value.expiresAt > Date.now() && value.email.length <= 254) return value.email;
    sessionStorage.removeItem(key);
  } catch { /* Storage có thể không dùng được hoặc chứa giá trị cũ. */ }
  return '';
}

export function forgetPasswordResetEmail() {
  try { sessionStorage.removeItem(key); } catch { /* Storage có thể không dùng được. */ }
}
