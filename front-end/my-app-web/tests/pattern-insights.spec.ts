import { test, expect, type Page } from '@playwright/test';
import { createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Last-30-day pattern insights on /insight → "Nhịp sống & Thói quen".
 * A fresh account has no data, so the real backend shows the empty state; the filled state uses a
 * synthetic insight served through a browser route.
 */
async function openPatterns(page: Page, email: string, password: string) {
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
  await page.goto('/insight');
  await page.getByRole('button', { name: 'Nhịp sống & Thói quen' }).click();
  return page.getByRole('region', { name: 'Nhịp của bạn trong 30 ngày' });
}

test.describe('pattern insights panel', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('refresh on a new account keeps the friendly empty state (real backend)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'pattern-empty');
    const panel = await openPatterns(page, account.email, account.password);
    await expect(panel).toContainText('Chưa thấy nhịp nào đủ rõ');
    // The former "Mối liên hệ cảm xúc" card is merged into this panel, including its no-causation reminder.
    await expect(page.getByRole('heading', { name: 'Mối liên hệ cảm xúc' })).toHaveCount(0);
    await expect(panel).toContainText('Lời nhắn nhỏ:');
    const refreshed = page.waitForResponse((r) => r.url().includes('/api/backend/insights:refresh'));
    await panel.getByRole('button', { name: 'Làm mới' }).click();
    expect((await refreshed).status()).toBe(200);
    await expect(panel).toContainText('Chưa thấy nhịp nào đủ rõ');
  });

  test('shows wording, evidence and lets the user hide an insight', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'pattern-filled');
    let hidden = false;
    let patchBody = '';
    await page.route('**/api/backend/insights?**', (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ nextCursor: null, items: hidden ? [] : [{
        id: '00000000-0000-7000-8000-0000000000aa', type: 'WEEKDAY_PATTERN', from: '2026-10-13', to: '2026-11-11',
        timezone: 'Asia/Ho_Chi_Minh', direction: 'UP', strength: 0.5, sampleSize: 26, algorithmVersion: 'pattern-30d-v1',
        generatedBy: 'AI_ASSISTED',
        narrative: 'Synthetic: thứ Hai có vẻ là ngày nặng nhất tuần của bạn (6,4 so với 4,9).',
        evidence: [
          { source: 'CHECKIN', date: '2026-10-19', metric: 'STRESS_SCORE', value: 6.8 },
          { source: 'JOURNAL', date: '2026-10-26', metric: 'STRESS_SCORE', value: 6.1 },
        ],
      }] }),
    }));
    await page.route('**/api/backend/insights/*', (route) => {
      // Only the hide call; GET insights/mascot (the Insight-page mascot) goes to the backend.
      if (route.request().method() !== 'PATCH') return route.fallback();
      patchBody = route.request().postData() ?? '';
      hidden = true;
      return route.fulfill({ status: 204 });
    });
    const panel = await openPatterns(page, account.email, account.password);
    await expect(panel).toContainText('Synthetic: thứ Hai có vẻ là ngày nặng nhất tuần');
    await expect(panel).toContainText('Nhịp trong tuần');
    await expect(panel).toContainText('AI diễn đạt lại');
    await expect(panel).toContainText('Dựa trên 26 ngày có dữ liệu');
    expect(await panel.innerText()).not.toMatch(/WEEKDAY_PATTERN|AI_ASSISTED|pattern-30d/);

    await panel.getByRole('button', { name: 'Xem căn cứ' }).click();
    await expect(panel.getByRole('img', { name: /2026-10-19: 6.8/ })).toBeVisible();

    await panel.getByRole('button', { name: 'Ẩn nhận định này' }).click();
    await expect(panel).toContainText('Chưa thấy nhịp nào đủ rõ');
    expect(JSON.parse(patchBody)).toEqual({ status: 'HIDDEN' });
  });
});
