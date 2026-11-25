# UC-23 — Xóa tài khoản

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** thiết kế theo quyết định ngày 2026-11-25, **chờ xác nhận**. Phần đánh dấu *(thay đổi)* khác với code hiện tại và sẽ được sửa trong PR backend/web.

- **Feature:** [Xuất dữ liệu, bắt đầu lại và xóa tài khoản](README.md)
- **Catalog:** UC-23 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

- Người dùng đã đăng nhập, tài khoản `ACTIVE` (tạo yêu cầu).
- Người dùng có tài khoản đang chờ xóa (hủy bằng cách đăng nhập lại).
- Worker hệ thống (thực hiện xóa).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** chức năng đang bật (`mylog.account-deletion.enabled=true`); đã đăng nhập; xác nhận lại danh tính; chưa có yêu cầu xóa đang hoạt động.
- **Hậu điều kiện:**
  - Ngay khi yêu cầu: tài khoản `DELETION_PENDING`, mọi phiên bị thu hồi, người dùng nhận email xác nhận kèm ngày sẽ xóa.
  - Sau 7 ngày nếu không đăng nhập lại: ảnh bị xóa khỏi kho lưu trữ, mọi dữ liệu của người dùng trong DB bị xóa, người dùng nhận email "đã xóa xong".
  - Nếu đăng nhập lại trong 7 ngày: yêu cầu bị hủy, tài khoản `ACTIVE`, người dùng vào app bình thường và thấy thông báo đã hủy.

## 3. Luồng chính

### Yêu cầu xóa

1. Người dùng mở "Xóa tài khoản" trong cài đặt, đánh dấu đã hiểu, gõ cụm từ xác nhận và xác nhận danh tính:
   - tài khoản có mật khẩu: nhập mật khẩu (tối đa 5 lần sai / 15 phút);
   - tài khoản chỉ Google: phiên hiện tại phải được tạo trong vòng 10 phút *(thay đổi)*.
2. `POST /account-deletion-requests` → `202`, `status = GRACE_PERIOD`, `scheduledFor = requestedAt + 7 ngày`.
3. Backend chuyển tài khoản sang `DELETION_PENDING`, thu hồi mọi phiên (lý do `ACCOUNT_DELETION`) và gửi email xác nhận *(thay đổi)*.
4. Client đăng xuất và hiện: tài khoản sẽ bị xóa vào ngày `scheduledFor`; đăng nhập lại trước ngày đó để hủy.

### Hủy bằng cách đăng nhập lại *(thay đổi)*

5. Trong thời gian chờ, người dùng đăng nhập bằng mật khẩu hoặc Google như bình thường.
6. Nếu tài khoản `DELETION_PENDING` và yêu cầu còn ở `GRACE_PERIOD` với `scheduledFor` chưa tới: backend hủy yêu cầu (`CANCELLED`), chuyển tài khoản về `ACTIVE`, ghi audit `ACCOUNT_DELETION_CANCELLED`, cấp phiên như đăng nhập thường và trả thêm `deletionCancelled: true`.
7. Client hiện thông báo "Yêu cầu xóa tài khoản đã được hủy".

### Worker xóa

8. Hết 7 ngày, worker nhận yêu cầu (`FOR UPDATE SKIP LOCKED`, lease 5 phút), chuyển `PROCESSING`.
9. Trong **một** transaction *(thay đổi)*:
   1. đọc và giải mã email để gửi thư sau cùng;
   2. đưa mọi ảnh của người dùng vào hàng đợi xóa file (`asset_deletion_queue`, xem [README](README.md) mục 5);
   3. purge DB: outbox của bài nhật ký, bản ghi chi phí AI, sự kiện safety, audit do chính người dùng thực hiện, dòng `users` và mọi bảng `ON DELETE CASCADE`;
   4. đánh dấu yêu cầu `COMPLETED`, set `user_id = NULL`.
10. Sau commit, gửi email "đã xóa xong" tới địa chỉ đã đọc ở bước 9.1 *(thay đổi)*. Email không có nội dung nhật ký.
11. Job hàng đợi xóa file trên Cloudinary/local và thử lại cho tới khi thành công.

## 4. Luồng phụ và ngoại lệ

