export class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string | null, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export function apiQuery(values: Record<string, string | number | boolean | null | undefined>): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value));
  }
  const result = query.toString();
  return result ? `?${result}` : '';
}

export function apiJsonBody(value: unknown, headers: Record<string, string> = {}): RequestInit {
  return { headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(value) };
}

export async function apiResponse(path: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(`/api/backend/${path}`, { ...init, cache: 'no-store' });
  if (response.ok) return response;
  let code: string | null = null;
  let message = response.status === 401 ? 'Phiên đăng nhập đã hết hạn.'
    : response.status === 403 ? 'Bạn không có quyền thực hiện thao tác này.'
    : response.status === 409 ? 'Dữ liệu đã thay đổi. Vui lòng tải lại.'
    : response.status === 429 ? 'Bạn đã thao tác quá nhiều lần. Vui lòng thử lại sau.'
    : response.status === 503 ? 'Không thể kết nối máy chủ.'
    : 'Không thể hoàn tất yêu cầu.';
  if (response.headers.get('content-type')?.includes('application/problem+json')) {
    try {
      const problem = await response.json() as { code?: string; detail?: string };
      code = typeof problem.code === 'string' ? problem.code : null;
      if (typeof problem.detail === 'string') message = problem.detail;
    } catch { /* Keep the generic message. */ }
  }
  throw new ApiError(response.status, code, message);
}

export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  return (await apiResponse(path, init)).json() as Promise<T>;
}
