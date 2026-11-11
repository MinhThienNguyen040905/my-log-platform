// M2 — quên / đặt lại mật khẩu trên Expo web với backend local thật (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md mục 5).
// Cần: backend local, Mailpit, Expo web đang chạy (`npx expo start --web`). Chỉ dùng tài khoản synthetic.
// Backend local chỉ cho CORS từ http://localhost:3000; nếu Expo web chạy ở cổng khác, đặt CORS_SHIM=1 để
// trình duyệt test tự thêm header CORS cho request API (chỉ trong phiên test, không đổi backend).
import { chromium } from 'playwright-core';
import { randomBytes, randomUUID } from 'node:crypto';

const appUrl = process.env.EXPO_WEB_URL ?? 'http://localhost:8081';
const backend = process.env.MYLOG_BACKEND_URL ?? 'http://localhost:8080';
const mailpit = process.env.MAILPIT_URL ?? 'http://127.0.0.1:8025';
const browserPath = process.env.BROWSER_PATH ?? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const email = `mobile-reset-${randomUUID()}@example.invalid`;
const password = `Synthetic-${randomBytes(12).toString('hex')}!`;
const newPassword = `${password}New`;
const passed = [];
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function latestCode(to, subject) {
  for (let attempt = 0; attempt < 30; attempt++) {
    await wait(500);
    const listing = await (await fetch(`${mailpit}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`)).json();
    const message = listing.messages?.find((item) => item.Subject.includes(subject));
    if (!message) continue;
    const detail = await (await fetch(`${mailpit}/api/v1/message/${encodeURIComponent(message.ID)}`)).json();
    const code = `${detail.Text ?? ''}`.match(/\b[0-9]{8}\b/)?.[0];
    if (code) return code;
  }
  throw new Error(`No "${subject}" email arrived in Mailpit.`);
}

