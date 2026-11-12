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

  test('last week’s reflection appears in full on the tab without any click (real backend)', async ({ page, request }) => {
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
    let creates = 0;
    page.on('request', (r) => { if (r.method() === 'POST' && /\/api\/backend\/reports$/.test(r.url())) creates++; });
    await page.goto('/insight');
    // No button: the tab asks for last week's reflection by itself and stays on /insight while it is prepared.
    await expect(page.getByRole('button', { name: 'Tạo bản nhìn lại tuần trước' })).toHaveCount(0);
    const latest = page.getByRole('article', { name: /^Tuần · / });
    await expect(latest.getByText(/Tuần này bạn viết nhật ký 3\/7 ngày/)).toBeVisible({ timeout: 30000 });
    await expect(latest.getByRole('heading', { name: 'Tuần của bạn' })).toBeVisible();
    await expect(latest.getByRole('region', { name: 'Ngày nổi bật' })).toContainText('Mở bài "Synthetic report day 2"');
    expect(page.url()).toMatch(/\/insight$/);

    // Reopening the tab does not ask again: the week already has its reflection.
    await page.reload();
    await expect(latest.getByText(/Tuần này bạn viết nhật ký 3\/7 ngày/)).toBeVisible({ timeout: 15000 });
    expect(creates).toBe(1);
  });

  test('earlier periods list only the 4 previous weeks or the previous month', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'report-older');
    const report = (type: string, i: number) => ({
      id: `00000000-0000-7000-8000-${String(i).padStart(12, '0')}`, type, from: `2026-${type === 'WEEKLY' ? '10' : String(10 - i).padStart(2, '0')}-${String(i + 1).padStart(2, '0')}`,
      to: '2026-11-08', timezone: 'Asia/Ho_Chi_Minh', version: 1, status: 'READY', sampleSize: 7, generatedBy: 'RULE',
      narrative: `Synthetic ${type} ${i}`, metrics: {}, evidence: [], entryTitles: {}, createdAt: '2026-11-11T00:00:00Z', completedAt: '2026-11-11T00:00:00Z',
    });
    await page.route('**/api/backend/reports?**', (route) => {
      const type = new URL(route.request().url()).searchParams.get('type') ?? 'WEEKLY';
      const count = type === 'WEEKLY' ? 7 : 3;
      return route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify({ nextCursor: null, items: Array.from({ length: count }, (_, i) => report(type, i)) }) });
    });
    await page.route('**/api/backend/reports/0000*', (route) => route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify(report('WEEKLY', 0)) }));
    await page.route('**/api/backend/dashboard?**', (route) => route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify({ range: '90d', timeline: [1, 2, 3].map((d) => ({ date: `2026-11-0${d}`, journalCount: 1 })), topMoods: [], topEmotions: [], topTopics: [] }) }));
    await page.route('**/api/backend/reports', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(report('WEEKLY', 0)) }));
    await login(page, account.email, account.password);
    await page.goto('/insight');

    const older = page.getByRole('region', { name: 'Các kỳ trước' });
    await expect(older.getByRole('article')).toHaveCount(4);
    await expect(older).toContainText('Synthetic WEEKLY 4');
    await expect(older).not.toContainText('Synthetic WEEKLY 5');

    await page.getByRole('button', { name: 'Tháng', exact: true }).click();
    await expect(older.getByRole('article')).toHaveCount(1);
    await expect(older).toContainText('Synthetic MONTHLY 1');
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
    let allowance = { available: true, availableFrom: '2026-11-12' };
    await page.route('**/api/backend/reports/regeneration', (route) => route.fulfill({
      status: 200, contentType: 'application/json', body: JSON.stringify(allowance),
    }));
    await page.route(`**/api/backend/reports/${REPORT_ID}:regenerate`, (route) => {
      allowance = { available: false, availableFrom: '2026-11-16' };
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

    // Once used, the button stays locked for the rest of the week and says when it opens again.
    await page.goto(`/reports/${REPORT_ID}/print`);
    await expect(page.getByRole('button', { name: 'Tạo lại với dữ liệu mới' })).toBeDisabled();
    await expect(page.getByText(/Tuần này bạn đã dùng lượt tạo lại\. Bạn có thể tạo lại từ .*16\/11/)).toBeVisible();
    expect(regenerated).toBe(1);
  });
});
