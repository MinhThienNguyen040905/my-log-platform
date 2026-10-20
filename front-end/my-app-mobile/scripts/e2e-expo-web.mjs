import { chromium } from 'playwright-core';
import { randomBytes, randomUUID } from 'node:crypto';

const browserPath = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const email = `mobile-ui-${randomUUID()}@example.invalid`;
const password = `Synthetic-${randomBytes(18).toString('hex')}!`;
let browser;
const passed = [];
function step(name) { passed.push(name); }
function wait(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

try {
  browser = await chromium.launch({ executablePath: browserPath, headless: true });
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('http://localhost:3000/', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await page.getByRole('button', { name: 'Tạo tài khoản' }).click();
  await page.getByText('Bắt đầu trang mới.').waitFor();
  await page.locator('input[aria-label="Email"]').last().fill(email);
  await page.locator('input[aria-label="Mật khẩu"]').last().fill(password);
  await page.locator('input[aria-label="Nhập lại mật khẩu"]').last().fill(password);
  await page.getByRole('checkbox').click();
  await page.getByRole('button', { name: 'Tạo tài khoản' }).last().click();
  await page.getByText('Kiểm tra hộp thư.').waitFor({ timeout: 15000 });
  step('register-screen');

  let code;
  for (let attempt = 0; attempt < 15 && !code; attempt++) {
    await wait(500);
    const listing = await (await fetch('http://127.0.0.1:8025/api/v1/messages')).json();
    const message = listing.messages?.find((item) => JSON.stringify(item.To).includes(email));
    if (!message) continue;
    const detail = await (await fetch(`http://127.0.0.1:8025/api/v1/message/${encodeURIComponent(message.ID)}`)).json();
    code = `${detail.Text ?? ''} ${detail.HTML ?? ''}`.match(/\b[0-9]{8}\b/)?.[0];
  }
  if (!code) throw new Error('Verification email did not arrive in Mailpit.');
  await page.getByRole('textbox', { name: 'Mã xác minh' }).fill(code);
  await page.getByRole('button', { name: 'Xác minh', exact: true }).click();
  await page.getByText('Mở sổ của bạn.').last().waitFor({ timeout: 15000 });
  step('verify-screen');

  await page.locator('input[aria-label="Email"]').last().fill(email);
  await page.locator('input[aria-label="Mật khẩu"]').last().fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await page.getByText('Đây là sổ của bạn.').waitFor({ timeout: 15000 });
  step('login-screen');

  await page.getByRole('textbox', { name: 'Tên hiển thị' }).fill('Synthetic UI Tester');
  await page.getByRole('textbox', { name: 'Bút danh' }).fill('Test Journal');
  await page.getByRole('checkbox', { name: 'Nhìn lại bản thân' }).click();
  await page.getByRole('button', { name: 'Hoàn tất' }).click();
  await page.getByText('Chào Test Journal.').waitFor({ timeout: 15000 });
  step('onboarding-screen');

  await page.getByRole('button', { name: 'Mở nhật ký' }).click();
  await page.getByText('Những trang đã viết.').waitFor();
  await page.getByRole('button', { name: 'Viết trang mới' }).click();
  await page.getByRole('textbox', { name: 'Tiêu đề' }).fill('Synthetic UI journal');
  await page.getByRole('textbox', { name: 'Nội dung' }).fill('A synthetic note written by the mobile web E2E test.');
  await page.getByRole('button', { name: 'Lưu nhật ký' }).click();
  await page.getByText('Synthetic UI journal').last().waitFor({ timeout: 15000 });
  await page.getByRole('button', { name: 'Yêu thích', exact: true }).click();
  await page.getByRole('button', { name: 'Về danh sách' }).click();
  await page.getByRole('button', { name: 'Mở nhật ký Synthetic UI journal' }).click();
  page.once('dialog', (dialog) => void dialog.accept());
  await page.getByRole('button', { name: 'Xóa trang' }).click();
  await page.waitForURL('**/journal', { timeout: 15000 });
  await page.getByRole('button', { name: 'Về trang chủ' }).click();
  step('journal-create-favorite-delete-screen');

  await page.getByRole('button', { name: 'Tổng quan' }).click();
  await page.getByText('Những ngày đã qua.').waitFor();
  await page.getByRole('button', { name: 'Về trang chủ' }).click();
  await page.getByRole('button', { name: 'Góc nhìn và báo cáo' }).click();
  await page.getByText('Điều dữ liệu kể lại.').waitFor();
  await page.getByRole('button', { name: 'Về trang chủ' }).click();
  await page.getByRole('button', { name: 'Chăm sóc bản thân' }).click();
  await page.getByText('Điều bạn muốn vun đắp.').waitFor();
  await page.getByRole('textbox', { name: 'Tên mục tiêu' }).fill('Synthetic daily reflection');
  await page.getByRole('button', { name: 'Tạo mục tiêu' }).click();
  await page.getByText('Synthetic daily reflection', { exact: true }).waitFor({ timeout: 15000 });
  await page.getByRole('textbox', { name: 'Thói quen cho Synthetic daily reflection' }).fill('Synthetic habit');
  await page.getByRole('button', { name: 'Thêm thói quen hằng ngày' }).click();
  await page.getByText('Synthetic habit', { exact: false }).waitFor({ timeout: 15000 });
  await page.getByRole('button', { name: 'Về trang chủ' }).click();
  step('overview-and-self-care-screens');

  await page.getByRole('button', { name: 'Hồ sơ và cài đặt' }).click();
  await page.getByRole('textbox', { name: 'Bút danh' }).fill('Updated Journal');
  const profileSave = page.waitForResponse((response) => response.url().endsWith('/api/v1/me') && response.request().method() === 'PATCH', { timeout: 15000 });
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  const saveResponse = await profileSave;
  if (!saveResponse.ok()) throw new Error(`Profile save returned HTTP ${saveResponse.status()}`);

  await page.getByRole('button', { name: 'Phiên và quyền dữ liệu' }).click();
  await page.getByText('Phiên và quyền dữ liệu.').waitFor();
  await page.getByRole('button', { name: 'Tạo bản CSV' }).click();
  await page.getByText('CSV ·', { exact: false }).waitFor({ timeout: 15000 });
  await page.getByRole('button', { name: 'Về hồ sơ' }).click();
  step('settings-and-export-screen');

  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await page.waitForFunction(() => ['/', '/login'].includes(location.pathname), { timeout: 15000 });
  step('profile-and-logout-screen');
  if (errors.length) throw new Error(`Page errors: ${errors.map((value) => value.slice(0, 80)).join('; ')}`);
  process.stdout.write(`PASS ${passed.join(', ')}\n`);
} catch (error) {
  process.stderr.write(`FAIL after ${passed.join(', ')}: ${error instanceof Error ? error.message.slice(0, 2500) : 'unknown error'}\n`);
  process.exitCode = 1;
} finally {
  await browser?.close();
}
