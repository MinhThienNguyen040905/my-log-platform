import { test, expect } from '@playwright/test';
import { loginAsTestAccount, testAccount } from './helpers/login';
import { openToolsDrawerIfNeeded } from './helpers/tools-drawer';

/**
 * Trợ lý trang viết (COMPANION_UI_PLAN.md mục 2.2 / Bước 2–5) chạy với adapter giả
 * (`NEXT_PUBLIC_COMPANION_MOCK=1` trong .env.local). Gom vào một test để chỉ đăng nhập một lần —
 * backend giới hạn 5 lần đăng nhập/15 phút/tài khoản. Tự skip nếu thiếu tài khoản hoặc không bật mock.
 */
test('trợ lý: consent → trả lời → chèn câu hỏi → Esc thu nhỏ → khép lại → thẻ thói quen → SAFETY_FLOW', async ({ page }) => {
  test.skip(!testAccount(), 'Cần PLAYWRIGHT_TEST_EMAIL/PLAYWRIGHT_TEST_PASSWORD và backend đang chạy.');
  test.skip(process.env.COMPANION_E2E_MOCK !== '1', 'Đặt COMPANION_E2E_MOCK=1 khi dev server chạy với NEXT_PUBLIC_COMPANION_MOCK=1.');
  test.setTimeout(90_000);

  await loginAsTestAccount(page);
  await page.goto('/journal-editor');
  const editor = page.locator('.ProseMirror[contenteditable="true"]');
  await expect(editor).toBeVisible();
  await editor.click();
  await page.keyboard.type('Hôm nay buổi họp kéo dài, mình hơi mệt.');
  await openToolsDrawerIfNeeded(page);

  // Mở trợ lý: lần đầu phải xin consent, chưa có câu trả lời nào.
  await page.getByRole('button', { name: 'Trợ lý đồng hành' }).click();
  const panel = page.getByRole('complementary', { name: 'Trợ lý trang viết' });
  await expect(panel).toBeVisible();
  await panel.getByRole('button', { name: 'Đồng ý cho phép trợ lý đọc bài đang viết' }).click();

  // Sau consent: có câu mở và ít nhất một câu hỏi gợi ý.
  const insertQuestion = panel.getByRole('button', { name: /Chèn câu hỏi vào bài/ }).first();
  await expect(insertQuestion).toBeVisible();
  await insertQuestion.click();
  await expect(page.locator('.ProseMirror blockquote')).toHaveCount(1);

  // Nhắn tự do.
  await panel.getByLabel('Tin nhắn cho trợ lý').fill('Mình muốn viết tiếp về buổi họp');
  await panel.getByRole('button', { name: 'Gửi tin nhắn' }).click();
  await expect(panel.getByText('Mình muốn viết tiếp về buổi họp')).toBeVisible();
  await expect(panel.getByText('Mình nghe rồi', { exact: false })).toBeVisible();

  // Esc thu nhỏ, mở lại vẫn còn hội thoại.
  await page.keyboard.press('Escape');
  await expect(panel).toBeHidden();
  await openToolsDrawerIfNeeded(page);
  await page.getByRole('button', { name: 'Trợ lý đồng hành' }).click();
  await expect(panel.getByText('Mình muốn viết tiếp về buổi họp')).toBeVisible();

  // Khép lại → thẻ đánh giá 1–5 và thẻ gợi ý thói quen từ mẫu.
  await panel.getByRole('button', { name: 'Khép lại' }).click();
  await expect(panel.getByRole('button', { name: 'Đánh giá tâm trạng mức 4 trên 5' })).toBeVisible();
  const habitCard = panel.getByRole('region', { name: 'Gợi ý thói quen tự chăm sóc' })
    .or(panel.getByLabel('Gợi ý thói quen tự chăm sóc'));
  await expect(habitCard.first()).toBeVisible();
  await panel.getByRole('button', { name: 'Không cảm ơn, tạm ẩn gợi ý thói quen 7 ngày' }).click();
  await expect(habitCard.first()).toBeHidden();

  // Kết thúc, không lưu. Mở lại: cuộc trò chuyện mới, hội thoại cũ đã xóa, có ô nhắn.
  await panel.getByRole('button', { name: 'Kết thúc cuộc trò chuyện, không đưa vào bài' }).click();
  await expect(panel).toBeHidden();
  await openToolsDrawerIfNeeded(page);
  await page.getByRole('button', { name: 'Trợ lý đồng hành' }).click();
  await expect(panel.getByLabel('Tin nhắn cho trợ lý')).toBeVisible();
  await expect(panel.getByText('Mình muốn viết tiếp về buổi họp')).toHaveCount(0);
});

test('trợ lý: SAFETY_FLOW đóng khung chat và mở SafetyModal', async ({ page }) => {
  test.skip(!testAccount(), 'Cần PLAYWRIGHT_TEST_EMAIL/PLAYWRIGHT_TEST_PASSWORD và backend đang chạy.');
  test.skip(process.env.COMPANION_E2E_MOCK !== '1', 'Đặt COMPANION_E2E_MOCK=1 khi dev server chạy với NEXT_PUBLIC_COMPANION_MOCK=1.');

  await loginAsTestAccount(page);
  await page.goto('/journal-editor');
  await expect(page.locator('.ProseMirror[contenteditable="true"]')).toBeVisible();
  await openToolsDrawerIfNeeded(page);
  const panel = page.getByRole('complementary', { name: 'Trợ lý trang viết' });
  await page.getByRole('button', { name: 'Trợ lý đồng hành' }).click();
  await panel.getByRole('button', { name: 'Đồng ý cho phép trợ lý đọc bài đang viết' }).click();
  await expect(panel.getByLabel('Tin nhắn cho trợ lý')).toBeVisible();
  await panel.getByLabel('Tin nhắn cho trợ lý').fill('test an toàn');
  await panel.getByRole('button', { name: 'Gửi tin nhắn' }).click();
  await expect(panel).toBeHidden();
  await expect(page.getByRole('dialog').first()).toBeVisible();
});
