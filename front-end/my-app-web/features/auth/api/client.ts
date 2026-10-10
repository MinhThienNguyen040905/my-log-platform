export async function authPost(path: string, body: unknown): Promise<Response> {
  return fetch(`/api/auth/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export async function authError(response: Response, locale: 'vi' | 'en' = 'vi'): Promise<string> {
  const en = locale === 'en';
  if (response.status === 503) return en ? 'Could not connect to the server. Please try again.' : 'Không thể kết nối máy chủ. Vui lòng thử lại.';
  if (response.status === 429) return en ? 'Too many attempts. Please wait and try again.' : 'Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại.';
  try {
    const problem = await response.json();
    if (response.status === 401) return en ? 'The email or password is incorrect, or the account is not verified.' : 'Email hoặc mật khẩu không đúng, hoặc tài khoản chưa được xác minh.';
    if (response.status === 409) return en ? 'This email is already registered.' : 'Email này đã được đăng ký.';
    return en ? 'Could not complete the request. Please try again.' : typeof problem.detail === 'string' ? problem.detail : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.';
  } catch { return en ? 'Could not complete the request. Please try again.' : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.'; }
}
