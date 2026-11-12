import { test, expect } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * The history calendar marks every day with entries in the visible month from
 * GET /journal-entries/daily-counts, including days whose entries are not in the loaded list pages
 * (the list loads 5 entries per page). Synthetic entries only.
 */
test.use({ timezoneId: 'Asia/Ho_Chi_Minh' });

test.describe('calendar daily counts', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('marks older days beyond the first list page, with counts and mood colours', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'calendar-counts');
    const auth = { Authorization: `Bearer ${account.token}` };
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    // Seven entries on days 1–6 of the current month (two on day 1), 12:00 local = 05:00Z; never in the future.
    const plan: [number, string][] = [[1, 'sad'], [1, 'happy'], [2, 'neutral'], [3, 'neutral'], [4, 'happy'], [5, 'neutral'], [6, 'happy']]
      .filter(([day]) => new Date(Date.UTC(year, month, day as number, 5)) < now) as [number, string][];
    test.skip(plan.length < 7, 'Needs at least the 6th of the month to have passed.');
    for (const [i, [day, mood]] of plan.entries()) {
      const response = await request.post(`${BACKEND_URL}/api/v1/journal-entries`, {
        headers: { ...auth, 'Idempotency-Key': `pw-calendar-${Date.now()}-${i}` },
        data: { timezone: 'Asia/Ho_Chi_Minh', title: `Synthetic day ${day}`, moodCode: mood,
          occurredAt: new Date(Date.UTC(year, month, day, 5, i)).toISOString(),
          contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Synthetic.' }] }] } },
      });
      expect(response.status()).toBe(201);
    }

    await page.goto('/auth/login');
    await page.locator('#login-email').fill(account.email);
    await page.locator('#login-password').fill(account.password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
    const counts = page.waitForResponse((r) => r.url().includes('/api/journal-entries/daily-counts'));
    await page.goto('/history-calendar');
    expect((await counts).status()).toBe(200);

    const label = (day: number, n: number) => `${day}/${month + 1}/${year}: ${n} bài viết`;
    // Day 1 holds the two oldest entries: not in the first list page of 5, still marked with its count.
    await expect(page.getByRole('button', { name: label(1, 2), exact: true })).toBeVisible();
    for (const day of [2, 3, 4, 5, 6]) await expect(page.getByRole('button', { name: label(day, 1), exact: true })).toBeVisible();

    // Moods are always shown: day 1's badge takes the colour of its latest entry (happy), no toggle exists.
    await expect(page.getByLabel('Hiện cảm xúc và chỉ số')).toHaveCount(0);
    await expect(page.getByRole('button', { name: label(1, 2), exact: true }).locator('span[aria-hidden="true"]'))
      .toHaveClass(/bg-mood-calm-joy/);
  });
});
