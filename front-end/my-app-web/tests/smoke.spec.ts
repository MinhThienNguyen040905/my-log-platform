import { test, expect } from '@playwright/test';

/**
 * No backend, no auth — only the public pages. This proves the Playwright harness (browser,
 * dev server, config) actually works end to end; everything behind `app/(app)/` needs a running
 * backend and a seeded test account, see tests/README.md.
 */
test.describe('smoke', () => {
  test('landing page renders', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/.+/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('login page has the fields the other specs rely on', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('#login-email')).toBeVisible();
    await expect(page.locator('#login-password')).toBeVisible();
  });

  test('(app) routes redirect to login without a session', async ({ page }) => {
    await page.goto('/journal-editor');
    await expect(page).toHaveURL(/\/auth\/login/);
  });
});
