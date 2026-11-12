import { test, expect, type Page } from '@playwright/test';
import { createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Home-page mascot: it wears the mood picked most in the last 7 days (synthetic summary served through a
 * browser route), and the choice in Settings is saved to the real profile.
 */
async function login(page: Page, email: string, password: string) {
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
}

function mockMascot(page: Page, body: object) {
  return page.route('**/api/backend/insights/mascot', (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify(body),
  }));
}

test.describe('insight mascot', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('new account: the default mascot waits for a mood (real backend)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'mascot-empty');
    await login(page, account.email, account.password);
    const response = page.waitForResponse((r) => r.url().includes('/api/backend/insights/mascot'));
    await page.goto('/dashboard');
    expect((await response).status()).toBe(200);
    const card = page.getByTestId('mascot-card');
    await expect(card).toContainText('Mầm Xanh cùng bạn nhìn lại 7 ngày');
    await expect(card).toContainText('Chưa có bài nào chọn cảm xúc trong 7 ngày qua.');
    await expect(card.getByRole('img', { name: 'Linh vật Mầm Xanh đang chờ bạn viết' })).toHaveAttribute('data-mood', 'idle');
  });

  test('mirrors the most picked mood and follows the mascot chosen in settings', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'mascot-pick');
    await mockMascot(page, { mood: 'sad', moodCount: 3, entryCount: 5, from: '2026-11-06', to: '2026-11-12', calm: false });
    await login(page, account.email, account.password);

    await page.goto('/setting');
    const picker = page.getByTestId('mascot-picker');
    const saved = page.waitForResponse((r) => r.url().includes('/api/me') && r.request().method() === 'PATCH');
    await picker.getByRole('radio', { name: /Mèo Cam/ }).click();
    expect((await saved).status()).toBe(200);
    await expect(picker.getByRole('radio', { name: /Mèo Cam/ })).toHaveAttribute('aria-checked', 'true');
    await picker.getByRole('button', { name: /Thử biểu cảm/ }).click();
    await expect(picker.getByRole('img', { name: 'Linh vật Mèo Cam đang bình thường' })).toBeVisible();

    await page.goto('/dashboard');
    const card = page.getByTestId('mascot-card');
    await expect(card).toContainText('7 ngày qua bạn thường thấy: buồn · 3/5 bài');
    await expect(card.getByRole('img', { name: 'Linh vật Mèo Cam đang buồn' })).toHaveAttribute('data-mascot', 'cat');

    // The choice is stored server-side: a reload keeps it.
    await page.reload();
    await expect(page.getByTestId('mascot-card').getByRole('img', { name: /Mèo Cam/ })).toBeVisible();
  });

  test('"very bad" week links to calming tools; a high-risk week stays gentle', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'mascot-calm');
    let calm = false;
    await page.route('**/api/backend/insights/mascot', (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ mood: 'very-bad', moodCount: 4, entryCount: 4, from: '2026-11-06', to: '2026-11-12', calm }),
    }));
    await login(page, account.email, account.password);
    const card = page.getByTestId('mascot-card');
    await expect(card).toContainText('thử vài nhịp thở chậm');
    await expect(card.getByRole('link', { name: /công cụ thư giãn/ })).toHaveAttribute('href', '/journal-editor');

    calm = true;
    await page.reload();
    await expect(page.getByTestId('mascot-card')).toContainText('tìm đến người tin cậy hoặc nguồn hỗ trợ');
  });

  test('the home check-in button opens today’s check-in; its icon follows the mascot', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'mascot-care');
    await mockMascot(page, { mood: 'happy', moodCount: 2, entryCount: 2, from: '2026-11-06', to: '2026-11-12', calm: false });
    await login(page, account.email, account.password);
    const open = page.getByTestId('checkin-open');
    const checkin = page.getByRole('region', { name: 'Check-in hôm nay' });
    await expect(open).toHaveText(/Check-in hôm nay/);
    await expect(open.locator('svg.lucide-droplets')).toHaveCount(1);
    await expect(checkin).toHaveCount(0);
    await open.click();
    await expect(open).toHaveAttribute('aria-expanded', 'true');
    await expect(page.getByRole('dialog', { name: 'Check-in hôm nay' })).toBeVisible();
    await expect(checkin.getByRole('button', { name: 'Lưu check-in' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(checkin).toHaveCount(0);
    await expect(open).toHaveAttribute('aria-expanded', 'false');
    await open.click();
    await page.getByRole('dialog').getByRole('button', { name: 'Đóng' }).click();
    await expect(checkin).toHaveCount(0);

    for (const [name, icon] of [['Mèo Cam', 'lucide-fish'], ['Ly Cà Phê', 'lucide-bean']]) {
      await page.goto('/setting');
      const saved = page.waitForResponse((r) => r.url().includes('/api/me') && r.request().method() === 'PATCH');
      await page.getByTestId('mascot-picker').getByRole('radio', { name: new RegExp(name) }).click();
      expect((await saved).status()).toBe(200);
      await page.goto('/dashboard');
      await expect(page.getByTestId('checkin-open').locator(`svg.${icon}`)).toHaveCount(1);
    }
  });

  test('"Ẩn linh vật" removes the mascot but keeps the check-in button', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'mascot-off');
    await mockMascot(page, { mood: 'happy', moodCount: 2, entryCount: 2, from: '2026-11-06', to: '2026-11-12', calm: false });
    await login(page, account.email, account.password);
    await page.goto('/setting');
    const saved = page.waitForResponse((r) => r.url().includes('/api/me') && r.request().method() === 'PATCH');
    await page.getByTestId('mascot-picker').getByRole('radio', { name: 'Ẩn linh vật' }).click();
    expect((await saved).status()).toBe(200);
    await page.goto('/dashboard');
    await expect(page.getByTestId('checkin-open')).toBeVisible();
    await expect(page.getByTestId('mascot-card')).toHaveCount(0);
  });
});
