import { test, expect } from '@playwright/test';
import { loginAsTestAccount, testAccount } from './helpers/login';
import { openToolsDrawerIfNeeded } from './helpers/tools-drawer';

/**
 * Bước 1 của COMPANION_UI_PLAN.md: nút "Công cụ dịu" mở panel (Thở 2 phút, Nhạc dịu), không gọi
 * API, không cần consent. Chi tiết máy phát nhạc ở ambient-player.spec.ts.
 * Cần backend + tài khoản kiểm thử thật (xem tests/README.md); tự skip nếu thiếu.
 */
test.describe('Công cụ dịu trên trang viết', () => {
  test.beforeEach(async ({ page }) => {
    test.skip(!testAccount(), 'Cần PLAYWRIGHT_TEST_EMAIL/PLAYWRIGHT_TEST_PASSWORD và backend đang chạy.');
    await loginAsTestAccount(page);
    await page.goto('/journal-editor');
    await expect(page.locator('.ProseMirror[contenteditable="true"]')).toBeVisible();
    await openToolsDrawerIfNeeded(page);
  });

  test('mở panel và thở chậm 2 phút có đếm nhịp, dừng giữa chừng không lỗi', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    const panel = page.getByRole('complementary', { name: 'Công cụ dịu' });
    await expect(panel).toBeVisible();
    // "Viết xả ra trang" không còn nằm trong Công cụ dịu.
    await expect(panel.getByText('Viết xả ra trang')).toHaveCount(0);

    await panel.getByRole('button', { name: 'Bắt đầu thở' }).click();
    const exercise = panel.getByRole('region', { name: 'Thở chậm 2 phút' });
    await expect(exercise).toContainText('Hít vào');
    await expect(exercise).toContainText(/[12]:\d{2}/); // đếm ngược 2 phút đang chạy

    await exercise.getByRole('button', { name: 'Dừng thở' }).click();
    await expect(exercise.getByRole('button', { name: 'Bắt đầu thở' })).toBeVisible();
  });

  test('phát nhạc dịu (bài Tiếng mưa), đóng panel còn thanh nhạc nhỏ, tắt được', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    await page.getByRole('tab', { name: 'Nhạc dịu' }).click();
    const player = page.getByRole('region', { name: 'Nhạc nền', exact: true });
    await player.getByRole('button', { name: 'Phát' }).click();
    await expect(player).toContainText('Tiếng mưa rơi êm dịu');
    await expect(player).toContainText('Đang phát');

    await page.getByRole('button', { name: 'Đóng công cụ dịu' }).click();
    const mini = page.getByRole('region', { name: 'Thanh nhạc nền' });
    await expect(mini).toContainText('Đang phát');
    await mini.getByRole('button', { name: 'Tắt và đóng nhạc nền' }).click();
    await expect(mini).toHaveCount(0);
  });

  test('rời trang khi âm thanh đang phát thì không còn tiến trình nào kẹt lại', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    await page.getByRole('tab', { name: 'Nhạc dịu' }).click();
    const player = page.getByRole('region', { name: 'Nhạc nền', exact: true });
    await player.getByRole('button', { name: 'Phát' }).click();
    await expect(player).toContainText('Đang phát');

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/dashboard/);
  });
});
