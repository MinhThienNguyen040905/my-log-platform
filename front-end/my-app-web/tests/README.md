# Playwright — web

## Chạy

```bash
npm run test:e2e              # headless, chạy luôn `npm run dev` (dev server tự khởi động)
npm run test:e2e:ui           # chế độ UI, xem từng bước
npx playwright test smoke     # chỉ chạy 1 file
```

Lần đầu trên máy mới: `npx playwright install chromium`.

## Hai nhóm test

### 1. `smoke.spec.ts` — không cần backend
Chỉ chạm các trang công khai (`/`, `/auth/login`). Dùng để xác nhận harness Playwright + dev server hoạt động, không cần Docker/PostgreSQL/tài khoản thật.

### 2. Mọi route trong `app/(app)/` (dashboard, journal-editor, history-calendar, insight, reports, setting, admin...)
`app/(app)/layout.tsx` chặn bằng cookie `mylog_refresh` trước khi vào, và dữ liệu thực tế đi qua BFF tới backend thật. Test các route này cần:

- Backend chạy ở `http://localhost:8080` (hoặc đặt `BASE_URL`/biến env API tương ứng) — xem `backend/README.md`. Cần Docker cho PostgreSQL theo `backend/AGENTS.md`.
- Một tài khoản kiểm thử **đã xác minh email và hoàn tất onboarding**, đặt qua:
  ```bash
  export PLAYWRIGHT_TEST_EMAIL="..."
  export PLAYWRIGHT_TEST_PASSWORD="..."
  ```
  Không commit hai giá trị này. Không dùng dữ liệu người dùng thật — tạo tài khoản synthetic riêng cho test.
- Nếu hai biến trên không có, các test trong `journal-editor-calming-tools.spec.ts` tự `test.skip()` (không fail) — xem log để biết lý do.

`JOURNAL_NAV_PLAYWRIGHT_SCENARIOS.md` ở thư mục gốc là một bộ kịch bản điều hướng rộng hơn, viết theo lối "giao cho agent" (có chú thích thủ công); `journal-editor-calming-tools.spec.ts` là bản Playwright thật, tự động, chỉ cho tính năng Công cụ dịu (xem `COMPANION_UI_PLAN.md`).

## Quy tắc khi thêm test
- Chỉ dùng dữ liệu nhật ký/tài khoản tổng hợp (synthetic), không copy dữ liệu thật vào assertion hay fixture.
- Không `waitForTimeout` cố định khi có thể chờ locator/request/URL.
- Không log hay chụp màn hình chứa token, cookie, mật khẩu.
- Mỗi test tạo state riêng (tài khoản/bài viết của nó); không phụ thuộc thứ tự chạy giữa các test.
