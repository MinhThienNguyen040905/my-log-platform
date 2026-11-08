import type { Page } from '@playwright/test';

/**
 * Dưới 1360px, nút "Công cụ dịu"/"Trợ lý đồng hành" nằm trong ngăn kéo đóng mặc định, mở bằng nút
 * nổi "Mở công cụ" (xem JournalEditor.tsx, cùng điều kiện responsive với COMPANION_UI_PLAN.md mục
 * 2.3). Quyết định bằng `viewportSize()` thay vì dò `isVisible()` trên nút nổi: `isVisible()` chỉ
 * chụp một khoảnh khắc, không tự chờ, nên có thể đọc nhầm lúc trang vừa hydrate xong — khiến bước
 * mở ngăn kéo bị bỏ qua và các bước sau treo tới hết timeout. Viewport thì biết ngay, không đua.
 */
export async function openToolsDrawerIfNeeded(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (viewport && viewport.width >= 1360) return;
  // Ngăn kéo có thể đã mở từ bước trước (nút nổi khi đó tên là "Đóng công cụ").
  const toggle = page.locator('button[aria-expanded][aria-label$="công cụ"]');
  if ((await toggle.getAttribute('aria-label')) === 'Mở công cụ') await toggle.click();
}
