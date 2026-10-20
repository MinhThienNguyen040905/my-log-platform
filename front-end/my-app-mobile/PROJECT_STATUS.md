# Tổng quan trạng thái MyLog mobile — 2026-10-13

## Phạm vi triển khai

| Giai đoạn | Mã hiện có | Kiểm chứng | Còn mở |
| --- | --- | --- | --- |
| 0–2: nền tảng, UI, auth | API client, xác minh mã, đăng nhập, refresh, logout | API smoke và Expo web Playwright với backend local | Native device; lỗi mạng và giới hạn tốc độ chưa được kiểm thử UI đầy đủ |
| 3: onboarding, hồ sơ | Cập nhật hồ sơ có `If-Match`, hoàn tất onboarding, đọc/thu hồi consent | API smoke, UI Playwright; xung đột phiên bản trả `409` | Chưa đối chiếu thay đổi hồ sơ giữa web và mobile trên hai phiên đồng thời |
| 4: nhật ký | Danh sách cursor, chi tiết, editor văn bản, bản nháp theo user, CRUD, yêu thích, tạo/gắn/bỏ nhãn | API smoke CRUD/ETag/tag; UI Playwright tạo, yêu thích và xóa; Android/web bundle | Chưa có rich text, ảnh; backend chưa trả danh sách nhãn gắn với từng bài nên UI chưa thể đánh dấu trạng thái nhãn; chưa thử mất mạng/xung đột trên native; chưa kiểm tra đồng bộ trực tiếp web ↔ mobile |
| 5: mở rộng | Dashboard, lịch sử lọc theo ngày, insight/report và chi tiết report, self-care mục tiêu/thói quen, quyền đồng ý, quản lý phiên và xuất CSV/PDF | API smoke cho endpoint đọc và export CSV; UI Playwright mở các màn hình, tạo mục tiêu/thói quen và yêu cầu bản CSV | Yêu cầu xóa tài khoản, email link, kiểm chứng chia sẻ tệp native còn mở; insight/report chỉ hiển thị dữ liệu backend đã có |

## Kết quả kiểm tra trong phiên này

- `npx tsc --noEmit`: đạt.
- `npm run lint`: đạt.
- `expo export --platform all`: đạt bundle Android, iOS, web; chưa chạy ứng dụng native.
- `node scripts/e2e-api-smoke.mjs`: đạt 32 bước với backend, PostgreSQL, Redis và Mailpit local; bao gồm auth, onboarding, journal CRUD, `409`, favorite, tag, dashboard, insight, report, self-care, session, export CSV và logout.
- `node scripts/e2e-expo-web.mjs`: đạt đăng ký → xác minh → đăng nhập → onboarding → tạo/yêu thích/xóa nhật ký → dashboard/insight/self-care → hồ sơ → yêu cầu export → đăng xuất. Playwright chạy Expo web, không chạy binary Android/iOS.

## Tổng quan repository

- `backend/`: Spring Boot sở hữu nghiệp vụ, authorization, safety, dữ liệu và API. Bài smoke ở trên đã dùng backend local thật; chưa chạy toàn bộ test suite backend trong phiên này.
- `front-end/my-app-web/`: Next.js dùng route `/api` làm ranh giới HTTP cho web. Không thay đổi web trong phiên này; chưa chạy build/test web trong phiên này.
- `front-end/my-app-mobile/`: các luồng trên đã được nối trực tiếp với backend. Giao diện vẫn cần kiểm tra cỡ chữ lớn, bàn phím, safe area và screen reader trên thiết bị thật.
- `safety-model/`: repository mô tả baseline có thể train; README của phần này ghi rõ chưa có model/artifact được duyệt. Mobile chỉ hiển thị trạng thái do backend trả về, không tự tạo phân tích hay nguồn hỗ trợ.

## Điểm cần ưu tiên tiếp

1. Chạy app trên Android emulator hoặc thiết bị, kiểm tra lưu bản nháp SecureStore, khôi phục phiên, navigation và bàn phím.
2. Bổ sung contract backend để đọc nhãn đã gắn của từng bài trước khi làm UI nhãn có trạng thái chọn chính xác.
3. Kiểm thử đồng bộ dữ liệu giữa web và mobile, cursor nhiều trang, xung đột khi hai thiết bị cùng sửa, mất mạng lúc lưu.
4. Kiểm tra tải/chia sẻ tệp export trên Android/iOS; hoàn thiện yêu cầu xóa tài khoản và email link sau khi có luồng xác nhận, hủy và cấu hình Universal Links/App Links.

Không dùng kết quả bundle hoặc Playwright web để kết luận ứng dụng Android/iOS đã sẵn sàng phát hành.
