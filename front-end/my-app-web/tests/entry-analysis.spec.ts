import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * W5 — "Gợi ý nhìn lại" trong chi tiết bài viết ở trang Lịch sử (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md).
 * Local không bật worker phân tích, nên các trạng thái thật quan sát được là đang chờ và bị chặn vì an toàn;
 * trạng thái đã phân tích / lỗi được giả lập bằng route của trình duyệt với dữ liệu synthetic.
 * Câu kích hoạt an toàn lấy đúng từ backend JournalCheckinIntegrationTest.
 */
const RAW_CODES = /NOT_REQUESTED|ANALYZING|ANALYZED|ANALYSIS_FAILED|ANALYSIS_OUTDATED|BLOCKED_BY_SAFETY|\bPENDING\b|sentimentScore|analysisId/;

test.describe('entry analysis panel', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  async function createEntry(request: APIRequestContext, token: string, title: string, text: string) {
    const response = await request.post(`${BACKEND_URL}/api/v1/journal-entries`, {
      headers: { Authorization: `Bearer ${token}`, 'Idempotency-Key': `pw-${Date.now()}-${Math.floor(Math.random() * 1e9)}` },
      data: { timezone: 'Asia/Ho_Chi_Minh', title, contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text }] }] } },
    });
    expect(response.status()).toBe(201);
    return (await response.json()) as { id: string; riskLevel: string; analysisStatus: string };
  }

  async function openFromHistory(page: Page, email: string, password: string, title: string) {
    await page.goto('/auth/login');
    await page.locator('#login-email').fill(email);
    await page.locator('#login-password').fill(password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
    await page.goto('/history-calendar');
    await page.locator('[data-open-entry]', { hasText: title }).first().click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('region', { name: 'Gợi ý nhìn lại' })).toBeVisible({ timeout: 15000 });
    return dialog.getByRole('region', { name: 'Gợi ý nhìn lại' });
  }

  test('waiting state from the real backend, in plain language', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'analysis-wait');
    const entry = await createEntry(request, account.token, 'Synthetic quiet day', 'Hôm nay mình đi dạo và uống trà.');
    expect(['PENDING', 'NOT_REQUESTED']).toContain(entry.analysisStatus);
    const panel = await openFromHistory(page, account.email, account.password, 'Synthetic quiet day');
    await expect(panel).toContainText(/Đang đọc trang viết của bạn|chưa được phân tích/);
    expect(await panel.innerText()).not.toMatch(RAW_CODES);
  });

  test('blocked by safety: no retry, safety resources reachable (real backend)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'analysis-blocked');
    const entry = await createEntry(request, account.token, 'Synthetic safety check', 'I want to die');
    expect(entry.riskLevel).toBe('HIGH');
    expect(entry.analysisStatus).toBe('BLOCKED_BY_SAFETY');
    const panel = await openFromHistory(page, account.email, account.password, 'Synthetic safety check');
    await expect(panel).toContainText('Trang này không có gợi ý tự động');
    await expect(panel.getByRole('button', { name: 'Phân tích lại' })).toHaveCount(0);
    expect(await panel.innerText()).not.toMatch(RAW_CODES);
    await panel.getByRole('button', { name: 'Xem hỗ trợ an toàn' }).click();
    await expect(page.getByRole('dialog', { name: /Bạn không phải đơn độc/ })).toBeVisible();
  });

  test('analyzed result shows reflection and labelled chips, never scores or IDs (simulated)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'analysis-done');
    const entry = await createEntry(request, account.token, 'Synthetic good day', 'Một ngày nhẹ nhàng.');
    await page.route(`**/api/backend/journal-entries/${entry.id}/analysis`, (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ entryId: entry.id, contentVersion: 1, status: 'ANALYZED', analysisId: '00000000-0000-7000-8000-000000000001',
        sentiment: 'POSITIVE', sentimentScore: 0.82, emotions: ['CALM', 'JOY', 'UNKNOWN_CODE'], topics: ['SELF_CARE'],
        reflection: 'Synthetic reflection: bạn đã dành thời gian cho bản thân.' }),
    }));
    const panel = await openFromHistory(page, account.email, account.password, 'Synthetic good day');
    await expect(panel).toContainText('Synthetic reflection: bạn đã dành thời gian cho bản thân.');
    await expect(panel.getByRole('list', { name: 'Cảm xúc' }).getByRole('listitem')).toHaveText(['Bình yên', 'Vui']);
    await expect(panel.getByRole('list', { name: 'Chủ đề' }).getByRole('listitem')).toHaveText(['Chăm sóc bản thân']);
    const text = await panel.innerText();
    expect(text).not.toMatch(/0[.,]82|00000000-0000|UNKNOWN_CODE|POSITIVE/);
  });

  test('failed analysis offers a retry that calls the backend (simulated)', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'analysis-failed');
    const entry = await createEntry(request, account.token, 'Synthetic failed analysis', 'Một ngày bình thường.');
    let status = 'ANALYSIS_FAILED';
    let retries = 0;
    await page.route(`**/api/backend/journal-entries/${entry.id}/analysis`, (route) => route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ entryId: entry.id, contentVersion: 1, status, analysisId: null, sentiment: null, sentimentScore: null, emotions: [], topics: [], reflection: null }),
    }));
    await page.route(`**/api/backend/journal-entries/${entry.id}/analysis:retry`, (route) => {
      retries++; status = 'PENDING';
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'PENDING' }) });
    });
    const panel = await openFromHistory(page, account.email, account.password, 'Synthetic failed analysis');
    await expect(panel).toContainText('Chưa tạo được gợi ý cho trang này.');
    await panel.getByRole('button', { name: 'Phân tích lại' }).click();
    await expect(panel).toContainText('Đang đọc trang viết của bạn');
    expect(retries).toBe(1);
  });
});
