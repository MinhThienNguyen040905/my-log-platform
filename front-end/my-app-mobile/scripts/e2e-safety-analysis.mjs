// M4 + M5 — kết quả phân tích và màn hỗ trợ an toàn trên Expo web với backend local thật
// (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md mục 5). Chỉ dùng tài khoản và nội dung synthetic; câu kích
// hoạt safety lấy đúng từ JournalCheckinIntegrationTest của backend, không viết câu mới.
// Cần: backend local, Mailpit, Expo web đang chạy với EXPO_PUBLIC_API_BASE_URL trỏ tới backend.
// Đặt CORS_SHIM=1 nếu Expo web không chạy ở http://localhost:3000 (origin duy nhất backend local cho phép).
import { chromium } from 'playwright-core';
import { randomBytes, randomUUID } from 'node:crypto';

const appUrl = process.env.EXPO_WEB_URL ?? 'http://localhost:8081';
const backend = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';
const mailpit = process.env.MAILPIT_URL ?? 'http://127.0.0.1:8025';
const browserPath = process.env.BROWSER_PATH ?? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const email = `mobile-safety-${randomUUID()}@example.invalid`;
const password = `Synthetic-${randomBytes(12).toString('hex')}!`;
const SAFETY_TRIGGER = 'I want to die'; // backend/src/test/java/com/mylog/journal/JournalCheckinIntegrationTest.java
const RAW_STATUS = /NOT_REQUESTED|ANALYZING|ANALYZED|ANALYSIS_FAILED|ANALYSIS_OUTDATED|BLOCKED_BY_SAFETY|\bPENDING\b|\bHIGH\b|\bCRITICAL\b/;
const PHONE = /\+?\d[\d\s().-]{6,}\d/;
const passed = [];
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function post(path, body, token) {
  return fetch(`${backend}/api/v1${path}`, { method: 'POST', body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) } });
}
async function verificationCode() {
  for (let attempt = 0; attempt < 30; attempt++) {
    await wait(500);
    const listing = await (await fetch(`${mailpit}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`)).json();
    const message = listing.messages?.[0];
    if (!message) continue;
    const detail = await (await fetch(`${mailpit}/api/v1/message/${encodeURIComponent(message.ID)}`)).json();
    const code = `${detail.Text ?? ''}`.match(/\b[0-9]{8}\b/)?.[0];
    if (code) return code;
  }
  throw new Error('Verification email did not arrive.');
}

