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

## Đăng ký và xác minh email

`/auth/register` gửi thông tin tới backend để tạo lượt đăng ký tạm. Email có liên kết xác minh hạn 24 giờ và mã 8 chữ số hạn 10 phút. Chỉ sau khi người dùng xác minh bằng một trong hai cách thì tài khoản thật mới được tạo. Web giữ email tạm trong `sessionStorage` tối đa 30 phút để nhập mã hoặc gửi lại thư; đăng ký lại bằng cùng email sẽ thay lượt tạm và vô hiệu hóa mã/link cũ. Trang `/auth/verify-email` không tự xác minh khi mở link bằng GET: người dùng phải bấm xác nhận để web gọi `POST /api/v1/auth/email-verifications:confirm`. Token được xóa khỏi thanh địa chỉ và không lưu trong browser storage. Sau khi xác minh, người dùng đăng nhập riêng.

Onboarding đọc trạng thái đồng ý từ backend. `TERMS` và `PRIVACY` đã ghi nhận khi đăng ký. Các quyền tùy chọn `AI_PROCESSING`, `ANALYTICS`, `MODEL_TRAINING` mặc định chưa được cấp; trang hiển thị trạng thái vì chưa có tài liệu đồng ý công bố để bật quyền. Quyền đã được cấp từ trước có thể bỏ chọn để thu hồi.

## Công cụ tài khoản local

Khi thử nghiệm local, mở `http://localhost:3000/dev/users` để xem email của các tài khoản đang hoạt động. Cấu hình cùng giá trị `MYLOG_DEV_USERS_KEY` trong `backend/.env` và `front-end/my-app-web/.env.local`. Trang tự tải danh sách, chỉ hoạt động khi Next.js chạy `npm run dev` trên `127.0.0.1`; khóa chỉ được server web dùng để gọi backend. Backend phải chạy profile `local`. Xem [hướng dẫn backend](../../backend/README.md).

## Linh vật

Trang chủ (`/dashboard`) hiển thị linh vật user chọn ở Cài đặt (Mầm Xanh, Mèo Cam, Ly Cà Phê), mang cảm xúc chọn nhiều nhất 7 ngày qua. Nút dưới linh vật (tưới nước / cho ăn / thêm hạt cà phê) mở check-in hôm nay trong hộp thoại (pop-up); khi user chọn "Ẩn linh vật", check-in hiện thẳng trên trang chủ. Hình vẽ là SVG inline (`features/insights/components/MascotArt.tsx`), chuyển động bằng CSS (`MoodMascot.module.css`), không cần asset hay runtime ngoài. Cảm xúc → biểu cảm: Vui → vui, Bình thường/chưa có dữ liệu → bình thường, Tức giận → tức giận, Buồn → buồn, Rất tệ → mệt mỏi. Tuần có bài rủi ro cao (`calm`) hoặc thiết bị bật giảm chuyển động thì linh vật đứng yên; khi `calm` không hiện mặt tức giận.

## Bắt đầu hành trình mới

Cài đặt → "Dữ liệu & quyền riêng tư" có thẻ "Bắt đầu hành trình mới": xóa vĩnh viễn toàn bộ dữ liệu nhật ký (qua `POST /api/me/data-reset`), giữ tài khoản. Xác nhận bằng ô đồng ý, gõ `BẮT ĐẦU LẠI` và mật khẩu; xong thì dọn bản nháp/khóa `mylog…` của user trên trình duyệt và tải lại trang chủ. Test: `tests/data-reset.spec.ts`.

## Xóa tài khoản

Web không có chức năng xóa tài khoản. User gửi email cho admin; API xóa vẫn còn ở backend (xem [backend/README.md](../../backend/README.md)). Đặt địa chỉ admin vào `NEXT_PUBLIC_SUPPORT_EMAIL` trong `.env.local` để tab "Dữ liệu & quyền riêng tư" ở Cài đặt hiện dòng liên hệ; không đặt thì dòng này ẩn. Không commit địa chỉ thật.

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

Trang nhật ký lưu bài viết trên backend; `localStorage` chỉ dùng cho bản nháp chưa lưu và dữ liệu cũ để nhập lại. Backend sở hữu kiểm tra an toàn bản nháp và trợ lý trang viết tại `POST /api/v1/journal-writing-suggestions`; web gọi endpoint này qua BFF (`secured-proxy.ts`) khi người dùng mở khung chat Trợ lý. Xem [AI Analysis Guide](../../backend/docs/AI_ANALYSIS_GUIDE.md) và [COMPANION_UI_PLAN.md](COMPANION_UI_PLAN.md).
