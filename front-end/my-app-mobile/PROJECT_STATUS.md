# Tổng quan trạng thái MyLog mobile — 2026-11-15

Đối chiếu source ngày 2026-11-15. Kết quả smoke/bundle ngày 2026-10-13 được giữ riêng ở dưới; chưa chạy lại typecheck/lint/export hay kiểm thử native trong đợt rà soát mới. Xem [báo cáo repository](../../PROJECT_STATUS.md) và [khoảng thiếu web/mobile](../../backend/docs/WEB_MOBILE_GAP_IMPLEMENTATION_PLAN.md).

## Phạm vi triển khai

| Giai đoạn | Mã hiện có | Kiểm chứng | Còn mở |
| --- | --- | --- | --- |
| 0–2: nền tảng, UI, auth | API client, xác minh mã, đăng nhập, refresh, logout; màn quên/đặt lại mật khẩu bằng mã 8 chữ số | Auth cơ bản có API smoke và Expo web Playwright cũ; reset mật khẩu có ghi nhận kiểm thử đợt 1 ngày 2026-11-11 trong kế hoạch kiểm thử web/mobile | Native device; Universal Links/App Links, đổi mật khẩu trong tài khoản và Google login chưa nối; lỗi mạng/rate limit cần kiểm chứng đầy đủ |
| 3: onboarding, hồ sơ | Cập nhật hồ sơ có `If-Match`, hoàn tất onboarding, đọc/thu hồi consent | API smoke, UI Playwright; xung đột phiên bản trả `409` | Chưa đối chiếu thay đổi hồ sơ giữa web và mobile trên hai phiên đồng thời |
| 4: nhật ký | Danh sách cursor/tìm kiếm, chi tiết, editor văn bản, bản nháp theo user, CRUD, yêu thích, tạo/gắn/bỏ nhãn; đọc analysis/retry/polling và màn hỗ trợ safety | CRUD/ETag/tag có smoke/UI/bundle cũ; analysis/safety có ghi nhận kiểm thử đợt 1 ngày 2026-11-11 | Chưa có rich text, ảnh, Companion hoặc câu hỏi gợi ý. Backend đã có `GET /journal-entries/{id}/tags` nhưng mobile chưa gọi để đánh dấu nhãn đang gắn; mất mạng/xung đột native và đồng bộ web ↔ mobile chưa kiểm chứng |
| 5: mở rộng | Dashboard, lịch sử lọc theo ngày, insight/report và chi tiết report, tạo mục tiêu/thói quen và completion, quyền đồng ý, quản lý phiên và xuất CSV/PDF | API smoke cho endpoint đọc và export CSV; UI Playwright mở các màn hình, tạo mục tiêu/thói quen và yêu cầu bản CSV (bằng chứng cũ) | Check-in riêng, sửa/xóa đầy đủ goal/habit, feedback, yêu cầu xóa tài khoản, email link và chia sẻ tệp native còn mở; insight/report chỉ hiển thị dữ liệu backend đã có |

## Kiểm tra ngày 2026-11-15

- Lần gọi `./node_modules/.bin/tsc.cmd --noEmit --incremental false` không chạy được vì chưa có executable trong dependencies local; chưa có kết quả typecheck mới.
- Chưa chạy lại lint, Expo export, API smoke hoặc Expo web E2E. Docker daemon local chưa hoạt động trong đợt rà soát.
- Chưa kiểm thử Android/iOS native. Đọc source xác nhận implementation không thay thế bằng chứng chạy app.

## Bằng chứng lịch sử ngày 2026-10-13

- `npx tsc --noEmit`: đạt.
- `npm run lint`: đạt.
- `expo export --platform all`: đạt bundle Android, iOS, web; chưa chạy ứng dụng native.
- `node scripts/e2e-api-smoke.mjs`: đạt 32 bước với backend, PostgreSQL, Redis và Mailpit local; bao gồm auth, onboarding, journal CRUD, `409`, favorite, tag, dashboard, insight, report, self-care, session, export CSV và logout.
- `node scripts/e2e-expo-web.mjs`: đạt đăng ký → xác minh → đăng nhập → onboarding → tạo/yêu thích/xóa nhật ký → dashboard/insight/self-care → hồ sơ → yêu cầu export → đăng xuất. Playwright chạy Expo web, không chạy binary Android/iOS.

## Ranh giới backend và client

- `backend/`: Spring Boot sở hữu nghiệp vụ, authorization, safety, dữ liệu và API. Bài smoke lịch sử ở trên dùng backend local thật; đợt rà soát 2026-11-15 chỉ chạy 68 test backend được chọn, chưa full integration suite.
- `front-end/my-app-web/`: Next.js dùng route `/api` làm ranh giới HTTP cho web. Build/typecheck/lint đã được kiểm tra ngày 2026-11-15; không dùng kết quả web để kết luận mobile đạt kiểm thử.
- `front-end/my-app-mobile/`: các luồng trên đã được nối trực tiếp với backend. Giao diện vẫn cần kiểm tra cỡ chữ lớn, bàn phím, safe area và screen reader trên thiết bị thật.
- `safety-model/`: repository mô tả baseline có thể train; README của phần này ghi rõ chưa có model/artifact được duyệt. Mobile chỉ hiển thị trạng thái do backend trả về, không tự tạo phân tích hay nguồn hỗ trợ.

## Điểm cần ưu tiên tiếp

1. Chạy app trên Android emulator hoặc thiết bị, kiểm tra lưu bản nháp SecureStore, khôi phục phiên, navigation và bàn phím.
2. Dùng endpoint backend `GET /journal-entries/{id}/tags` để hiển thị trạng thái chọn nhãn; bổ sung check-in riêng và đổi mật khẩu trước các slice viết nâng cao.
3. Kiểm thử đồng bộ dữ liệu giữa web và mobile, cursor nhiều trang, xung đột khi hai thiết bị cùng sửa, mất mạng lúc lưu.
4. Kiểm tra tải/chia sẻ tệp export trên Android/iOS; hoàn thiện yêu cầu xóa tài khoản và email link sau khi có luồng xác nhận, hủy và cấu hình Universal Links/App Links.

Không dùng kết quả bundle hoặc Playwright web để kết luận ứng dụng Android/iOS đã sẵn sàng phát hành.
