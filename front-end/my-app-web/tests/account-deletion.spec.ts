import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * UC-23, Settings → "Dữ liệu & quyền riêng tư" → "Xóa tài khoản": a request signs the user out and schedules deletion
 * in 7 days; signing in again cancels it. Needs the local backend with MYLOG_ACCOUNT_DELETION_ENABLED=true, the web
 * dev server with NEXT_PUBLIC_ACCOUNT_DELETION_ENABLED=true, Mailpit, and ACCOUNT_DELETION_E2E=1. Synthetic account only.
 */
async function login(page: Page, email: string, password: string) {
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
}

test.describe('account deletion (UC-23)', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(process.env.ACCOUNT_DELETION_E2E !== '1', 'Set ACCOUNT_DELETION_E2E=1 with account deletion enabled on backend and web.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('wrong password keeps the account; a request signs out; signing in again cancels it', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'account-deletion');
    await login(page, account.email, account.password);
    await page.goto('/setting?tab=data');
    await page.getByTestId('delete-account-open').click();
    const dialog = page.getByRole('dialog', { name: 'Xóa tài khoản MyLog?' });
    const submit = dialog.getByRole('button', { name: 'Xóa tài khoản sau 7 ngày' });
    await expect(submit).toBeDisabled();
    await dialog.getByRole('checkbox').check();
    await dialog.getByLabel('Cụm từ xác nhận xóa tài khoản').fill('XÓA TÀI KHOẢN');
    await dialog.getByLabel('Mật khẩu xác nhận xóa tài khoản').fill('Wrong-password-123!');
    await submit.click();
    await expect(dialog.getByRole('alert')).toContainText('Mật khẩu không đúng.');
    expect((await request.get(`${BACKEND_URL}/api/v1/me`, { headers: { Authorization: `Bearer ${account.token}` } })).status()).toBe(200);

    await dialog.getByLabel('Mật khẩu xác nhận xóa tài khoản').fill(account.password);
    await submit.click();
    await expect(dialog.getByRole('status')).toContainText('Tài khoản sẽ bị xóa vào');
    // Every session was revoked, including the one used by the API helper.
    expect((await request.get(`${BACKEND_URL}/api/v1/me`, { headers: { Authorization: `Bearer ${account.token}` } })).status()).toBe(401);

    await dialog.getByRole('button', { name: 'Về trang đăng nhập' }).click();
    await login(page, account.email, account.password);
    await expect(page.getByText('Đã hủy yêu cầu xóa tài khoản')).toBeVisible();
  });
});
