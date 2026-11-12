import zlib from 'node:zlib';
import { test, expect, type Page } from '@playwright/test';
import { BACKEND_URL, createOnboardedAccount, localStackAvailable } from './helpers/mailpit';

/**
 * Inserting an image in the journal editor with local asset storage (Cloudinary off): the backend returns
 * signed image URLs as backend paths, which the web rewrites to its signed-image proxy. Also saves an entry
 * with highlighted text, which the backend accepts as a single-colour mark. Synthetic image and text only.
 */
/** Valid solid-colour RGB PNG built in memory (synthetic, no real photo). */
function syntheticPng(size = 8): Buffer {
  const chunk = (type: string, data: Buffer) => {
    const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const length = Buffer.alloc(4); length.writeUInt32BE(data.length);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(zlib.crc32(body) >>> 0);
    return Buffer.concat([length, body, crc]);
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0); header.writeUInt32BE(size, 4);
  header[8] = 8; header[9] = 2; // 8-bit RGB
  const row = Buffer.concat([Buffer.from([0]), Buffer.alloc(size * 3, 0x7a)]);
  const pixels = zlib.deflateSync(Buffer.concat(Array.from({ length: size }, () => row)));
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header), chunk('IDAT', pixels), chunk('IEND', Buffer.alloc(0))]);
}
const PNG = syntheticPng();

const loaded = (page: Page, selector: string) =>
  page.waitForFunction((s) => {
    const img = document.querySelector<HTMLImageElement>(s);
    return !!img && img.complete && img.naturalWidth > 0;
  }, selector, { timeout: 15000 });

test.describe('journal image insert', () => {
  test.beforeEach(async ({ request }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Backend flow: run once per suite.');
    test.skip(!(await localStackAvailable(request)), 'Needs local backend and Mailpit.');
  });

  test('preview and inserted image load; highlighted text saves', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'image-insert');
    await page.goto('/auth/login');
    await page.locator('#login-email').fill(account.email);
    await page.locator('#login-password').fill(account.password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
    await page.goto('/journal-editor');
    const editor = page.locator('.ProseMirror[contenteditable="true"]');
    await expect(editor).toBeVisible();

    await editor.click();
    await page.keyboard.type('Synthetic highlighted line');
    await page.getByText('Synthetic highlighted line').click({ clickCount: 3 });
    await page.getByRole('button', { name: 'Tô sáng' }).click();
    await expect(editor.locator('mark')).toHaveText('Synthetic highlighted line');
    await page.keyboard.press('End');

    await page.getByRole('button', { name: 'Chèn ảnh', exact: true }).click();
    await page.locator('input[type="file"][accept*="image"]').setInputFiles({ name: 'synthetic.png', mimeType: 'image/png', buffer: PNG });
    await loaded(page, 'img[alt="Preview"]');
    await page.getByRole('button', { name: 'Dán vào trang viết' }).click();
    await loaded(page, '.ProseMirror img[data-asset-id]');

    await page.getByRole('button', { name: 'Lưu', exact: true }).first().click();
    const auth = { Authorization: `Bearer ${account.token}` };
    await expect.poll(async () => (await (await request.get(`${BACKEND_URL}/api/v1/journal-entries?limit=1`, { headers: auth })).json()).items.length,
      { timeout: 15000 }).toBe(1);
    const list = await (await request.get(`${BACKEND_URL}/api/v1/journal-entries?limit=1`, { headers: auth })).json();
    const detail = await (await request.get(`${BACKEND_URL}/api/v1/journal-entries/${list.items[0].id}`, { headers: { Authorization: `Bearer ${account.token}` } })).json();
    const json = JSON.stringify(detail.contentJson);
    expect(json).toContain('"type":"highlight"');
    expect(json).toContain('"data-asset-id"');

    // Reopening the entry keeps the image with its asset ID, so updating the entry still saves the image.
    await page.goto(`/journal-editor?id=${list.items[0].id}`);
    await loaded(page, '.ProseMirror img[data-asset-id]');
    await expect(page.locator('.ProseMirror').getByRole('button', { name: 'Xóa ảnh' })).toHaveCount(1);
    await page.locator('.ProseMirror').getByText('Synthetic highlighted line').click();
    await page.keyboard.press('End');
    await page.keyboard.type(' edited');
    await page.getByRole('button', { name: 'Cập nhật bài' }).first().click();
    await expect.poll(async () => JSON.stringify((await (await request.get(`${BACKEND_URL}/api/v1/journal-entries/${list.items[0].id}`,
      { headers: auth })).json()).contentJson), { timeout: 15000 }).toContain('edited');
    const updated = await (await request.get(`${BACKEND_URL}/api/v1/journal-entries/${list.items[0].id}`, { headers: auth })).json();
    expect(JSON.stringify(updated.contentJson)).toContain(detail.contentJson.content.find((n: { type: string }) => n.type === 'image').attrs['data-asset-id']);
  });

  test('an image is removed only with its X button, never with Backspace/Delete', async ({ page, request }) => {
    const account = await createOnboardedAccount(request, 'image-remove');
    await page.goto('/auth/login');
    await page.locator('#login-email').fill(account.email);
    await page.locator('#login-password').fill(account.password);
    await page.locator('#login-password').locator('xpath=ancestor::form[1]').locator('button[type="submit"]').click();
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
    await page.goto('/journal-editor');
    const editor = page.locator('.ProseMirror[contenteditable="true"]');
    await editor.click();
    await page.keyboard.type('Before the photo');

    await page.getByRole('button', { name: 'Chèn ảnh', exact: true }).click();
    await page.locator('input[type="file"][accept*="image"]').setInputFiles({ name: 'synthetic.png', mimeType: 'image/png', buffer: PNG });
    await loaded(page, 'img[alt="Preview"]');
    await page.getByRole('button', { name: 'Dán vào trang viết' }).click();
    await loaded(page, '.ProseMirror img[data-asset-id]');
    const image = editor.locator('img[data-asset-id]');

    // Cursor right after the image: Backspace selects it, more Backspace/Delete must not remove it.
    await page.keyboard.type('After');
    for (let i = 0; i < 8; i++) await page.keyboard.press('Backspace');
    await page.keyboard.press('Delete');
    await expect(image).toHaveCount(1);
    // Select everything and delete: text around may change, the image stays.
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await expect(image).toHaveCount(1);
    // Typing still works after the blocked deletion.
    await editor.click();
    await page.keyboard.press('Control+End');
    await page.keyboard.type(' still editable');
    await expect(editor).toContainText('still editable');

    await image.hover();
    await editor.getByRole('button', { name: 'Xóa ảnh' }).click();
    await expect(image).toHaveCount(0);
    await expect(editor).toContainText('still editable');
  });
});
