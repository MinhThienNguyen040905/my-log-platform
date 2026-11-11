import type { APIRequestContext } from '@playwright/test';

/**
 * Local-only helpers: read synthetic emails from Mailpit and create synthetic accounts directly
 * against the local backend. Never point these at a shared or production environment.
 */
export const MAILPIT_URL = process.env.MAILPIT_URL ?? 'http://localhost:8025';
export const BACKEND_URL = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';

export async function localStackAvailable(request: APIRequestContext): Promise<boolean> {
  try {
    const [backend, mailpit] = await Promise.all([
      request.get(`${BACKEND_URL}/actuator/health`, { timeout: 3000 }),
      request.get(`${MAILPIT_URL}/api/v1/messages?limit=1`, { timeout: 3000 }),
    ]);
    return backend.ok() && mailpit.ok();
  } catch {
    return false;
  }
}

export function syntheticEmail(label: string): string {
  return `qa+${label}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.test`;
}

/** Waits for the newest email to `to` whose subject contains `subject`, returns its plain text. */
export async function waitForEmail(request: APIRequestContext, to: string, subject: string, timeoutMs = 15000): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const search = await request.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${to}"` } });
    if (search.ok()) {
      const { messages } = (await search.json()) as { messages: { ID: string; Subject: string }[] };
      const match = messages.find((message) => message.Subject.includes(subject));
      if (match) {
        const message = await request.get(`${MAILPIT_URL}/api/v1/message/${match.ID}`);
        const body = (await message.json()) as { Text: string };
        return body.Text;
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`No "${subject}" email arrived in Mailpit in time`);
}

export function extractCode(text: string): string {
  const match = text.match(/\b(\d{8})\b/);
  if (!match) throw new Error('No 8-digit code in email');
  return match[1];
}

/** Logs in through the backend API (not the web) and returns the access token. */
export async function backendLogin(request: APIRequestContext, email: string, password: string): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/v1/auth/login`, { data: { email, password, deviceName: 'playwright' } });
  if (!response.ok()) throw new Error(`Login failed with ${response.status()}`);
  return ((await response.json()) as { accessToken: string }).accessToken;
}

/** A verified synthetic account that has finished onboarding, so web routes open without a detour. */
export async function createOnboardedAccount(request: APIRequestContext, label: string) {
  const account = await createVerifiedAccount(request, label);
  const token = await backendLogin(request, account.email, account.password);
  const auth = { Authorization: `Bearer ${token}` };
  const profile = await request.get(`${BACKEND_URL}/api/v1/me`, { headers: auth });
  const etag = profile.headers()['etag'];
  const completed = await request.post(`${BACKEND_URL}/api/v1/me/onboarding:complete`, { headers: { ...auth, 'If-Match': etag } });
  if (!completed.ok()) throw new Error(`Onboarding failed with ${completed.status()}`);
  return { ...account, token };
}

/** Registers and verifies a synthetic account through the backend API, returns its credentials. */
export async function createVerifiedAccount(request: APIRequestContext, label: string) {
  const email = syntheticEmail(label);
  const password = `Synthetic-Pass-${Math.floor(Math.random() * 1e9)}`;
  const registered = await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
    data: {
      email, password, timezone: 'Asia/Ho_Chi_Minh', locale: 'vi',
      termsVersion: 'v1', privacyVersion: 'v1', acceptTerms: true, acceptPrivacy: true,
    },
  });
  if (!registered.ok()) throw new Error(`Register failed with ${registered.status()}`);
  const code = extractCode(await waitForEmail(request, email, 'xác minh email'));
  const verified = await request.post(`${BACKEND_URL}/api/v1/auth/email-verifications:confirm-code`, { data: { email, code } });
  if (!verified.ok()) throw new Error(`Verification failed with ${verified.status()}`);
  return { email, password };
}