- Chức năng tắt (`mylog.account-deletion.enabled=false`, **mặc định**): mọi endpoint `/account-deletion-requests*` trả `404`, worker không chạy *(thay đổi)*.
- Sai mật khẩu: `403 PASSWORD_INCORRECT`; quá 5 lần sai: `429`. Tài khoản Google chưa đăng nhập lại trong 10 phút: `403 RECENT_LOGIN_REQUIRED` *(thay đổi; hiện mọi trường hợp đều trả `401` và không giới hạn số lần sai)*.
- Đã có yêu cầu đang hoạt động: `409`.
- Đăng nhập khi yêu cầu đã `PROCESSING`/`FAILED` hoặc đã quá `scheduledFor`: `401` như tài khoản không hoạt động; không hủy được nữa.
- Worker lỗi DB: rollback, yêu cầu `FAILED`, retry với backoff 60 giây × 2^lần thử (tối đa 24 giờ). Sau 10 lần thử vẫn lỗi: giữ `FAILED`, `lastErrorCode = DELETION_STUCK`, hiện ở màn hình vận hành ([UC-25](../14-admin/UC-25-operations-failed-jobs.md)) *(thay đổi)*.
- Gửi email lỗi: không chặn việc xóa; chỉ ghi log (không ghi địa chỉ email).
- Xóa file lỗi: không ảnh hưởng yêu cầu xóa; hàng đợi tự thử lại.

## 5. Business rule

- Thời gian chờ 7 ngày; mỗi người dùng tối đa một yêu cầu `GRACE_PERIOD`/`PROCESSING`/`FAILED`.
- Audit *(thay đổi)*: xóa audit có `actor_user_id` = người dùng và `actor_type = USER`. **Giữ** audit do admin hoặc hệ thống thực hiện trên tài khoản (ví dụ `USER_SUSPENDED`); `target_id` còn lại chỉ là một UUID không còn trỏ tới ai.
- Dòng `deletion_requests` giữ lại với `user_id = NULL` và `subject_hash`, xóa sau 365 ngày.
- Không có cache nào cần xóa: backend không dùng Redis cho dữ liệu người dùng.
- Dữ liệu đã gửi provider AI: không có API xóa trong code. Provider chỉ được duyệt khi không lưu dữ liệu (zero retention) hoặc có cơ chế xóa (xem [05-ai-analysis](../05-ai-analysis.md)).
- Endpoint `:status` và `:cancel` bị bỏ *(thay đổi)*, thay bằng hủy khi đăng nhập.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/account-deletion-requests` | bearer, `{password?}` | `202 {id, status, requestedAt, scheduledFor, completedAt}` | 401, 403, 404 khi tắt, 409, 429 |
| POST | `/api/v1/auth/login`, `/api/v1/auth/google` | như [UC-02](../01-identity-auth/UC-02-login-logout.md) | thêm `deletionCancelled: boolean` *(thay đổi)* | như UC-02 |

## 7. Giao diện web và mobile

- **Web** *(thay đổi)*: thẻ "Xóa tài khoản" trong tab "Dữ liệu" của cài đặt, theo mẫu `StartOverCard.tsx` (đánh dấu đã hiểu, gõ cụm từ, mật khẩu hoặc yêu cầu đăng nhập lại với Google). Chỉ hiện khi backend bật chức năng. Sau khi gửi: đăng xuất, hiện ngày xóa. Sau khi đăng nhập với `deletionCancelled: true`: hiện thông báo. Bỏ dòng "xóa tài khoản qua email".
- **Mobile:** chờ quyết định phát hành trên store (Apple và Google Play bắt buộc có xóa tài khoản trong app).

## 8. Tiêu chí chấp nhận

- [x] Yêu cầu xóa đổi trạng thái tài khoản và thu hồi phiên; worker purge dữ liệu (`M7IntegrationTest`).
- [ ] Cờ tắt → endpoint `404`, worker không chạy.
- [ ] HTTP: sai mật khẩu `403`, quá số lần `429`, Google chưa đăng nhập lại `403`, đã có yêu cầu `409`.
- [ ] Đăng nhập lại (mật khẩu và Google) trong thời gian chờ hủy yêu cầu và trả `deletionCancelled: true`; sau `scheduledFor` thì không hủy được.
- [ ] Audit của admin trên tài khoản còn lại sau khi xóa; audit của người dùng bị xóa.
- [ ] Ảnh được đưa vào hàng đợi trong cùng transaction; lỗi kho lưu trữ không làm mất việc xóa file.
- [ ] Email xác nhận và email hoàn tất được gửi; lỗi gửi không chặn việc xóa.
- [ ] Web: Playwright cho yêu cầu xóa và thông báo hủy.
- [ ] Kiểm tra với Cloudinary thật trên staging.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đang làm | Luồng cơ bản đã kiểm chứng; các phần *(thay đổi)* chưa làm |
| Web | chưa làm | Chỉ có link email hỗ trợ |
| Mobile | chưa làm | Chờ quyết định phát hành trên store |

## 10. Câu hỏi mở

1. Mobile có làm trong bản đầu không (phụ thuộc việc phát hành trên store)?
