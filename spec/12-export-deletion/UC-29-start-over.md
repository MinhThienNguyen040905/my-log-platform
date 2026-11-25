# UC-29 — Bắt đầu lại

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code và quyết định ngày 2026-11-25, **chờ xác nhận**. Backend đã làm theo spec này (2026-11-25).

- **Feature:** [Xuất dữ liệu, bắt đầu lại và xóa tài khoản](README.md)
- **Catalog:** UC-29 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập, tài khoản `ACTIVE`.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có access token hợp lệ; xác nhận lại danh tính.
- **Hậu điều kiện:** nhật ký và mọi dữ liệu dẫn xuất bị xóa vĩnh viễn; hồ sơ trở về mặc định; tài khoản, email, consent và phiên đăng nhập còn nguyên; người dùng tiếp tục dùng app như tài khoản mới.

## 3. Luồng chính

1. Người dùng mở "Bắt đầu lại" trong tab "Dữ liệu" của cài đặt.
2. Đánh dấu đã hiểu, gõ cụm từ `BẮT ĐẦU LẠI`, và xác nhận danh tính:
   - tài khoản có mật khẩu: nhập mật khẩu (tối đa 5 lần sai / 15 phút);
   - tài khoản chỉ Google: phiên hiện tại phải được tạo trong vòng 10 phút.
3. `POST /me/data:reset` với `{password?, confirm: true}`.
4. Trong **một** transaction (`DataResetService.java`, `JpaAccountDataReset.java`):
   1. khóa dòng `users` để chặn ghi dữ liệu mới trong lúc xóa;
   2. đưa mọi ảnh của người dùng vào hàng đợi xóa file `asset_deletion_queue`;
   3. xóa dữ liệu (xem mục 5);
   4. đưa hồ sơ về mặc định;
   5. ghi audit `DATA_RESET` (chỉ ghi là đã reset, không ghi nội dung).
5. Trả `204`. Web hiện thông báo thành công và tải lại dữ liệu.
6. Job hàng đợi xóa file trên Cloudinary/local và thử lại cho tới khi thành công.

## 4. Luồng phụ và ngoại lệ

- Thiếu `confirm: true`: `400`.
- Sai mật khẩu: `403 PASSWORD_INCORRECT`; quá 5 lần sai trong 15 phút: `429`.
- Tài khoản Google, phiên tạo quá 10 phút trước: `403 RECENT_LOGIN_REQUIRED`; web hướng dẫn đăng xuất, đăng nhập lại rồi thử trong 10 phút.
- Lỗi DB: rollback toàn bộ, không xóa gì.
- Xóa file lỗi: file còn trong hàng đợi, thử lại với backoff; không mất dấu file.

## 5. Business rule

- **Xóa vĩnh viễn:** nhật ký (kể cả bài đã xóa mềm), ảnh, nhãn, check-in, kết quả phân tích, job AI, outbox, bản ghi chi phí AI, sự kiện safety, insight, báo cáo, kiểu suy nghĩ, mục tiêu, thói quen, lần hoàn thành, tùy chọn companion, yêu cầu export, phản hồi, idempotency key.
- **Đưa về mặc định**: hồ sơ — `displayName`, `penName`, `onboardingGoals`, `mascot` về rỗng/mặc định. Giữ `timezone`, `locale`, `onboardingCompletedAt` để người dùng không phải làm lại onboarding.
- **Giữ nguyên:** tài khoản (email, mật khẩu, trạng thái), consent, phiên đăng nhập, vai trò, audit log.
- Không có quota riêng ngoài giới hạn nhập sai mật khẩu.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/me/data:reset` | `{password?, confirm: true}` | `204` | 400, 401, 403, 429 |

## 7. Giao diện web và mobile

- **Web:** `features/user/components/StartOverCard.tsx` trong `SettingsView`; gọi qua route `app/api/me/data-reset/route.ts`.
- **Mobile:** chưa có; chờ quyết định phát hành mobile.

## 8. Tiêu chí chấp nhận

- [x] Xóa đúng dữ liệu của người dùng, không chạm dữ liệu người khác (`DataResetIntegrationTest`).
- [x] Luồng web có xác nhận, cụm từ và mật khẩu (Playwright `data-reset.spec.ts`).
- [x] Ảnh vào hàng đợi trong cùng transaction; kho lưu trữ lỗi thì file vẫn được xóa ở lần thử sau.
- [x] Hồ sơ về mặc định, giữ `timezone`, `locale`, `onboardingCompletedAt`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `DataResetIntegrationTest`, `AssetDeletionQueueJobTest` |
| Web | đã kiểm chứng | `data-reset.spec.ts` |
| Mobile | chưa làm | Chờ quyết định phát hành mobile |

## 10. Câu hỏi mở

Không có.
