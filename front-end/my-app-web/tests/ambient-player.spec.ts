import { test, expect, type Page } from '@playwright/test';

/**
 * Máy phát nhạc nền trên trang viết (AP-01..AP-13 trong kế hoạch). Không cần backend: phiên đăng
 * nhập được giả lập trong trình duyệt bằng hồ sơ synthetic, mọi API khác trả 503. Playwright không
 * nghe được âm thanh, nên test ghi lại các AudioContext trang tạo ra và kiểm tra trạng thái của chúng
 * (running = đang phát, suspended = tạm dừng, closed = đã tắt) cùng với giao diện.
 */
const PROFILE = {
  userId: 'synthetic-user', displayName: 'Demo', penName: 'Demo', onboardingGoals: [],
  timezone: 'Asia/Ho_Chi_Minh', locale: 'vi', onboardingCompletedAt: '2026-01-01T00:00:00Z', version: 1,
};
const TRACK_ORDER = ['Tiếng mưa rơi êm dịu', 'Piano lofi trầm lắng', 'Sóng biển & gió nhẹ', 'Lửa trại đêm', 'Quán cà phê yên tĩnh'];

async function openEditor(page: Page) {
  await page.context().addCookies([{ name: 'mylog_refresh', value: 'synthetic', domain: 'localhost', path: '/' }]);
  await page.addInitScript(() => {
    const Original = window.AudioContext;
    (window as unknown as { __contexts: AudioContext[] }).__contexts = [];
    window.AudioContext = class extends Original {
      constructor(options?: AudioContextOptions) {
        super(options);
        (window as unknown as { __contexts: AudioContext[] }).__contexts.push(this);
      }
    };
  });
  await page.route('**/api/**', (route) => route.request().url().includes('/api/auth/session')
    ? route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(PROFILE) })
    : route.fulfill({ status: 503, body: '' }));
  await page.goto('/journal-editor', { waitUntil: 'networkidle' });
  await expect(page.getByRole('button', { name: 'Công cụ dịu', exact: true })).toBeVisible();
}

/** The player has no button of its own: it opens from "Công cụ dịu" → "Nghe âm thanh dịu". */
async function openPlayer(page: Page) {
  await page.getByRole('button', { name: 'Công cụ dịu', exact: true }).click();
  await page.getByRole('menuitem', { name: 'Nghe âm thanh dịu' }).click();
}

const contextStates = (page: Page) =>
  page.evaluate(() => (window as unknown as { __contexts: AudioContext[] }).__contexts.map((c) => c.state));
const player = (page: Page) => page.getByRole('region', { name: 'Nhạc nền' });
const trackTitle = (page: Page) => player(page).locator('[aria-live="polite"]');

