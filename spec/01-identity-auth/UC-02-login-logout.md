# UC-02 — Đăng nhập và đăng xuất

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Định danh và xác thực](README.md)
- **Catalog:** UC-02 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã có tài khoản (email/mật khẩu hoặc Google).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** tài khoản `ACTIVE`, không trong thời gian khóa.
- **Hậu điều kiện (đăng nhập):** một phiên mới trong `auth_sessions` (30 ngày), client nhận access token (10 phút) và refresh token.
- **Hậu điều kiện (đăng xuất):** phiên hiện tại bị thu hồi với lý do `LOGOUT`.

## 3. Luồng chính

**Đăng nhập email/mật khẩu**

1. Người dùng nhập email, mật khẩu; client gửi kèm `deviceName`.
2. Backend kiểm tra rate limit, tài khoản, khóa tạm, mật khẩu, trạng thái (`IdentityService.java:173-195`).
3. Thành công: reset bộ đếm sai, tạo phiên, trả `{userId, accessToken, refreshToken, expiresInSeconds: 600}`.

**Đăng nhập Google**

1. Client lấy authorization code từ Google, gửi `POST /auth/google {code, timezone, locale, deviceName}`.
2. Backend đổi code lấy ID token, yêu cầu `email_verified = true` (`GoogleOAuthClient.java:112-115`).
3. Xử lý theo 3 trường hợp (`GoogleAuthProvisioningService.java:47-99`):
   - `sub` đã liên kết → đăng nhập vào tài khoản đó;
   - email chưa có tài khoản → tạo tài khoản mới (không mật khẩu), hồ sơ mặc định;
   - email đã có tài khoản → liên kết Google. Nếu tài khoản đó **chưa xác minh email**, xóa mật khẩu cũ, thu hồi mọi phiên và xóa lượt chờ (chống chiếm tài khoản trước).
4. Tạo phiên và trả token như đăng nhập thường.

**Làm mới phiên**

1. Khi access token hết hạn, client gọi `POST /auth/refresh {refreshToken}`.
2. Backend xoay refresh token, gia hạn phiên 30 ngày, cấp access token mới.

**Đăng xuất**

1. Client gọi `POST /auth/logout` kèm bearer; backend thu hồi phiên theo claim `sid`. Trả `204`.

## 4. Luồng phụ và ngoại lệ

- Sai email/mật khẩu, tài khoản không tồn tại, bị khóa, chưa `ACTIVE`, hoặc tài khoản chỉ có Google mà đăng nhập bằng mật khẩu: cùng một lỗi `401`.
- Tài khoản `DELETION_PENDING` với yêu cầu xóa còn trong thời gian chờ: xác thực đúng (mật khẩu hoặc Google) thì **hủy yêu cầu xóa**, tài khoản về `ACTIVE`, đăng nhập thành công và response có `deletionCancelled: true` *(thay đổi, xem [UC-23](../12-export-deletion/UC-23-delete-account.md))*. Mọi response đăng nhập khác có `deletionCancelled: false`.
- Sai mật khẩu lần thứ 5 → khóa 15 phút; trong thời gian khóa, cả mật khẩu đúng cũng trả `401`. Đặt lại mật khẩu (UC-03) xóa khóa.
- Google OAuth tắt (`mylog.identity.oauth.google.enabled=false`) → `404`.
- Refresh token đã dùng → thu hồi toàn bộ họ phiên, `401`.
- Refresh token hết hạn, phiên bị thu hồi hoặc tài khoản không còn `ACTIVE` → `401`.

## 5. Business rule

- Xem bảng rate limit và token ở [README](README.md#3-business-rule-và-phân-quyền-chung).
- Mỗi lần đăng nhập tạo một phiên riêng; không giới hạn số phiên đồng thời.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | `email`, `password`, `deviceName?` | `200` AuthResponse | 401, 429 |
| POST | `/api/v1/auth/google` | `code`, `timezone?`, `locale?`, `deviceName?` | `200` AuthResponse | 401, 404, 429 |
| POST | `/api/v1/auth/refresh` | `refreshToken` | `200` AuthResponse | 401 |
| POST | `/api/v1/auth/logout` | bearer | `204` | 401 |

## 7. Giao diện web và mobile

- **Web:** `app/auth/login` (`LoginForm`, nút Google 1-Tap, tùy chọn "ghi nhớ"). Token lưu trong cookie `httpOnly`; refresh cookie sống 30 ngày khi chọn ghi nhớ, nếu không là cookie phiên trình duyệt.
- **Mobile:** `(auth)/login.tsx`; refresh tự động trong `src/features/auth/api.ts`. Chưa có Google.

## 8. Tiêu chí chấp nhận

- [x] Đăng nhập, xoay refresh token và chỉ truy cập dữ liệu của mình (`registrationVerificationLoginRotationAndOwnership`).
- [x] Tài khoản chờ xác minh hoặc bị đình chỉ không nhận token (`pendingAndSuspendedAccountsCannotGetTokens`).
- [x] Sai nhiều lần thì khóa (`failedLoginsLockAccountEvenWhenRequestReturnsError`).
- [x] Google: tạo user mới, chiếm lại tài khoản chưa xác minh, từ chối đăng nhập mật khẩu với tài khoản chỉ Google, lỗi khi tắt (`GoogleAuthFlowTest`).
- [x] JWT đúng hạn và claim (`IdentityJwtTest`).
- [ ] Test riêng cho đăng xuất và phát hiện tái sử dụng refresh token qua HTTP.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `IdentityFlowIntegrationTest`, `GoogleAuthFlowTest`, `IdentityJwtTest` |
| Web | xong | Email/mật khẩu và Google |
| Mobile | đang làm | Email/mật khẩu, refresh, đăng xuất; chưa có Google |

## 10. Câu hỏi mở

- Có giới hạn số phiên đồng thời cho mỗi tài khoản không?
