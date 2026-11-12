import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Weekly/monthly reflection ("Nhìn lại"): on-demand creation from /insight and the detail page.
 * The real-backend test needs MYLOG_REPORTS_ENABLED=true so the worker builds the report; the detail
 * test serves a synthetic report through a browser route.
 */
async function login(page: Page, email: string, password: string) {
  await page.goto('/auth/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
}

const REPORT_ID = '00000000-0000-7000-8000-0000000000bb';
const ENTRY_ID = '00000000-0000-7000-8000-0000000000cc';

test.describe('reflection reports', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('create last week’s reflection on demand (real backend)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'report-create');
    // /insight needs at least 3 journal days before it shows reflections; write them in last week (Mon–Wed).
    const today = new Date();
    const monday = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() - ((today.getUTCDay() + 6) % 7) - 7, 13));
    for (let i = 0; i < 3; i++) {
      const at = new Date(monday.getTime() + i * 86400000).toISOString();
      const response = await request.post(`${BACKEND_URL}/api/v1/journal-entries`, {
        headers: { Authorization: `Bearer ${account.token}`, 'Idempotency-Key': `pw-report-${Date.now()}-${i}` },
        data: { timezone: 'Asia/Ho_Chi_Minh', occurredAt: at, title: `Synthetic report day ${i}`, moodScore: 6 + i,
          contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Synthetic quiet day.' }] }] } },
      });
      expect(response.status()).toBe(201);
    }
    await login(page, account.email, account.password);
    await page.goto('/insight');
    const created = page.waitForResponse((r) => r.request().method() === 'POST' && /\/api\/backend\/reports$/.test(r.url()));
    await page.getByRole('button', { name: 'Tạo bản nhìn lại tuần trước' }).click();
    expect([200, 202]).toContain((await created).status());
    await page.waitForURL(/\/reports\/[0-9a-f-]+\/print/, { timeout: 15000 });
    // The worker polls every few seconds; the page refetches until the report is ready.
    await expect(page.getByText(/Tuần này bạn viết nhật ký 3\/7 ngày/)).toBeVisible({ timeout: 30000 });
    await expect(page.getByRole('region', { name: 'Ngày nổi bật' })).toContainText('Mở bài "Synthetic report day 2"');
    await expect(page.getByRole('heading', { name: 'Tuần của bạn' })).toBeVisible();
  });

  test('detail page shows comparison, highlight days with titles and tones', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'report-detail');
    let regenerated = 0;
    await page.route(`**/api/backend/reports/${REPORT_ID}`, (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({
        id: REPORT_ID, type: 'WEEKLY', from: '2026-11-02', to: '2026-11-08', timezone: 'Asia/Ho_Chi_Minh',
        version: 1, status: 'READY', sampleSize: 7, generatedBy: 'RULE',
        narrative: 'Synthetic: Tuần này bạn viết nhật ký 7/7 ngày. Điểm tâm trạng trung bình là 6,9/10, nhích lên so với tuần trước (6,4).',
        metrics: {
          journalDays: 7, periodDays: 7, longestJournalStreak: 7, averageMood: 6.87, averageStress: 4.9, averageEnergy: 5.6, averageSleepMinutes: 430,
          previous: { averageMood: 6.43, averageStress: 5.6, averageEnergy: 5.5, averageSleepMinutes: 425 },
          bestDay: { date: '2026-11-07', moodScore: 8.6, entryId: ENTRY_ID },
          lowestDay: { date: '2026-11-03', moodScore: 4.9 },
          topEmotions: [{ code: 'CALM', count: 5, previousCount: 2 }], topTopics: [{ code: 'WORK', count: 4, previousCount: 4 }],
        },
        entryTitles: { [ENTRY_ID]: 'Synthetic: cuối tuần thong thả' },
        evidence: [{ source: 'CHECKIN', date: '2026-11-07', metric: 'MOOD_SCORE', value: 8.6 }],
        createdAt: '2026-11-11T10:00:00Z', completedAt: '2026-11-11T10:00:05Z',
      }),
    }));
    await page.route(`**/api/backend/reports/${REPORT_ID}:regenerate`, (route) => {
      regenerated++;
      return route.fulfill({ status: 202, contentType: 'application/json', body: JSON.stringify({ id: REPORT_ID, status: 'PENDING' }) });
    });
    await login(page, account.email, account.password);
    await page.goto(`/reports/${REPORT_ID}/print`);

    await expect(page.getByText('Synthetic: Tuần này bạn viết nhật ký 7/7 ngày.')).toBeVisible();
    await expect(page.getByText('tuần trước 6,4')).toBeVisible();
    await expect(page.getByText('Viết nhật ký 7/7 ngày · chuỗi dài nhất 7 ngày')).toBeVisible();
    const highlights = page.getByRole('region', { name: 'Ngày nổi bật' });
    await expect(highlights).toContainText('Ngày dễ chịu nhất');
    await expect(highlights.getByRole('link', { name: 'Mở bài "Synthetic: cuối tuần thong thả"' }))
      .toHaveAttribute('href', `/journal-editor?id=${ENTRY_ID}`);
    await expect(highlights).toContainText('Ngày nặng nề nhất');
    await expect(page.getByRole('region', { name: 'Sắc thái trong bài viết' })).toContainText('Bình yên × 5');
    expect(await page.locator('article').innerText()).not.toMatch(/CALM|WORK|bestDay|entryId/);

    await page.getByRole('button', { name: 'Tạo lại với dữ liệu mới' }).click();
    await expect.poll(() => regenerated).toBe(1);
  });
});
