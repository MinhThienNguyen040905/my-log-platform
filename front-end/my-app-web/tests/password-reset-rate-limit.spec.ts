import { test, expect } from '@playwright/test';
import { localStackAvailable, syntheticEmail } from './helpers/mailpit';

/**
 * W1-11 — vượt giới hạn tần suất (backend: 5 yêu cầu / 15 phút / email, 10 / 15 phút / IP).
 * Cố ý dùng hết hạn mức của IP local trong 15 phút, nên chỉ chạy khi bật RUN_RATE_LIMIT=1 và chạy
 * SAU các test đặt lại mật khẩu khác. Dùng một email synthetic không tồn tại.
 */
test('W1-11: rate-limited reset requests show a retry-later message without crashing', async ({ page, request }, testInfo) => {
  test.skip(testInfo.project.name !== 'chromium', 'Run once per suite.');
  test.skip(process.env.RUN_RATE_LIMIT !== '1', 'Exhausts the local reset quota for 15 minutes; opt in with RUN_RATE_LIMIT=1.');
  test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));

  const email = syntheticEmail('reset-limit');
  let lastStatus = 0;
  for (let attempt = 0; attempt < 12 && lastStatus !== 429; attempt++) {
    await page.goto('/auth/forgot-password');
    await page.locator('#forgot-email').fill(email);
    const [response] = await Promise.all([
      page.waitForResponse((r) => r.url().endsWith('/api/auth/password-resets') && r.request().method() === 'POST'),
      page.getByRole('button', { name: /GỬI HƯỚNG DẪN|SEND INSTRUCTIONS/ }).click(),
    ]);
    lastStatus = response.status();
  }
  expect(lastStatus).toBe(429);
  await expect(page.locator('p[role="alert"]')).toContainText(/quá nhiều lần|Too many attempts/);
  await expect(page.locator('#forgot-email')).toBeVisible();
  expect(errors).toEqual([]);
});