let browser;
let page;
try {
  const registered = await post('/auth/register', { email, password, timezone: 'Asia/Ho_Chi_Minh', locale: 'vi',
    termsVersion: 'v1', privacyVersion: 'v1', acceptTerms: true, acceptPrivacy: true });
  if (!registered.ok) throw new Error(`register ${registered.status}`);
  if (!(await post('/auth/email-verifications:confirm-code', { email, code: await verificationCode() })).ok) throw new Error('verify failed');
  passed.push('synthetic-account');

  browser = await chromium.launch({ executablePath: browserPath, headless: true });
  page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  if (process.env.CORS_SHIM === '1') {
    await page.route('**/api/v1/**', async (route) => {
      const cors = { 'access-control-allow-origin': new URL(appUrl).origin, 'access-control-allow-headers': '*',
        'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS' };
      if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers: cors });
      // Backend rejects a disallowed Origin server-side (403), so drop it for this test session only.
      const headers = { ...route.request().headers() };
      delete headers.origin;
      const response = await route.fetch({ headers });
      return route.fulfill({ response, headers: { ...response.headers(), ...cors } });
    });
  }

  await page.goto(`${appUrl}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.getByText('Mở sổ của bạn.').waitFor({ timeout: 30000 });
  await page.waitForLoadState('networkidle');
  // Wait until React has attached its handlers to the input; filling earlier never reaches component state.
  await page.waitForFunction(() => { const input = document.querySelector('input[aria-label="Email"]');
    return !!input && Object.keys(input).some((key) => key.startsWith('__reactProps')); }, null, { timeout: 30000 });
  await page.locator('input[aria-label="Email"]').last().fill(email);
  await page.locator('input[aria-label="Mật khẩu"]').last().fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await page.getByText('Đây là sổ của bạn.').waitFor({ timeout: 15000 });
  await page.getByRole('textbox', { name: 'Tên hiển thị' }).fill('Synthetic Safety Tester');
  await page.getByRole('textbox', { name: 'Bút danh' }).fill('Safety QA');
  await page.getByRole('checkbox', { name: 'Nhìn lại bản thân' }).click();
  await page.getByRole('button', { name: 'Hoàn tất' }).click();
  await page.getByText('Chào Safety QA.').waitFor({ timeout: 15000 });
  passed.push('login-onboarding');

  async function writeEntry(title, body) {
    // Navigate inside the app: a full page load on web drops the in-memory session.
    // Expo Router keeps earlier screens mounted but hidden; only act on visible elements.
    const visible = (locator) => locator.locator('visible=true').first();
    if (!(await visible(page.getByRole('button', { name: 'Viết trang mới' })).count())) {
      if (await visible(page.getByRole('button', { name: 'Về danh sách' })).count()) await visible(page.getByRole('button', { name: 'Về danh sách' })).click();
      else await visible(page.getByRole('button', { name: 'Mở nhật ký' })).click();
    }
    await visible(page.getByRole('button', { name: 'Viết trang mới' })).click();
    await visible(page.getByRole('textbox', { name: 'Tiêu đề' })).waitFor({ timeout: 15000 });
    await visible(page.getByRole('textbox', { name: 'Tiêu đề' })).fill(title);
    await visible(page.getByRole('textbox', { name: 'Nội dung' })).fill(body);
    await visible(page.getByRole('button', { name: 'Lưu nhật ký' })).click();
    await visible(page.getByText('Gợi ý nhìn lại')).waitFor({ timeout: 15000 });
  }

  // Count analysis reads to prove polling runs while waiting and stops when the screen is left.
  let analysisReads = 0;
  page.on('request', (request) => { if (/\/journal-entries\/[^/]+\/analysis$/.test(new URL(request.url()).pathname)) analysisReads++; });

  // M4 + M5-04: an ordinary entry shows the analysis card in plain language and no safety screen.
  await writeEntry('Synthetic ordinary day', 'Hôm nay mình đi dạo và uống trà với bạn. Một ngày nhẹ nhàng.');
  if (await page.getByText('Bạn không phải đơn độc lúc này.').isVisible()) throw new Error('Safety modal opened for an ordinary entry');
  await wait(2500);
  const ordinaryText = await page.locator('body').innerText();
  if (RAW_STATUS.test(ordinaryText)) throw new Error(`Raw status code visible: ${ordinaryText.match(RAW_STATUS)?.[0]}`);
  passed.push('analysis-card-plain-language');

  // M4-01: while the backend still reports a waiting state, the card says so and keeps polling.
  const waiting = await page.getByText('Đang đọc trang viết của bạn', { exact: false }).isVisible();
  if (waiting) {
    await wait(5000);
    if (analysisReads < 2) throw new Error(`Expected polling while waiting, saw ${analysisReads} read(s)`);
    passed.push('polls-while-waiting');
    // M4-05 (opt-in, ~2 min): polling gives up with a "come back later" message instead of spinning forever.
    if (process.env.LONG === '1') {
      await page.getByText('Phân tích đang mất nhiều thời gian hơn bình thường', { exact: false }).waitFor({ timeout: 150000 });
      const readsAtTimeout = analysisReads;
      await wait(35000);
      if (analysisReads !== readsAtTimeout) throw new Error('Polling continued after the timeout message');
      passed.push('polling-timeout-message');
    }
  }

  // M4-06: leaving the screen stops polling (longest backoff step is 30 s).
  await page.getByRole('button', { name: 'Về danh sách' }).locator('visible=true').first().click();
  await page.waitForURL(/\/journal$/, { timeout: 15000 });
  const readsWhenLeft = analysisReads;
  await wait(35000);
  if (analysisReads !== readsWhenLeft) throw new Error(`Polling continued after leaving the screen (${analysisReads - readsWhenLeft} extra read(s))`);
  passed.push('polling-stops-when-leaving');

  // M5: a HIGH-risk entry opens the safety screen right after saving; no phone number is invented.
  await writeEntry('Synthetic safety check', SAFETY_TRIGGER);
  await page.getByText('Bạn không phải đơn độc lúc này.').waitFor({ timeout: 15000 });
  await wait(1500);
  const modalText = await page.locator('body').innerText();
  const resources = await (await fetch(`${backend}/api/v1/safety/resources?locale=vi-VN&country=VN`)).json();
  // Contacts are only callable through 'Gọi …' buttons built from backend data; none may exist without an approved resource.
  const callButtons = await page.getByRole('button', { name: /^Gọi / }).count();
  if (Array.isArray(resources) && resources.length === 0 && (callButtons > 0 || PHONE.test(modalText.replace(/\d{4}-\d{2}-\d{2}/g, '')))) {
    throw new Error('A phone number is shown although the backend has no approved resource');
  }
  passed.push('safety-modal-after-high-risk-save');

  await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();
  await page.getByText('Trang này không có gợi ý tự động.', { exact: false }).waitFor({ timeout: 10000 });
  // M4-04: blocked-by-safety offers no retry.
  if (await page.getByRole('button', { name: 'Phân tích lại' }).count()) throw new Error('Retry offered for a safety-blocked entry');
  await page.getByRole('button', { name: 'Xem hỗ trợ an toàn' }).first().click();
  await page.getByText('Bạn không phải đơn độc lúc này.').waitFor();
  passed.push('blocked-analysis-no-retry-and-reopen-safety');
  await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();

  // M5-01: the HIGH-risk entry was still saved and appears in the journal list.
  await page.getByRole('button', { name: 'Về danh sách' }).locator('visible=true').first().click();
  await page.waitForURL(/\/journal$/, { timeout: 15000 });
  await page.getByText('Synthetic safety check').locator('visible=true').first().waitFor({ timeout: 15000 });
  passed.push('high-risk-entry-still-saved');

  if (errors.length) throw new Error(`Page errors: ${errors.join(' | ')}`);
  console.log(JSON.stringify({ ok: true, passed, approvedResources: Array.isArray(resources) ? resources.length : null }));
} catch (error) {
  const alerts = page ? await page.locator('[role=alert]').allInnerTexts().catch(() => []) : [];
  if (page && process.env.FAILURE_SCREENSHOT) await page.screenshot({ path: process.env.FAILURE_SCREENSHOT }).catch(() => {});
  console.error(JSON.stringify({ ok: false, passed, alerts, error: error instanceof Error ? error.message : String(error) }));
  process.exitCode = 1;
} finally {
  await browser?.close();
}
