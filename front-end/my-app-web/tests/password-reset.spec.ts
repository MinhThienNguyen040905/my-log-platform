import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createVerifiedAccount, extractCode, localStackAvailable, syntheticEmail, waitForEmail } from './helpers/mailpit';

/**
 * W1 — quên / đặt lại mật khẩu (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md mục 4).
 * Cần backend local + Mailpit; tự skip nếu thiếu. Chỉ chạy trên project `chromium` vì backend giới
 * hạn 10 yêu cầu đặt lại / 15 phút / IP — file này dùng 6 yêu cầu mỗi lần chạy. W1-11 (vượt giới hạn)
 * nằm riêng trong password-reset-rate-limit.spec.ts vì nó cố ý dùng hết hạn mức.
 * Mọi tài khoản và mật khẩu đều synthetic (`qa+…@example.test`).
 */
const SUBMIT = /ĐẶT MẬT KHẨU MỚI|SET NEW PASSWORD/;
const INVALID = /không hợp lệ hoặc đã hết hạn|invalid or expired/;
const formAlert = (page: Page) => page.locator('p[role="alert"]');

test.describe('password reset', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Rate-limited backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  /** Submits the forgot form; returns the HTTP status of the BFF call and the on-screen message. */
  async function requestReset(page: Page, email: string) {
    await page.goto('/auth/forgot-password');
    await page.locator('#forgot-email').fill(email);
    const [response] = await Promise.all([
      page.waitForResponse((r) => r.url().endsWith('/api/auth/password-resets') && r.request().method() === 'POST'),
      page.getByRole('button', { name: /GỬI HƯỚNG DẪN|SEND INSTRUCTIONS/ }).click(),
    ]);
    await expect(page.getByRole('status')).toBeVisible();
    return { status: response.status(), message: await page.getByRole('status').innerText() };
  }

  async function resetWithCode(page: Page, email: string, code: string, password: string) {
    await page.goto('/auth/reset-password');
    await page.locator('#reset-email').fill(email);
    await page.locator('#reset-code').fill(code);
    await page.locator('#reset-code-newPassword').fill(password);
    await page.locator('#reset-code-confirmPassword').fill(password);
    await page.getByRole('button', { name: SUBMIT }).click();
  }

  async function canLogin(page: Page, email: string, password: string) {
    const response = await page.request.post(`${BACKEND_URL}/api/v1/auth/login`, { data: { email, password } });
    return response.ok();
  }

  test('W1-01/02: registered and unknown emails get the same status and message; only the registered one gets mail', async ({ page, request }) => {
    const account = await createVerifiedAccount(request, 'reset-enum');
    const unknown = syntheticEmail('reset-unknown');
    const known = await requestReset(page, account.email);
    const missing = await requestReset(page, unknown);
    expect(known.status).toBe(202);
    expect(missing.status).toBe(known.status);
    expect(missing.message).toBe(known.message);
    const text = await waitForEmail(request, account.email, 'Đặt lại mật khẩu');
    expect(text).toMatch(/#token=[\w-]+/);
    expect(text).toMatch(/\b\d{8}\b/);
    await expect(waitForEmail(request, unknown, 'Đặt lại mật khẩu', 3000)).rejects.toThrow();
  });

  test('W1-03/04/07/09/14: reset through the email link', async ({ page, request }) => {
    const account = await createVerifiedAccount(request, 'reset-link');
    await requestReset(page, account.email);
    const text = await waitForEmail(request, account.email, 'Đặt lại mật khẩu');
    const link = new URL(text.match(/(https?:\/\/\S+#token=[\w-]+)/)![1]);

    const response = await page.goto(link.pathname + link.hash);
    // W1-14: the reset page must not leak the token through Referer and must not be cached.
    expect(response?.headers()['referrer-policy']).toBe('no-referrer');
    // `next dev` forces `no-cache, must-revalidate` on HTML pages; the configured `no-store` only
    // shows on a production build (`next build && next start`). Both prevent reuse of a cached copy.
    expect(response?.headers()['cache-control']).toMatch(/no-store|no-cache/);
    // W1-03: the token is removed from the address bar once read; the link form is shown.
    await expect.poll(() => page.url()).not.toContain('token=');
    await expect(page.locator('#reset-link-newPassword')).toBeVisible();

    // W1-09: same password as before is rejected with its own message; the link stays usable.
    await page.locator('#reset-link-newPassword').fill(account.password);
    await page.locator('#reset-link-confirmPassword').fill(account.password);
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(formAlert(page)).toContainText(/khác mật khẩu hiện tại|different from the current/);

    // W1-04: a new password works, the old one no longer does.
    const newPassword = `${account.password}-New`;
    await page.locator('#reset-link-newPassword').fill(newPassword);
    await page.locator('#reset-link-confirmPassword').fill(newPassword);
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(page).toHaveURL(/\/auth\/login/);
    expect(await canLogin(page, account.email, newPassword)).toBe(true);
    expect(await canLogin(page, account.email, account.password)).toBe(false);

    // W1-07: the same link cannot be used twice, and the page offers a new request.
    await page.goto(link.pathname + link.hash);
    const another = `${newPassword}-Again`;
    await page.locator('#reset-link-newPassword').fill(another);
    await page.locator('#reset-link-confirmPassword').fill(another);
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(formAlert(page)).toContainText(INVALID);
    await expect(page.getByRole('link', { name: /Gửi yêu cầu mới|Request a new one/ })).toBeVisible();
  });

  test('W1-05/06/10: reset with email + 8-digit code', async ({ page, request }) => {
    const account = await createVerifiedAccount(request, 'reset-code');
    await requestReset(page, account.email);
    const code = extractCode(await waitForEmail(request, account.email, 'Đặt lại mật khẩu'));

    await page.getByRole('link', { name: /Tôi đã có mã|I already have/ }).click();
    await expect(page).toHaveURL(/\/auth\/reset-password$/);
    // The email typed on the previous page is carried over without going through the URL.
    await expect(page.locator('#reset-email')).toHaveValue(account.email);

    // W1-10 + W1-06 (short code): client-side validation blocks the submit — no request is sent.
    let confirmCalls = 0;
    page.on('request', (r) => { if (r.url().includes('/api/auth/password-resets/confirm')) confirmCalls++; });
    await page.locator('#reset-code').fill(code.slice(0, 6));
    await page.locator('#reset-code-newPassword').fill('short');
    await page.locator('#reset-code-confirmPassword').fill('different');
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(page.locator('#reset-code-error')).toBeVisible();
    await expect(page.locator('#reset-code-newPassword-error')).toBeVisible();
    await expect(page.locator('#reset-code-confirmPassword-error')).toBeVisible();
    expect(confirmCalls).toBe(0);

    // W1-06: a wrong 8-digit code gets a generic invalid/expired message.
    const wrong = code === '00000000' ? '11111111' : '00000000';
    const newPassword = `${account.password}-Code`;
    await page.locator('#reset-code').fill(wrong);
    await page.locator('#reset-code-newPassword').fill(newPassword);
    await page.locator('#reset-code-confirmPassword').fill(newPassword);
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(formAlert(page)).toContainText(INVALID);
    expect(confirmCalls).toBe(1);

    // W1-05: the right code resets the password — pasted with a space, as people copy it from mail.
    await page.locator('#reset-code').fill(`${code.slice(0, 4)} ${code.slice(4)}`);
    await page.getByRole('button', { name: SUBMIT }).click();
    await expect(page).toHaveURL(/\/auth\/login/);
    expect(await canLogin(page, account.email, newPassword)).toBe(true);
  });

  test('W1-08: a second request invalidates the first code', async ({ page, request }) => {
    const account = await createVerifiedAccount(request, 'reset-twice');
    await requestReset(page, account.email);
    const first = extractCode(await waitForEmail(request, account.email, 'Đặt lại mật khẩu'));
    await requestReset(page, account.email);
    let second = first;
    for (let attempt = 0; attempt < 20 && second === first; attempt++) {
      second = extractCode(await waitForEmail(request, account.email, 'Đặt lại mật khẩu'));
      if (second === first) await page.waitForTimeout(500);
    }
    expect(second, 'second reset email carries a new code').not.toBe(first);

    const newPassword = `${account.password}-Twice`;
    await resetWithCode(page, account.email, first, newPassword);
    await expect(formAlert(page)).toContainText(INVALID);
    await resetWithCode(page, account.email, second, newPassword);
    await expect(page).toHaveURL(/\/auth\/login/);
  });

  test('W1-12: a browser that was signed in is signed out after the reset', async ({ page, request, browser }) => {
    const account = await createVerifiedAccount(request, 'reset-session');
    const other = await browser.newContext();
    const otherPage = await other.newPage();
    await otherPage.goto('/auth/login');
    await otherPage.locator('#login-email').fill(account.email);
    await otherPage.locator('#login-password').fill(account.password);
    await otherPage.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await expect(otherPage).not.toHaveURL(/\/auth\/login/, { timeout: 15000 });

    await requestReset(page, account.email);
    const code = extractCode(await waitForEmail(request, account.email, 'Đặt lại mật khẩu'));
    await resetWithCode(page, account.email, code, `${account.password}-Session`);
    await expect(page).toHaveURL(/\/auth\/login/);

    await otherPage.goto('/dashboard');
    await expect(otherPage).toHaveURL(/\/auth\/login/, { timeout: 15000 });
    await other.close();
  });

  test('W1-15/16: English copy and keyboard order on both pages (no backend request)', async ({ page, context }) => {
    await context.addCookies([{ name: 'mylog_locale', value: 'en', domain: 'localhost', path: '/' }]);
    for (const [path, heading] of [['/auth/forgot-password', 'Forgot password'], ['/auth/reset-password', 'Reset password']] as const) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 })).toHaveText(heading);
      const text = await page.locator('main').innerText();
      expect(text, `no raw i18n keys on ${path}`).not.toMatch(/\bauth\.[a-zA-Z]+/);
      expect(text, `no Vietnamese copy left on ${path}`).not.toMatch(/[ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]/i);
    }

    // W1-16: Tab reaches the email field before the submit button; errors are announced as alerts.
    await page.goto('/auth/forgot-password');
    const focusOrder: string[] = [];
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press('Tab');
      focusOrder.push(await page.evaluate(() => document.activeElement?.id || document.activeElement?.tagName || ''));
    }
    const emailAt = focusOrder.indexOf('forgot-email');
    expect(emailAt, `focus order: ${focusOrder.join(' > ')}`).toBeGreaterThanOrEqual(0);
    expect(focusOrder[emailAt + 1]).toBe('BUTTON');
    await page.locator('#forgot-email').fill('not-an-email');
    await page.locator('#forgot-email').blur();
    await expect(page.locator('#forgot-email-error')).toHaveAttribute('role', 'alert');
  });
});
