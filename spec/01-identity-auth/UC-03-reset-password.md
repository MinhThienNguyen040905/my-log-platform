# UC-03 — Lấy lại mật khẩu

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Định danh và xác thực](README.md)
- **Catalog:** UC-03 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

File này gồm hai luồng: **quên mật khẩu** (chưa đăng nhập) và **đổi mật khẩu** (đã đăng nhập, chưa có UC riêng trong catalog).

## 1. Tác nhân

- Quên mật khẩu: khách không nhớ mật khẩu.
- Đổi mật khẩu: người dùng đã đăng nhập, tài khoản có mật khẩu.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** tài khoản tồn tại, không `SUSPENDED`/`DELETED`, có mật khẩu.
- **Hậu điều kiện (quên mật khẩu):** mật khẩu mới được lưu; email được đánh dấu đã xác minh nếu chưa; khóa đăng nhập được xóa; **mọi phiên bị thu hồi**; ghi audit `PASSWORD_RESET`; gửi email thông báo bảo mật.
- **Hậu điều kiện (đổi mật khẩu):** mật khẩu mới được lưu; các phiên **khác** bị thu hồi (giữ phiên hiện tại; nếu không xác định được phiên hiện tại thì thu hồi tất cả); ghi audit `PASSWORD_CHANGED`; gửi email thông báo.

## 3. Luồng chính

**Quên mật khẩu** (`IdentityService.java:416-572`)

1. Khách nhập email → `POST /auth/password-resets`. Luôn trả `202` với cùng một thông điệp.
2. Nếu tài khoản hợp lệ: thu hồi token reset cũ, tạo token link (hạn 1 giờ) và mã 8 số (hạn 15 phút), gửi email.
3. Khách đặt mật khẩu mới bằng link (`:confirm {token, newPassword}`) hoặc bằng mã (`:confirm-code {email, code, newPassword}`).
4. Backend tiêu thụ token/mã một lần, kiểm tra chính sách mật khẩu, lưu mật khẩu và thu hồi phiên. Trả `204`.

**Đổi mật khẩu** (`IdentityService.java:351-414`)

1. Người dùng nhập mật khẩu hiện tại và mật khẩu mới → `POST /me/password`.
2. Backend kiểm tra mật khẩu hiện tại, chính sách mật khẩu, lưu và thu hồi các phiên khác. Trả `204`.

## 4. Luồng phụ và ngoại lệ

- Email không tồn tại, tài khoản bị đình chỉ hoặc đã xóa: vẫn trả `202`, không gửi email.
- Tài khoản chỉ có Google: vẫn trả `202`, phát sự kiện `OAuthPasswordResetAttemptEvent` (gửi email hướng dẫn đăng nhập Google) thay vì tạo token.
- Token/mã sai, hết hạn hoặc đã dùng: `401`. Mã hết hạn trước link; link vẫn dùng được trong 1 giờ.
- Đổi mật khẩu với tài khoản chỉ Google: `400` mã `OAUTH_ONLY_ACCOUNT`.
- Mã lỗi `400` của chính sách mật khẩu: `CURRENT_PASSWORD_INCORRECT`, `NEW_PASSWORD_SAME_AS_OLD`, `PASSWORD_LENGTH_INVALID`, `PASSWORD_CONTAINS_USER_INFO`, `ACCOUNT_NOT_ACTIVE`.
- Đổi mật khẩu sai mật khẩu hiện tại 5 lần trong 15 phút: `429`.

## 5. Business rule

- Mỗi tài khoản chỉ có một token reset còn hiệu lực.
- Mật khẩu mới: 12–72 byte UTF-8, khác mật khẩu cũ, không chứa phần trước `@` của email (khi phần đó ≥ 4 ký tự).
- Rate limit: xem [README](README.md#3-business-rule-và-phân-quyền-chung).

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/auth/password-resets` | `email` | `202 {message}` | 429 |
| POST | `/api/v1/auth/password-resets:confirm` | `token`, `newPassword` | `204` | 400, 401, 429 |
| POST | `/api/v1/auth/password-resets:confirm-code` | `email`, `code`, `newPassword` | `204` | 400, 401, 429 |
| POST | `/api/v1/me/password` | `currentPassword`, `newPassword` | `204` | 400, 401, 429 |

## 7. Giao diện web và mobile

- **Web:** `app/auth/forgot-password`, `app/auth/reset-password` (link và mã); đổi mật khẩu trong cài đặt (`features/user/components/ChangePasswordForm.tsx`).
- **Mobile:** `(auth)/forgot-password.tsx`, `(auth)/reset-password.tsx` (chỉ mã). Chưa có màn hình đổi mật khẩu.

## 8. Tiêu chí chấp nhận

- [x] Link và mã hết hạn bị từ chối; mã hết hạn trước link (`PasswordResetExpiryIntegrationTest`).
- [x] Các nhánh reset: chống dò, tài khoản Google, token một lần, thu hồi phiên (`PasswordResetUnitTest`, `IdentityControllerPasswordResetTest`).
- [x] Đổi mật khẩu: thành công, sai mật khẩu cũ, trùng cũ, chứa email, quá ngắn, tài khoản Google, rate limit, chưa đăng nhập (`ChangePasswordIntegrationTest`, `ChangePasswordUnitTest`).
- [x] Web: luồng reset và rate limit (`password-reset.spec.ts`, `password-reset-rate-limit.spec.ts`).

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Các test ở mục 8 |
| Web | đã kiểm chứng | Playwright mục 8 |
| Mobile | đang làm | Reset bằng mã có script `scripts/e2e-password-reset.mjs`; chưa có đổi mật khẩu |

## 10. Câu hỏi mở

- Có tách "Đổi mật khẩu" thành UC riêng (UC-29) trong catalog không?
