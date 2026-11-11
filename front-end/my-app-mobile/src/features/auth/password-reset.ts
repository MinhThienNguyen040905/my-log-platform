import { ApiError, apiRequest, clearSession, jsonBody } from '@/lib/api/client';

/**
 * Đặt lại mật khẩu (backend `IdentityController`). Mobile dùng mã 8 chữ số; liên kết trong email
 * mở trang web `/auth/reset-password` cho tới khi app cấu hình Universal Links / App Links.
 */
export async function requestPasswordReset(email: string) {
  await apiRequest('/auth/password-resets', { method: 'POST', ...jsonBody({ email }) }, false);
}

export async function resetPasswordWithCode(email: string, code: string, newPassword: string) {
  await apiRequest('/auth/password-resets:confirm-code', { method: 'POST',
    ...jsonBody({ email, code, newPassword }) }, false);
  // Backend thu hồi mọi phiên khi đặt lại thành công; xóa token cũ còn trong SecureStore.
  await clearSession();
}

// Email chuyển từ màn "Quên mật khẩu" sang màn nhập mã; chỉ giữ trong bộ nhớ, không ghi xuống máy.
let pendingResetEmail = '';
export function rememberResetEmail(email: string) { pendingResetEmail = email.trim(); }
export function readResetEmail() { return pendingResetEmail; }
export function clearResetEmail() { pendingResetEmail = ''; }

/** Backend chỉ nhận mật khẩu 12–72 byte UTF-8. */
export function passwordProblem(password: string, confirm: string): string | null {
  if (password.length < 12) return 'Mật khẩu mới cần ít nhất 12 ký tự.';
  if (new TextEncoder().encode(password).length > 72) return 'Mật khẩu tối đa 72 byte (chữ có dấu tính 2 byte).';
  if (password !== confirm) return 'Mật khẩu nhập lại chưa khớp.';
  return null;
}

export function resetErrorMessage(cause: unknown): string {
  if (cause instanceof ApiError) {
    if (cause.status === 401) return 'Mã không hợp lệ hoặc đã hết hạn. Hãy gửi yêu cầu mới.';
    if (cause.detail === 'NEW_PASSWORD_SAME_AS_OLD') return 'Mật khẩu mới phải khác mật khẩu hiện tại.';
    if (cause.detail === 'PASSWORD_CONTAINS_USER_INFO') return 'Mật khẩu không được chứa phần tên trong email của bạn.';
    if (cause.detail === 'PASSWORD_LENGTH_INVALID') return 'Mật khẩu mới cần dài 12–72 byte.';
    return cause.message;
  }
  return 'Không thể đặt lại mật khẩu. Vui lòng thử lại.';
}
