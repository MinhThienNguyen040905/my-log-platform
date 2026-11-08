import { test, expect } from '@playwright/test';

/**
 * Regression for the `sameOrigin()` fix in features/auth/api/backend.ts: a same-origin POST from
 * `BASE_URL` must reach the backend (never a bare 403 from the guard itself), while a forged
 * `Origin` or a missing one must still be rejected before the backend is called. Uses a bogus
 * account on purpose — this only needs to prove the guard's verdict, not log anyone in, and must
 * not spend the real test account's login-attempt quota (see IdentityService: 5/15min/subject).
 */
test.describe('sameOrigin() guard on /api/auth/login', () => {
  const body = { email: 'playwright-same-origin-probe@example.invalid', password: 'not-a-real-password' };

  test('matching Origin reaches the backend (401 invalid-credentials, not 403)', async ({ request, baseURL }) => {
    const res = await request.post(`${baseURL}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json', Origin: baseURL! },
      data: body,
    });
    expect(res.status()).toBe(401);
  });

  test('forged Origin is rejected before the backend is called', async ({ request, baseURL }) => {
    const res = await request.post(`${baseURL}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json', Origin: 'http://evil.example.invalid' },
      data: body,
    });
    expect(res.status()).toBe(403);
  });

  test('missing Origin is rejected', async ({ request, baseURL }) => {
    const res = await request.post(`${baseURL}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: body,
    });
    expect(res.status()).toBe(403);
  });
});
