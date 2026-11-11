import { test, expect } from '@playwright/test';
import { loginAsTestAccount, testAccount } from './helpers/login';
import { openToolsDrawerIfNeeded } from './helpers/tools-drawer';

/**
 * Bước 1 của COMPANION_UI_PLAN.md: nút "Công cụ dịu" mở thẳng 3 công cụ, không gọi API, không
 * cần consent. "Nghe âm thanh dịu" giờ mở máy phát nhạc nền (chi tiết ở ambient-player.spec.ts).
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

  test('mở menu và chạy Thở chậm 2 phút, dừng giữa chừng không lỗi', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    const menu = page.getByRole('menu', { name: 'Công cụ dịu' });
    await expect(menu).toBeVisible();

    await menu.getByRole('menuitem', { name: 'Thở chậm 2 phút' }).click();
    await expect(menu).toBeHidden();
    const status = page.getByRole('status');
    await expect(status).toContainText('Thở chậm cùng mình');
    await expect(status).toContainText(/[12]:\d{2}/); // đếm ngược 2 phút đang chạy

    await status.getByRole('button', { name: 'Dừng công cụ dịu' }).click();
    await expect(page.getByRole('status')).toHaveCount(0);
  });

  test('mở menu và phát âm thanh dịu (máy phát nhạc nền, bài Tiếng mưa), tắt được', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    await page.getByRole('menuitem', { name: 'Nghe âm thanh dịu' }).click();
    const player = page.getByRole('region', { name: 'Nhạc nền' });
    await expect(player).toContainText('Tiếng mưa rơi êm dịu');
    await expect(player).toContainText('Đang phát');

    await player.getByRole('button', { name: 'Tắt và đóng nhạc nền' }).click();
    await expect(player).toHaveCount(0);
  });

  test('viết xả chèn dòng mở đầu vào bài, không mở thanh trạng thái', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    await page.getByRole('menuitem', { name: 'Viết xả ra trang' }).click();

    await expect(page.locator('.ProseMirror')).toContainText('Điều làm mình bực nhất hôm nay là');
    await expect(page.getByRole('status')).toHaveCount(0);
  });

  test('rời trang khi âm thanh đang phát thì không còn tiến trình nào kẹt lại', async ({ page }) => {
    await page.getByRole('button', { name: 'Công cụ dịu' }).click();
    await page.getByRole('menuitem', { name: 'Nghe âm thanh dịu' }).click();
    await expect(page.getByRole('region', { name: 'Nhạc nền' })).toContainText('Đang phát');

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/dashboard/);
  });
});
