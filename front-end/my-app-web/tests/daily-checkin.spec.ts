import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * W2 — check-in hằng ngày (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md mục 4).
 * Cần backend local + Mailpit; tự skip nếu thiếu. Tài khoản và ghi chú đều synthetic.
 * Hồ sơ synthetic dùng timezone Asia/Ho_Chi_Minh; trình duyệt chạy ở America/Los_Angeles để
 * chứng minh ngày check-in tính theo hồ sơ chứ không theo máy (W2-07).
 */
const PROFILE_TZ = 'Asia/Ho_Chi_Minh';
const localDate = (timezone: string, at = new Date()) =>
  new Intl.DateTimeFormat('en-CA', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(at);

test.use({ timezoneId: 'America/Los_Angeles' });

test.describe('daily check-in', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  async function login(page: Page, email: string, password: string) {
    await page.goto('/auth/login');
    await page.locator('#login-email').fill(email);
    await page.locator('#login-password').fill(password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
    await openCheckin(page);
  }

  /** On the home page the check-in opens in a dialog from the hero's check-in button. */
  async function openCheckin(page: Page) {
    const care = page.getByTestId('checkin-open');
    await expect(care).toHaveAttribute('aria-expanded', 'false');
    await care.click();
    await expect(care).toHaveAttribute('aria-expanded', 'true');
  }

  const card = (page: Page) => page.getByRole('region', { name: /Check-in hôm nay|Today’s check-in/ });

  test('W2-01..04/06/07/08: create, show, edit as one upsert, in the profile’s day', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'checkin');
    const expectedDate = localDate(PROFILE_TZ);
    const puts: { url: string; body: Record<string, unknown> }[] = [];
    let dashboardReads = 0;
    page.on('request', (r) => {
      if (r.method() === 'PUT' && r.url().includes('/api/backend/check-ins/')) puts.push({ url: r.url(), body: r.postDataJSON() });
      if (r.method() === 'GET' && /\/api\/backend\/dashboard\?/.test(r.url())) dashboardReads++;
    });

    // W2-01: a fresh account sees the empty form; the backend 404 is not shown as an error.
    await login(page, account.email, account.password);
    await expect(card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ })).toBeVisible();
    await expect(card(page).locator('[role="alert"]')).toHaveCount(0);

    // Saving without a mood is blocked on the client.
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).locator('[role="alert"]')).toContainText(/chọn cảm xúc|choose your mood/);
    expect(puts).toHaveLength(0);

    // W2-02 + W2-04 (boundary values) + W2-06 (moodCode) + W2-07 (date from the profile timezone).
    await card(page).getByRole('button', { name: 'Rất tệ', exact: true }).click();
    await card(page).locator('#checkin-stress').fill('10');
    await card(page).locator('#checkin-energy').fill('1');
    await card(page).locator('#checkin-sleep').fill('0');
    await card(page).locator('#checkin-note').fill('Synthetic check-in note');
    const readsBeforeSave = dashboardReads;
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).getByText(/Bạn đã check-in hôm nay|You have checked in today/)).toBeVisible();
    expect(puts).toHaveLength(1);
    expect(puts[0].url).toContain(`/check-ins/${expectedDate}`);
    expect(puts[0].body).toMatchObject({ timezone: PROFILE_TZ, moodCode: 'very-bad', moodScore: 1.5, stressScore: 10, energyScore: 1, sleepMinutes: 0, note: 'Synthetic check-in note' });
    // W2-08: the dashboard is refreshed without reloading the page.
    await expect.poll(() => dashboardReads).toBeGreaterThan(readsBeforeSave);

    // Persisted: a reload still shows the summary.
    await page.reload();
    await openCheckin(page);
    await expect(card(page).getByText(/Bạn đã check-in hôm nay|You have checked in today/)).toBeVisible();
    await expect(card(page).getByRole('listitem').filter({ hasText: /^Rất tệ$/ })).toBeVisible();

    // W2-03: editing the same day updates the same record (upsert), version goes up.
    const auth = { Authorization: `Bearer ${account.token}` };
    const before = await (await request.get(`${BACKEND_URL}/api/v1/check-ins/${expectedDate}`, { headers: auth })).json();
    await card(page).getByRole('button', { name: /^Sửa$|^Edit$/ }).click();
    await card(page).getByRole('button', { name: 'Vui', exact: true }).click();
    await card(page).locator('#checkin-sleep').fill('24');
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).getByText(/Bạn đã check-in hôm nay|You have checked in today/)).toBeVisible();
    await expect(card(page).getByRole('listitem').filter({ hasText: /^Vui$/ })).toBeVisible();
    const after = await (await request.get(`${BACKEND_URL}/api/v1/check-ins/${expectedDate}`, { headers: auth })).json();
    expect(after.id).toBe(before.id);
    expect(after.version).toBeGreaterThan(before.version);
    expect(after).toMatchObject({ moodCode: 'happy', sleepMinutes: 1440 });
    const list = await (await request.get(`${BACKEND_URL}/api/v1/check-ins?from=${expectedDate}&to=${expectedDate}`, { headers: auth })).json();
    expect(list).toHaveLength(1);
  });

  test('activities: pick, show, and keep ones saved by another client', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'checkin-activities');
    const date = localDate(PROFILE_TZ);
    const auth = { Authorization: `Bearer ${account.token}` };
    // Another client (e.g. mobile) saved a check-in with an activity the web catalogue does not list.
    const seeded = await request.put(`${BACKEND_URL}/api/v1/check-ins/${date}`, { headers: auth, data: {
      timezone: PROFILE_TZ, moodCode: 'neutral', moodScore: 6,
      activities: [{ code: 'gardening', durationMinutes: 40, intensity: 'LOW' }, { code: 'walking', durationMinutes: 30, intensity: 'LOW' }],
    } });
    expect(seeded.status()).toBe(200);
    // The backend keeps activities when a client does not send the field at all.
    const keep = await request.put(`${BACKEND_URL}/api/v1/check-ins/${date}`, { headers: auth, data: { timezone: PROFILE_TZ, moodCode: 'neutral', moodScore: 6 } });
    expect((await keep.json()).activities).toHaveLength(2);

    await login(page, account.email, account.password);
    await expect(card(page).getByRole('listitem').filter({ hasText: /^Đi bộ$/ })).toBeVisible();
    await card(page).getByRole('button', { name: /^Sửa$|^Edit$/ }).click();
    await expect(card(page).getByRole('button', { name: 'Đi bộ', exact: true })).toHaveAttribute('aria-pressed', 'true');
    await expect(card(page).getByRole('button', { name: 'gardening', exact: true })).toHaveAttribute('aria-pressed', 'true');
    await card(page).getByRole('button', { name: 'Đọc sách', exact: true }).click();
    await card(page).getByRole('button', { name: 'Đi bộ', exact: true }).click(); // deselect on purpose
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).getByRole('listitem').filter({ hasText: /^Đọc sách$/ })).toBeVisible();

    const after = await (await request.get(`${BACKEND_URL}/api/v1/check-ins/${date}`, { headers: auth })).json();
    const byCode = Object.fromEntries((after.activities as { code: string; durationMinutes: number | null }[]).map((a) => [a.code, a]));
    expect(Object.keys(byCode).sort()).toEqual(['gardening', 'reading']);
    expect(byCode.gardening.durationMinutes).toBe(40); // untouched details survive a web edit
  });

  test('W2-05: the backend rejects out-of-range values', async ({ request }) => {
    const account = await createOnboardedAccount(request, 'checkin-invalid');
    const auth = { Authorization: `Bearer ${account.token}` };
    const day = localDate(PROFILE_TZ);
    const put = (body: Record<string, unknown>) => request.put(`${BACKEND_URL}/api/v1/check-ins/${day}`, { headers: auth, data: { timezone: PROFILE_TZ, ...body } });
    expect((await put({ stressScore: 0 })).status()).toBe(400);
    expect((await put({ energyScore: 11 })).status()).toBe(400);
    expect((await put({ note: 'x'.repeat(4001) })).status()).toBe(400);
    expect((await put({ moodCode: 'very_bad' })).status()).toBe(400);
    expect((await put({ moodCode: 'very-bad', moodScore: 1.5 })).status()).toBe(200);
  });

  test('W2-09: a network failure keeps what the user typed', async ({ page, request, context }) => {
    const account = await createOnboardedAccount(request, 'checkin-offline');
    await login(page, account.email, account.password);
    await card(page).getByRole('button', { name: 'Buồn', exact: true }).click();
    await card(page).locator('#checkin-note').fill('Synthetic offline note');
    await context.setOffline(true);
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).locator('[role="alert"]')).toContainText(/Chưa lưu được|Could not save/);
    await expect(card(page).locator('#checkin-note')).toHaveValue('Synthetic offline note');
    await expect(card(page).getByRole('button', { name: 'Buồn', exact: true })).toHaveAttribute('aria-pressed', 'true');
    await context.setOffline(false);
    await card(page).getByRole('button', { name: /Lưu check-in|Save check-in/ }).click();
    await expect(card(page).getByText(/Bạn đã check-in hôm nay|You have checked in today/)).toBeVisible();
  });

  test('W2-10: saving a journal entry with metrics does not create a check-in', async ({ request }) => {
    const account = await createOnboardedAccount(request, 'checkin-journal');
    const auth = { Authorization: `Bearer ${account.token}` };
    const created = await request.post(`${BACKEND_URL}/api/v1/journal-entries`, {
      headers: { ...auth, 'Idempotency-Key': `pw-${Date.now()}` },
      data: {
        timezone: PROFILE_TZ, title: 'Synthetic entry', moodCode: 'happy', stressScore: 3, energyScore: 7, sleepMinutes: 420,
        contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Một ngày bình thường.' }] }] },
      },
    });
    expect(created.status()).toBe(201);
    const checkin = await request.get(`${BACKEND_URL}/api/v1/check-ins/${localDate(PROFILE_TZ)}`, { headers: auth });
    expect(checkin.status()).toBe(404);
  });
});
