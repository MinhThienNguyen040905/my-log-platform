# UC-22 — Xuất dữ liệu cá nhân

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**. Phần đánh dấu *(thay đổi)* khác với code hiện tại.

- **Feature:** [Xuất dữ liệu, bắt đầu lại và xóa tài khoản](README.md)
- **Catalog:** UC-22 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập, tài khoản `ACTIVE`.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có access token hợp lệ; chưa có yêu cầu xuất nào đang `PENDING`, `PROCESSING` hoặc `READY`; chưa dùng hết 3 lượt xuất trong 24 giờ.
- **Hậu điều kiện:** người dùng tải được một file CSV hoặc PDF chứa dữ liệu của chính mình. File bị xóa nội dung khi hết hạn 24 giờ.

## 3. Luồng chính

1. Người dùng chọn định dạng CSV hoặc PDF → `POST /exports` → `202`, `status = PENDING`.
2. Worker nhận job, chụp dữ liệu, tạo file, mã hóa và lưu → `READY`, `expiresAt = completedAt + 24 giờ` (`ExportService.java:90-107`).
3. Client hỏi trạng thái bằng `GET /exports/{id}` (web hỏi mỗi 5 giây trong một khoảng giới hạn).
4. Khi `READY`, người dùng xác nhận lại danh tính theo quy tắc chung ([README](README.md) mục 3: mật khẩu, hoặc đăng nhập trong 10 phút với tài khoản Google *(thay đổi)*) → `POST /exports/{id}:authorize-download` → nhận URL có chữ ký, sống **60 giây** (`ExportService.java:60-67`).
5. Client gọi URL đó kèm bearer token → nhận file `mylog-export.csv` hoặc `.pdf`.

### Nội dung file

Theo `ExportSnapshot.java:44-77`: email tài khoản, hồ sơ, consent, phiên đăng nhập, nhật ký (kể cả bài đã xóa mềm còn được giữ, tối đa 30 ngày), kết quả phân tích của bài `ANALYZED`, nhãn và liên kết nhãn, check-in, mục tiêu, lần hoàn thành thói quen, phản hồi, kiểu suy nghĩ (chỉ mã, khoảng thời gian, phản hồi; **không** có trích dẫn), insight, báo cáo.

**Không có trong file:** ảnh trong nhật ký, sự kiện safety.

## 4. Luồng phụ và ngoại lệ

- `format` khác `CSV`/`PDF`: `400`.
- Đã có yêu cầu đang hoạt động: `409`. Yêu cầu `READY` đã hết hạn được chuyển `EXPIRED` trước khi kiểm tra, nên không chặn yêu cầu mới.
- Quá 3 lần trong 24 giờ: `429`. Lượt bị tính trước khi kiểm tra `409` (`ExportController.java:32`).
- Worker lỗi: retry với backoff 30 giây × 2^lần thử (tối đa 1 giờ); sau 3 lần → `FAILED` vĩnh viễn, `lastErrorCode = EXPORT_FAILED`.
- File lớn hơn 5 MB hoặc hơn 10.000 bài nhật ký: job lỗi.
- Tài khoản không còn `ACTIVE` khi worker chạy (ví dụ vừa bị đình chỉ): job lỗi.
- Sai mật khẩu khi authorize: `403 PASSWORD_INCORRECT`; quá 5 lần sai / 15 phút: `429` *(thay đổi; hiện trả `401` và không giới hạn)*.
- Tài khoản chỉ Google *(thay đổi; hiện không authorize được)*: phiên tạo trong vòng 10 phút thì authorize không cần mật khẩu; quá 10 phút → `403 RECENT_LOGIN_REQUIRED`. Client kiểm tra `canChangePassword = false` trong `GET /me`, không hiện ô mật khẩu, và khi gặp `RECENT_LOGIN_REQUIRED` thì **hiện thông báo** yêu cầu đăng xuất, đăng nhập lại rồi tải trong 10 phút.
- URL hết 60 giây, chữ ký sai, file hết hạn, hoặc người khác gọi: `404`. Không có bearer: `401`.
- Băm SHA-256 của file sau giải mã không khớp: lỗi nội bộ, không trả file.

## 5. Business rule

- Mỗi người dùng tối đa một yêu cầu đang hoạt động (unique index `uq_export_active_user`).
- Quota: 3 yêu cầu / 24 giờ / người dùng, lưu DB nên dùng chung giữa các instance.
- File mã hóa bằng envelope encryption, giới hạn 5 MB, giữ 24 giờ (ADR-0007). Khi hết hạn, nội dung file bị xóa ngay; metadata bị xóa sau 30 ngày.
- Chữ ký URL = HMAC của `userId`, `exportId`, thời điểm hết hạn và SHA-256 của file.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/exports` | `{format}` | `202 {id, format, status, createdAt, completedAt, expiresAt}` | 400, 401, 409, 429 |
| GET | `/api/v1/exports/{id}` | — | `200` như trên | 401, 404 |
| POST | `/api/v1/exports/{id}:authorize-download` | `{password?}` | `200 {url, expiresAt}` | 401, 403, 404, 429 |
| GET | `/api/v1/exports/{id}/file` | `expires`, `signature` (query) | `200` file | 401, 404 |

## 7. Giao diện web và mobile

- **Web:** `features/export/components/AccountDataExport.tsx` trong tab "Dữ liệu" của cài đặt; tải file qua route `app/api/backend/export-download/route.ts`.
- **Mobile:** `(app)/settings.tsx`, nút "CSV"/"PDF", làm mới trạng thái thủ công, nhập mật khẩu để tải; nhớ id export gần nhất theo user.

## 8. Tiêu chí chấp nhận

- [x] File được mã hóa khi lưu; người khác không xem hay tải được (`M7IntegrationTest.exportIsEncryptedOwnerScopedAndGrantExpires`).
- [x] Tải file qua HTTP: không có token → 401, người khác → 404, chủ sở hữu → 200 và `no-store`.
- [x] Chữ ký hết hạn, sai mật khẩu, file hết hạn đều bị từ chối; yêu cầu hết hạn không chặn yêu cầu mới; PDF hợp lệ.
- [x] Quota dùng chung, tính theo user và hành động (`writeQuotaPersistsAfterRejectionAndIsScopedByActionAndUser`).
- [ ] Test HTTP cho `POST /exports`, `GET /exports/{id}`, `:authorize-download`, và `429`.
- [ ] Playwright cho luồng xuất trên web.
- [ ] Kiểm tra tải và chia sẻ file trên Android/iOS thật.
- [ ] Tài khoản Google đăng nhập trong 10 phút tải được file; quá 10 phút → `403 RECENT_LOGIN_REQUIRED`; sai mật khẩu → `403`, quá số lần → `429`.
- [ ] Web và mobile không hiện ô mật khẩu cho tài khoản Google và hiện thông báo khi gặp `RECENT_LOGIN_REQUIRED`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M7IntegrationTest`; một phần chỉ test ở mức service |
| Web | đang làm | Chưa xử lý tài khoản Google; chưa có Playwright |
| Mobile | đang làm | Chưa xử lý tài khoản Google; chưa chạy trên thiết bị thật |

## 10. Câu hỏi mở

1. File có chứa **bài nhật ký đã xóa mềm** (test khẳng định điều này). Người dùng đã xóa bài có mong thấy nó trong file không?
2. File **không có ảnh** trong nhật ký. Có cần đưa ảnh (hoặc link) vào không?
3. Lượt quota bị tính cả khi yêu cầu bị từ chối vì `409`. Có cần đổi thứ tự không?