async function post(path, body) {
  return fetch(`${backend}/api/v1${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
}

let browser;
let page;
try {
  const registered = await post('/auth/register', { email, password, timezone: 'Asia/Ho_Chi_Minh', locale: 'vi',
    termsVersion: 'v1', privacyVersion: 'v1', acceptTerms: true, acceptPrivacy: true });
  if (!registered.ok) throw new Error(`register ${registered.status}`);
  const verified = await post('/auth/email-verifications:confirm-code', { email, code: await latestCode(email, 'xác minh email') });
  if (!verified.ok) throw new Error(`verify ${verified.status}`);
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

  // W1-11 on mobile: CHECK_RATE_LIMIT=1 submits until the backend answers 429 (5 / 15 min per email,
  // 10 / 15 min per IP — the browser's own address, which differs from the web BFF's). Uses up the quota.
  if (process.env.CHECK_RATE_LIMIT === '1') {
    await page.getByRole('button', { name: 'Quên mật khẩu?' }).click();
    await page.getByText('Đặt lại mật khẩu.').waitFor();
    const limitEmail = `mobile-limit-${randomUUID()}@example.invalid`;
    let limited = false;
    for (let attempt = 0; attempt < 12 && !limited; attempt++) {
      if (await page.getByRole('button', { name: 'Gửi lại yêu cầu' }).isVisible()) {
        await page.getByRole('button', { name: 'Gửi lại yêu cầu' }).click();
      }
      await page.locator('input[aria-label="Email"]').last().fill(limitEmail);
      await page.getByRole('button', { name: 'Gửi hướng dẫn' }).click();
      await page.waitForTimeout(800);
      limited = await page.getByText('Bạn đã thử quá nhiều lần', { exact: false }).isVisible();
    }
    if (!limited) throw new Error('Never reached the rate limit');
    if (!(await page.getByRole('button', { name: 'Gửi hướng dẫn' }).isVisible())) throw new Error('Form not usable after 429');
    console.log(JSON.stringify({ ok: true, passed: ['rate-limit-message'] }));
    process.exit(0);
  }
  await page.locator('input[aria-label="Email"]').last().fill(email);
  await page.getByRole('button', { name: 'Quên mật khẩu?' }).click();
  await page.getByText('Đặt lại mật khẩu.').waitFor();
  // The email typed on the login screen is carried over in memory, not through the URL.
  const carried = await page.locator('input[aria-label="Email"]').last().inputValue();
  if (carried !== email) throw new Error(`Email not carried to forgot screen (got length ${carried.length}, expected ${email.length})`);
  if (page.url().includes('example.invalid')) throw new Error('Email leaked into the URL');
  passed.push('login-to-forgot');

  await page.getByRole('button', { name: 'Gửi hướng dẫn' }).click();
  await page.getByText('Nếu email này có trong hệ thống', { exact: false }).waitFor({ timeout: 15000 });
  passed.push('request-reset-generic-message');

  const code = await latestCode(email, 'Đặt lại mật khẩu');
  await page.getByRole('button', { name: 'Nhập mã 8 chữ số' }).click();
  await page.getByText('Mật khẩu mới.', { exact: true }).waitFor();

  // Client-side checks block a short code and a short password before any request (W1-06, W1-10).
  let confirmCalls = 0;
  page.on('request', (request) => { if (request.url().includes('/auth/password-resets:confirm-code')) confirmCalls++; });
  await page.locator('input[aria-label="Mã 8 chữ số"]').fill(code.slice(0, 6));
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Nhập email và mã 8 chữ số.').waitFor();
  await page.locator('input[aria-label="Mã 8 chữ số"]').fill(code);
  await page.locator('input[aria-label="Mật khẩu mới"]').fill('short');
  await page.locator('input[aria-label="Nhập lại mật khẩu mới"]').fill('short');
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Mật khẩu mới cần ít nhất 12 ký tự.').waitFor();
  await page.locator('input[aria-label="Mật khẩu mới"]').fill(newPassword);
  await page.locator('input[aria-label="Nhập lại mật khẩu mới"]').fill(`${newPassword}x`);
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Mật khẩu nhập lại chưa khớp.').waitFor();
  if (confirmCalls !== 0) throw new Error(`Client validation still sent ${confirmCalls} request(s)`);
  passed.push('client-validation-no-request');

  // M2-03: a code pasted with a space keeps all 8 digits.
  await page.locator('input[aria-label="Mã 8 chữ số"]').fill(`${code.slice(0, 4)} ${code.slice(4)}`);
  const pasted = await page.locator('input[aria-label="Mã 8 chữ số"]').inputValue();
  if (pasted !== code) throw new Error(`Pasted code became "${pasted.replace(/\d/g, '#')}" (expected 8 digits)`);
  passed.push('paste-code-with-space');

  // Same password as before → backend detail NEW_PASSWORD_SAME_AS_OLD gets its own message.
  await page.locator('input[aria-label="Mật khẩu mới"]').fill(password);
  await page.locator('input[aria-label="Nhập lại mật khẩu mới"]').fill(password);
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Mật khẩu mới phải khác mật khẩu hiện tại.').waitFor({ timeout: 15000 });
  passed.push('same-password-message');

  // Wrong code → generic invalid/expired message.
  const wrong = code === '00000000' ? '11111111' : '00000000';
  await page.locator('input[aria-label="Mã 8 chữ số"]').fill(wrong);
  await page.locator('input[aria-label="Mật khẩu mới"]').fill(newPassword);
  await page.locator('input[aria-label="Nhập lại mật khẩu mới"]').fill(newPassword);
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Mã không hợp lệ hoặc đã hết hạn', { exact: false }).waitFor({ timeout: 15000 });
  passed.push('wrong-code-message');

  await page.locator('input[aria-label="Mã 8 chữ số"]').fill(`${code.slice(0, 4)} ${code.slice(4)}`);
  await page.getByRole('button', { name: 'Đặt mật khẩu mới' }).click();
  await page.getByText('Đã đặt lại mật khẩu.', { exact: false }).waitFor({ timeout: 15000 });
  passed.push('reset-success');

  // W1-02 on mobile: an unknown email shows exactly the same message as a registered one.
  await page.getByRole('button', { name: 'Quên mật khẩu?' }).last().click();
  await page.getByText('Đặt lại mật khẩu.').last().waitFor();
  await page.locator('input[aria-label="Email"]').last().fill(`mobile-unknown-${randomUUID()}@example.invalid`);
  await page.getByRole('button', { name: 'Gửi hướng dẫn' }).last().click();
  await page.getByText('Nếu email này có trong hệ thống', { exact: false }).last().waitFor({ timeout: 15000 });
  passed.push('unknown-email-same-message');

  const oldLogin = await post('/auth/login', { email, password });
  const newLogin = await post('/auth/login', { email, password: newPassword });
  if (oldLogin.ok || !newLogin.ok) throw new Error(`login after reset: old=${oldLogin.status} new=${newLogin.status}`);
  passed.push('old-password-rejected-new-accepted');

  if (errors.length) throw new Error(`Page errors: ${errors.join(' | ')}`);
  console.log(JSON.stringify({ ok: true, passed }));
} catch (error) {
  const alerts = page ? await page.locator('[role=alert]').allInnerTexts().catch(() => []) : [];
  console.error(JSON.stringify({ ok: false, passed, alerts, error: error instanceof Error ? error.message : String(error) }));
  process.exitCode = 1;
} finally {
  await browser?.close();
}
