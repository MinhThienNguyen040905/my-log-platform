export type Profile = { userId: string; displayName: string | null; penName: string | null; onboardingGoals: string[]; timezone: string; locale: string; onboardingCompletedAt: string | null; version: number };
export type Consent = { type: string; documentVersion: string; granted: boolean; decidedAt: string };

export class AccountApiError extends Error {
  constructor(message: string, public readonly status: number) { super(message); }
}

const supportedOnboardingGoals = new Set(['REFLECTION', 'SLEEP', 'MINDFULNESS', 'EXERCISE', 'SOCIAL']);

export async function accountRequest(path: string, init: RequestInit = {}) {
  const response = await fetch(`/api/me${path}`, { cache: 'no-store', ...init });
  if (!response.ok) {
    if (response.status === 401) throw new AccountApiError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 401);
    if (response.status === 409) throw new AccountApiError('Hồ sơ đã thay đổi ở nơi khác. Vui lòng kiểm tra và thử lại.', 409);
    if (response.status === 429) throw new Error('Bạn đã thao tác quá nhiều lần. Vui lòng thử lại sau.');
    if (response.status === 503) throw new Error('Không thể kết nối máy chủ. Vui lòng thử lại.');
    throw new Error('Không thể hoàn tất yêu cầu. Vui lòng thử lại.');
  }
  return response;
}

export async function getProfile(): Promise<Profile> { return (await accountRequest('')).json(); }

export async function getConsents(): Promise<Consent[]> { return (await accountRequest('/consents')).json(); }

export async function setConsent(type: string, documentVersion: string, granted: boolean): Promise<void> {
  await accountRequest(`/consents/${encodeURIComponent(type)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ documentVersion, granted }),
  });
}

export async function updateProfile(data: Partial<Pick<Profile, 'displayName' | 'penName' | 'timezone' | 'locale' | 'onboardingGoals'>>, version: number): Promise<Profile> {
  if (data.onboardingGoals?.some((goal) => !supportedOnboardingGoals.has(goal))
    || (data.onboardingGoals && new Set(data.onboardingGoals).size !== data.onboardingGoals.length)) {
    throw new Error('Mục tiêu không còn được hỗ trợ. Vui lòng tải lại trang và chọn lại.');
  }
  return (await accountRequest('', { method: 'PATCH', headers: { 'Content-Type': 'application/json', 'If-Match': `"${version}"` }, body: JSON.stringify(data) })).json();
}

export async function completeOnboarding(version: number): Promise<Profile> {
  return (await accountRequest('/onboarding', { method: 'POST', headers: { 'If-Match': `"${version}"` } })).json();
}
