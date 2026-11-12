import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Settings → "Dữ liệu & quyền riêng tư" → "Bắt đầu hành trình mới": deletes the user's journal data for good and keeps
 * the account. Needs the local backend and Mailpit; the account and its content are synthetic.
 */
const TZ = 'Asia/Ho_Chi_Minh';
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());

async function login(page: Page, email: string, password: string) {
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
}

test.describe('start a new journey (data reset)', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('wrong password keeps everything; confirming deletes the journal and keeps the account', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'data-reset');
    const auth = { Authorization: `Bearer ${account.token}` };
    const created = await request.post(`${BACKEND_URL}/api/v1/journal-entries`, {
      headers: { ...auth, 'Idempotency-Key': `pw-reset-${Date.now()}` },
      data: {
        timezone: TZ, title: 'Synthetic entry', moodCode: 'happy',
        contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Một ngày bình thường.' }] }] },
      },
    });
    expect(created.status()).toBe(201);
    expect((await request.put(`${BACKEND_URL}/api/v1/check-ins/${today()}`, { headers: auth, data: { timezone: TZ, moodCode: 'neutral', moodScore: 6 } })).status()).toBe(200);

    await login(page, account.email, account.password);
    await page.goto('/setting?tab=data');
    await page.getByTestId('start-over-open').click();
    const dialog = page.getByRole('dialog', { name: 'Xóa toàn bộ dữ liệu nhật ký?' });
    const submit = dialog.getByRole('button', { name: 'Xóa vĩnh viễn và bắt đầu lại' });
    await expect(submit).toBeDisabled();
    await dialog.getByRole('checkbox').check();
    await dialog.getByLabel('Cụm từ xác nhận').fill('BẮT ĐẦU LẠI');
    await dialog.getByLabel('Mật khẩu xác nhận xóa dữ liệu').fill('Wrong-password-123!');
    await submit.click();
    await expect(dialog.getByRole('alert')).toContainText('Mật khẩu không đúng.');
    // A wrong password neither deletes data nor signs the user out.
    expect((await (await request.get(`${BACKEND_URL}/api/v1/journal-entries?limit=5`, { headers: auth })).json()).items).toHaveLength(1);

    await dialog.getByLabel('Mật khẩu xác nhận xóa dữ liệu').fill(account.password);
    await submit.click();
    await expect(dialog.getByRole('status')).toContainText('Đã xóa xong');
    await page.waitForURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByTestId('insight-unlock-hint')).toContainText('Viết thêm 3 ngày');

    expect((await (await request.get(`${BACKEND_URL}/api/v1/journal-entries?limit=5`, { headers: auth })).json()).items).toHaveLength(0);
    expect((await request.get(`${BACKEND_URL}/api/v1/check-ins/${today()}`, { headers: auth })).status()).toBe(404);
    const me = await request.get(`${BACKEND_URL}/api/v1/me`, { headers: auth });
    expect(me.status()).toBe(200);
    expect((await me.json()).email).toBe(account.email);
  });
});
