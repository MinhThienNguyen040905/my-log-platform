import type { Page } from '@playwright/test';

/** Test account credentials, never logged or asserted on. Returns null if not configured. */
export function testAccount(): { email: string; password: string } | null {
  const email = process.env.PLAYWRIGHT_TEST_EMAIL;
  const password = process.env.PLAYWRIGHT_TEST_PASSWORD;
  if (!email || !password) return null;
  return { email, password };
}

/** Logs in through the real form, same flow as JOURNAL_NAV_PLAYWRIGHT_SCENARIOS.md. */
export async function loginAsTestAccount(page: Page): Promise<void> {
  const account = testAccount();
  if (!account) throw new Error('PLAYWRIGHT_TEST_EMAIL/PLAYWRIGHT_TEST_PASSWORD not set');
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(account.email);
  await page.locator('#login-password').fill(account.password);
  // Not matched by button copy: the CTA text is marketing copy ("VÀO KHÔNG GIAN VIẾT →"),
  // not a fixed "Đăng nhập" label, and changes independently of this test.
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/);
}