test.describe('ambient music player', () => {
  test.beforeEach(({}, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Desktop flow; AP-13 covers the phone layout explicitly.');
  });

  test('AP-01/12: open, play, pause and resume — also by keyboard', async ({ page }) => {
    await openEditor(page);
    expect(await contextStates(page)).toEqual([]); // nothing plays before a click
    await openPlayer(page);
    await expect(player(page)).toBeVisible();
    await expect(trackTitle(page)).toHaveText(TRACK_ORDER[0]);
    await expect.poll(() => contextStates(page)).toEqual(['running']);
    // No separate music button on the rail; "Công cụ dịu" stays lit while the player is open.
    const rail = page.getByRole('complementary', { name: 'Công cụ trang viết' });
    await expect(rail.getByRole('button', { name: /nhạc nền/i })).toHaveCount(0);
    await expect(rail.getByRole('button')).toHaveCount(5);
    await expect(page.getByRole('button', { name: 'Công cụ dịu', exact: true })).toHaveAttribute('aria-pressed', 'true');

    const pause = player(page).getByRole('button', { name: 'Tạm dừng' });
    await pause.focus();
    await page.keyboard.press('Enter');
    await expect.poll(() => contextStates(page)).toEqual(['suspended']);
    await expect(player(page)).toContainText('Tạm dừng');
    await player(page).getByRole('button', { name: 'Phát' }).click();
    await expect.poll(() => contextStates(page)).toEqual(['running']);
  });

  test('AP-02/03: next and previous wrap around; the list selects and marks the current track', async ({ page }) => {
    await openEditor(page);
    await openPlayer(page);
    for (const title of TRACK_ORDER.slice(1)) {
      await player(page).getByRole('button', { name: 'Bài sau' }).click();
      await expect(trackTitle(page)).toHaveText(title);
    }
    await player(page).getByRole('button', { name: 'Bài sau' }).click();
    await expect(trackTitle(page)).toHaveText(TRACK_ORDER[0]);
    await player(page).getByRole('button', { name: 'Bài trước' }).click();
    await expect(trackTitle(page)).toHaveText(TRACK_ORDER[4]);

    await player(page).getByRole('button', { name: 'Danh sách nhạc nền' }).click();
    await player(page).getByRole('button', { name: /Lửa trại đêm/ }).click();
    await expect(trackTitle(page)).toHaveText('Lửa trại đêm');
    await player(page).getByRole('button', { name: 'Danh sách nhạc nền' }).click();
    await expect(player(page).getByRole('button', { name: /Lửa trại đêm/ })).toHaveAttribute('aria-current', 'true');
    // Switching tracks cross-fades inside one context instead of opening new ones.
    expect(await contextStates(page)).toEqual(['running']);
  });

  test('AP-04: volume and mute; volume and last track are remembered after a reload', async ({ page }) => {
    await openEditor(page);
    await openPlayer(page);
    await player(page).getByRole('slider', { name: 'Âm lượng' }).fill('30');
    await player(page).getByRole('button', { name: 'Bài sau' }).click();
    await player(page).getByRole('button', { name: 'Tắt tiếng' }).click();
    await expect(player(page).getByRole('button', { name: 'Bật tiếng' })).toHaveAttribute('aria-pressed', 'true');
    await player(page).getByRole('button', { name: 'Bật tiếng' }).click();

    await page.reload({ waitUntil: 'networkidle' });
    await openPlayer(page);
    await expect(player(page).getByRole('slider', { name: 'Âm lượng' })).toHaveValue('30');
    await expect(trackTitle(page)).toHaveText(TRACK_ORDER[1]);
  });

  test('AP-05: the sleep timer fades out, stops and says why', async ({ page }) => {
    await page.clock.install();
    await openEditor(page);
    await openPlayer(page);
    await player(page).getByRole('combobox', { name: 'Hẹn giờ tắt nhạc nền' }).selectOption('15');
    await expect(player(page)).toContainText('tắt sau 15 phút');
    await page.clock.runFor('15:00');
    await page.clock.runFor('00:06');
    await expect(player(page).getByRole('status')).toContainText('Đã tắt nhạc nền theo hẹn giờ.');
    await expect.poll(() => contextStates(page)).toEqual(['closed']);
    await expect(player(page)).toContainText('Đã dừng');
  });

  test('AP-06: a picked mood suggests a track but never starts it by itself', async ({ page }) => {
    await openEditor(page);
    await expect(page.getByRole('button', { name: /Gợi ý nghe/ })).toHaveCount(0); // the default mood does not count
    await page.getByRole('button', { name: 'Tức giận', exact: true }).click();
    const chip = page.getByRole('button', { name: 'Gợi ý nghe: Tiếng mưa rơi êm dịu' });
    await expect(chip).toBeVisible();
    await page.waitForTimeout(500);
    expect(await contextStates(page)).toEqual([]);
    await chip.click();
    await expect(trackTitle(page)).toHaveText('Tiếng mưa rơi êm dịu');
    await expect.poll(() => contextStates(page)).toEqual(['running']);

    await player(page).getByRole('button', { name: 'Tắt và đóng nhạc nền' }).click();
    await page.getByRole('button', { name: 'Buồn', exact: true }).click();
    await page.getByRole('button', { name: 'Ẩn gợi ý nhạc nền' }).click();
    await expect(page.getByRole('button', { name: /Gợi ý nghe/ })).toHaveCount(0);
  });

  test('AP-07/09: "Nghe âm thanh dịu" opens the player on rain; breathing runs alongside', async ({ page }) => {
    await openEditor(page);
    await page.getByRole('button', { name: 'Công cụ dịu', exact: true }).click();
    await page.getByRole('menuitem', { name: 'Nghe âm thanh dịu' }).click();
    await expect(trackTitle(page)).toHaveText('Tiếng mưa rơi êm dịu');
    await expect.poll(() => contextStates(page)).toEqual(['running']);

    await page.getByRole('button', { name: 'Công cụ dịu', exact: true }).click();
    await page.getByRole('menuitem', { name: 'Thở chậm 2 phút' }).click();
    await expect(page.getByRole('button', { name: 'Dừng công cụ dịu' })).toBeVisible();
    await expect(player(page)).toBeVisible();
    expect(await contextStates(page)).toEqual(['running']);
  });

  test('AP-10: leaving the editor closes the audio', async ({ page }) => {
    await openEditor(page);
    await openPlayer(page);
    await expect.poll(() => contextStates(page)).toEqual(['running']);
    await page.getByRole('link', { name: 'Tổng quan' }).first().click();
    await page.waitForURL(/\/dashboard/);
    await expect.poll(() => contextStates(page)).toEqual(['closed']);
  });

  test('AP-11: reduced motion freezes the spinning disc', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await openEditor(page);
    await openPlayer(page);
    const disc = player(page).locator('span.rounded-full.bg-neutral-900');
    await expect(disc).toHaveCSS('animation-iteration-count', '1');
  });

  test('AP-13: phone layout — no horizontal scroll, the drawer shows the five tools', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await openEditor(page).catch(() => { /* on phones the rail is a hidden drawer until opened */ });
    // On phones the tools live in a drawer opened by the floating wrench button.
    await page.getByRole('button', { name: 'Mở công cụ' }).click();
    const drawer = page.getByRole('complementary', { name: 'Công cụ trang viết' });
    await expect(drawer.getByRole('button')).toHaveCount(6); // 5 tools + close
    await drawer.getByRole('button', { name: 'Công cụ dịu', exact: true }).click();
    await page.getByRole('menuitem', { name: 'Nghe âm thanh dịu' }).click();
    if (await drawer.isVisible()) await drawer.getByRole('button', { name: 'Đóng công cụ' }).click();
    await expect(player(page)).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
    // The track list must stay on screen too.
    await player(page).getByRole('button', { name: 'Danh sách nhạc nền' }).click();
    const box = await player(page).getByRole('list', { name: 'Danh sách nhạc nền' }).boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(375);
  });
});
