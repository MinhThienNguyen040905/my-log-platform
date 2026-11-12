import { test, expect } from '@playwright/test';
import { createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Dashboard "Phổ cảm xúc" follows the mood icons picked on entries; "Sắc thái trong bài viết" shows the
 * primary analysis tone as a share of entries. Synthetic dashboard payload served through a browser route.
 */
test.describe('dashboard emotion spectrum', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('picked moods and writing tones are shown separately with readable labels', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'dashboard-spectrum');
    await page.route('**/api/backend/dashboard?**', (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({
        range: '30d', timezone: 'Asia/Ho_Chi_Minh', from: '2026-10-14', to: '2026-11-12', timeline: [],
        topMoods: [{ code: 'neutral', count: 18, averageScore: 6 }, { code: 'happy', count: 10, averageScore: 8 }, { code: 'very-bad', count: 2, averageScore: 1.5 }],
        topEmotions: [{ code: 'CALM', count: 15, averageScore: 0.8 }, { code: 'JOY', count: 5, averageScore: 0.8 }],
        topTopics: [{ code: 'WORK', count: 4, averageScore: 0.9 }],
        currentJournalStreak: 3, longestJournalStreak: 7, journalEntryCount: 30, moodSampleSize: 30,
      }),
    }));
    await page.goto('/auth/login');
    await page.locator('#login-email').fill(account.email);
    await page.locator('#login-password').fill(account.password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });

    const moods = page.getByRole('region', { name: 'Phổ cảm xúc' });
    await expect(moods).toContainText('Cảm xúc bạn chọn khi viết mỗi bài.');
    await expect(moods.getByText('Bình thường', { exact: true })).toBeVisible();
    await expect(moods.getByText('Rất tệ', { exact: true })).toBeVisible();

    const tones = page.getByRole('region', { name: 'Sắc thái trong bài viết' });
    await expect(tones.getByText('Bình yên', { exact: true })).toBeVisible();
    await expect(tones.getByText('100%', { exact: true })).toBeVisible(); // axis runs to 100% (15/20 → 75%)

    await expect(page.getByText('Công việc', { exact: true })).toBeVisible();
    const main = await page.locator('main').innerText();
    expect(main).not.toMatch(/\b(CALM|JOY|WORK|neutral|very-bad)\b/);
  });
});
