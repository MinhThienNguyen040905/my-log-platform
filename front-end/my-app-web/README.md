# MyLog web

Giao diện Next.js App Router của MyLog. Ứng dụng web gọi backend qua các route `/api` của Next.js; nhật ký và dữ liệu tài khoản được lưu ở backend. Xem `AGENTS.md` để biết quy tắc kiến trúc chi tiết.

## Cấu trúc

| Thư mục | Trách nhiệm |
| --- | --- |
| `app/` | Route, layout, metadata và ghép các feature. Route handler `/api` là ranh giới HTTP phía web. |
| `features/<name>/` | API client, hook, schema, component và logic của từng nghiệp vụ. `index.ts` là API công khai cho feature khác. |
| `providers/` | Provider dùng toàn ứng dụng: ngôn ngữ, TanStack Query và thông báo. |
| `components/` | UI dùng chung, không import nghiệp vụ của feature. |
| `lib/` | Hạ tầng và utility dùng chung; `lib/i18n.ts` chứa dữ liệu bản dịch an toàn cho Server Component. |
| `types/` | Kiểu dữ liệu thực sự dùng xuyên feature. |

Import theo chiều `app → features → components/providers/lib`. Bên trong một feature dùng relative import; feature khác dùng API công khai qua `index.ts`. ESLint kiểm tra import ngược từ `app`, shared layer import feature và import qua barrel của chính feature.

`AccountProvider` sở hữu hồ sơ; `JournalProvider` sở hữu thao tác nhật ký. Chúng được ghép trong `app/_components/AuthGate.tsx`. Danh sách lịch sử dùng cursor và chỉ tải thêm khi người dùng yêu cầu. Dashboard, Insight và Settings dùng API tổng hợp; `journalEntryCount` của Dashboard là số bài trong khoảng thời gian đã yêu cầu, không phải tổng mọi thời điểm.

## Chạy và kiểm tra

```powershell
cd front-end/my-app-web
npm ci
npm run dev
npx tsc --noEmit
npm run lint
npm run build
```

Kiểm tra trên trình duyệt: đăng nhập/đăng ký với dữ liệu hợp lệ và không hợp lệ, tải lịch sử và trang tiếp theo, sửa/xóa/yêu thích bài, cập nhật hồ sơ, chuyển ngôn ngữ và tải lại trang. Luồng cần tài khoản phải dùng tài khoản thử nghiệm, không dùng nhật ký thật.

Build dùng `next/font/google` cho Playfair Display, Plus Jakarta Sans và Space Grotesk. Môi trường không truy cập được `fonts.googleapis.com` sẽ dừng build khi tải font; trường hợp đó chưa được tính là build đạt.