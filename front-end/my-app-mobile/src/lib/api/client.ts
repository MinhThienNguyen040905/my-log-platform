import { tokenStorage } from '@/lib/storage/token-storage';

export type AuthTokens = { userId: string; accessToken: string; refreshToken: string; expiresInSeconds: number };
export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

const baseUrl = process.env.EXPO_PUBLIC_API_BASE_URL?.replace(/\/$/, '');
let accessToken: string | null = null;
let refreshPending: Promise<string | null> | null = null;
let sessionGeneration = 0;
let onSessionCleared: (() => void) | null = null;

export function setSessionClearedListener(listener: (() => void) | null) { onSessionCleared = listener; }

function endpoint(path: string) {
  if (!baseUrl || !/^https?:\/\//.test(baseUrl)) throw new Error('Chưa cấu hình EXPO_PUBLIC_API_BASE_URL.');
  return `${baseUrl}/api/v1${path}`;
}

async function send(path: string, init: RequestInit, token?: string) {
  try {
    return await fetch(endpoint(path), {
      ...init,
      headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}), ...init.headers },
    });
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, 'Không thể kết nối máy chủ. Kiểm tra mạng và địa chỉ API.');
  }
}

export async function clearSession() {
  sessionGeneration++;
  accessToken = null;
  await tokenStorage.clear();
  onSessionCleared?.();
}

export async function saveSession(tokens: AuthTokens) {
  // Persist the rotated token before exposing its new access token to requests.
  await tokenStorage.write(tokens.refreshToken);
  accessToken = tokens.accessToken;
}

export async function refreshSession(): Promise<string | null> {
  if (refreshPending) return refreshPending;
  refreshPending = (async () => {
    const generation = sessionGeneration;
    const refreshToken = await tokenStorage.read();
    if (!refreshToken) return null;
    const response = await send('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) });
    if (response.status === 401) { await clearSession(); return null; }
    if (!response.ok) throw new ApiError(response.status, 'Không thể làm mới phiên. Vui lòng thử lại.');
    if (generation !== sessionGeneration) return null;
    await saveSession(await response.json() as AuthTokens);
    return accessToken;
  })().finally(() => { refreshPending = null; });
  return refreshPending;
}

export async function apiRequest(path: string, init: RequestInit = {}, authenticated = true): Promise<Response> {
  let token = authenticated ? accessToken ?? await refreshSession() : null;
  if (authenticated && !token) throw new ApiError(401, 'Phiên đăng nhập đã hết hạn.');
  let response = await send(path, init, token ?? undefined);
  if (authenticated && response.status === 401) {
    token = await refreshSession();
    if (!token) throw new ApiError(401, 'Phiên đăng nhập đã hết hạn.');
    response = await send(path, init, token);
  }
  if (response.status === 401 && authenticated) await clearSession();
  if (!response.ok) throw new ApiError(response.status, errorMessage(response.status));
  return response;
}

export function jsonBody(value: unknown): RequestInit { return { body: JSON.stringify(value) }; }

function errorMessage(status: number) {
  if (status === 400) return 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
  if (status === 401) return 'Thông tin đăng nhập hoặc mã xác minh không hợp lệ.';
  if (status === 409) return 'Dữ liệu đã thay đổi. Vui lòng tải lại trước khi lưu.';
  if (status === 429) return 'Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại.';
  if (status >= 500) return 'Máy chủ đang gặp sự cố. Vui lòng thử lại.';
  return 'Không thể hoàn tất yêu cầu.';
}
