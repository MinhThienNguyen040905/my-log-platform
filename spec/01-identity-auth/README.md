# 01 — Định danh và xác thực

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Đăng ký, xác minh email, đăng nhập (email/mật khẩu hoặc Google), làm mới phiên, đăng xuất, quên mật khẩu, đổi mật khẩu, quản lý phiên.

- **Module backend:** `com.mylog.identity` (phiên và đổi mật khẩu có endpoint trong `com.mylog.user`)
- **API gốc:** `/api/v1/auth`, `/api/v1/me/sessions`, `/api/v1/me/password`
- **Web:** [`features/auth`](../../front-end/my-app-web/features/auth), [`app/auth`](../../front-end/my-app-web/app/auth), [`app/api/auth`](../../front-end/my-app-web/app/api/auth)
- **Mobile:** [`src/features/auth`](../../front-end/my-app-mobile/src/features/auth), [`src/app/(auth)`](<../../front-end/my-app-mobile/src/app/(auth)>)
- **Migration:** `V2`, `V4`, `V16`, `V17`, `V19` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`adr/0001-authentication-and-session.md`](../../backend/docs/adr/0001-authentication-and-session.md)

## 1. Mục tiêu và phạm vi

Cho phép một người tạo tài khoản cá nhân, chứng minh sở hữu email và truy cập mylog an toàn trên nhiều thiết bị.

- **Trong phạm vi:** đăng ký 2 bước (lượt chờ → xác minh), đăng nhập email/mật khẩu, đăng nhập Google, access/refresh token, đăng xuất, quên và đặt lại mật khẩu, đổi mật khẩu khi đã đăng nhập, xem và thu hồi phiên.
- **Ngoài phạm vi:** MFA, đăng nhập bằng nhà cung cấp khác Google, đổi email. Xóa tài khoản thuộc [12-export-deletion](../12-export-deletion/README.md).

## 2. Use case

- [UC-01 — Đăng ký và xác minh email](UC-01-register-verify-email.md)
- [UC-02 — Đăng nhập và đăng xuất](UC-02-login-logout.md) (gồm Google và refresh)
- [UC-03 — Lấy lại mật khẩu](UC-03-reset-password.md) (gồm đổi mật khẩu khi đã đăng nhập)
- [UC-06 — Xem và thu hồi phiên đăng nhập](UC-06-sessions.md)

## 3. Business rule và phân quyền chung

- **Chuẩn hóa email:** trim, chữ thường, tối đa 254 ký tự. Email không lưu dạng rõ: tra cứu bằng HMAC (`lookupHash`), lưu bản mã hóa (`IdentityService.java:78-87`).
- **Chống dò tài khoản:** đăng nhập sai, tài khoản không tồn tại, bị khóa, chưa `ACTIVE` đều trả cùng lỗi `401 unauthorized` "Thông tin xác thực không hợp lệ", và vẫn chạy một phép so khớp mật khẩu giả để thời gian phản hồi tương đương (`IdentityService.java:180-188`).
- **Trạng thái tài khoản** (`users.status`): `ACTIVE`, `SUSPENDED`, `DELETION_PENDING`, `DELETED`. Chỉ `ACTIVE` được cấp và làm mới token.
- **Token:**
  - Access token là JWT, sống 10 phút, có claim `sid` = id phiên (`IdentityJwt.java:74`). API trả `expiresInSeconds = 600`.
  - Refresh token là chuỗi ngẫu nhiên 32 byte, chỉ lưu hash; phiên sống 30 ngày, mỗi lần refresh xoay token mới.
  - Dùng lại refresh token cũ → thu hồi cả "họ" phiên (`TOKEN_REUSE`) và ghi audit `REFRESH_REUSE_REVOKED` (`IdentityService.java:198-215`).
- **Rate limit** (cửa sổ 15 phút, lưu DB, dùng chung giữa các instance):

  | Hành động | Theo IP | Theo email |
  |---|---|---|
  | Đăng ký | 20 | 5 |
  | Gửi lại email xác minh | 10 | — |
  | Xác minh bằng mã | 30 | 10 |
  | Đăng nhập | 30 | 10 |
  | Google | 30 | 10 theo Google `sub` |
  | Yêu cầu reset mật khẩu | 10 | 5 (và 10/ngày) |
  | Xác nhận reset | 15 | 5 với mã |
  | Đổi mật khẩu sai | — | 5 lần sai/15 phút theo user |

  Giá trị reset lấy từ `mylog.identity.password-reset.*`, mặc định ở `MylogProperties.java:53`.
- **Khóa tạm:** sai mật khẩu 5 lần liên tiếp → khóa 15 phút (`IdentityService.java:183-186`).
- **Chính sách mật khẩu:**
  - Đăng ký: 12–128 ký tự (validate DTO).
  - Đặt lại và đổi mật khẩu: 12–72 **byte** UTF-8, không được trùng mật khẩu cũ, không chứa phần trước `@` của email nếu phần đó dài ≥ 4 ký tự (`IdentityService.java:581-594`).
- **Dọn dữ liệu hết hạn:** job `IdentityCleanup` chạy 03:15 UTC hằng ngày (`IdentityCleanup.java:19`).
- **Phân quyền:** các endpoint `/auth/*` công khai, trừ `/auth/logout`. Endpoint `/me/*` cần bearer token và luôn lọc theo user hiện tại.

### Chức năng ngoài catalog

- `GET /internal/dev/users`: liệt kê tài khoản cho môi trường dev (`DevAccountController`). Chỉ dùng local, không thuộc sản phẩm.

## 4. API contract

| Method | Path | Request | Response | Lỗi chính |
|---|---|---|---|---|
| POST | `/auth/register` | `email`, `password`, `timezone`, `locale`, `termsVersion`, `privacyVersion`, `acceptTerms=true`, `acceptPrivacy=true` | `202` `{registrationId, emailVerificationRequired: true}` | 400, 409 email đã có, 429 |
| POST | `/auth/email-verifications` | `email` | `202` (luôn, kể cả email không có lượt chờ) | 429 |
| POST | `/auth/email-verifications:confirm` | `token` | `204` | 401 |
| POST | `/auth/email-verifications:confirm-code` | `email`, `code` (8 số) | `204` | 401, 429 |
| POST | `/auth/login` | `email`, `password`, `deviceName?` | `200` `{userId, accessToken, refreshToken, expiresInSeconds}` | 401, 429 |
| POST | `/auth/google` | `code`, `timezone?`, `locale?`, `deviceName?` | `200` như login | 401, 404 khi Google tắt, 429 |
| POST | `/auth/refresh` | `refreshToken` | `200` như login | 401 |
| POST | `/auth/logout` | bearer | `204` | 401 |
| POST | `/auth/password-resets` | `email` | `202` `{message}` (luôn cùng thông điệp) | 429 |
| POST | `/auth/password-resets:confirm` | `token`, `newPassword` | `204` | 400 mã `PASSWORD_*`, 401, 429 |
| POST | `/auth/password-resets:confirm-code` | `email`, `code`, `newPassword` | `204` | 400, 401, 429 |
| POST | `/me/password` | `currentPassword`, `newPassword` | `204` | 400 mã `PasswordChangeException`, 401, 429 |
| GET | `/me/sessions` | bearer | `200` `[{id, deviceName, createdAt, lastUsedAt, expiresAt, current}]` | 401 |
| DELETE | `/me/sessions/{sessionId}` | bearer | `204` | 404 |
| DELETE | `/me/sessions?exceptCurrent=true` | bearer | `204` | 400 khi JWT không có `sid` |

Đường dẫn trên tính từ `/api/v1`. Lỗi trả `application/problem+json` theo `platform/web/ErrorCode.java`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `pending_registrations` | Lượt đăng ký chờ xác minh: email mã hóa, hash mật khẩu, hash token/mã, hạn, số lần nhập mã sai | V17 |
| `users` | Tài khoản: email mã hóa + HMAC, hash mật khẩu (null với tài khoản chỉ Google), `status`, `failed_logins`, `locked_until`, `email_verified_at`, `auth_provider` | V2, V17, V19 |
| `user_oauth_identities` | Liên kết `provider` + `sub` với user | V19 |
| `roles`, `permissions`, `role_permissions`, `user_roles` | RBAC; user mới được gán vai trò người dùng | V2 |
| `auth_sessions` | Phiên: hash refresh token, `family_id`, `device_name`, hạn, thu hồi | V2 |
| `auth_refresh_history` | Token đã dùng để phát hiện tái sử dụng | V2, V4 |
| `auth_action_tokens` | Token/mã một lần cho `VERIFY_EMAIL`, `RESET_PASSWORD` | V2, V16 |
| `auth_rate_limits` | Bộ đếm rate limit | V2 |
| `audit_logs` | Sự kiện bảo mật (đăng nhập Google, đổi/đặt lại mật khẩu, thu hồi phiên...) | V2 |

## 6. Safety, quyền riêng tư và consent

- Không log email, token, mật khẩu. `ChangePasswordRequest.toString()` che mật khẩu; test `authSecretsNeverAppearInApplicationLogs` kiểm tra log.
- Đăng ký ghi consent `TERMS` và `PRIVACY` theo phiên bản người dùng chấp nhận; các consent khác mặc định tắt (xem [02-profile-consent](../02-profile-consent/README.md)).
- Email thông báo bảo mật được gửi khi đổi hoặc đặt lại mật khẩu (`PasswordChangedEvent`).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `IdentityFlowIntegrationTest`, `GoogleAuthFlowTest`, `PasswordResetUnitTest`, `PasswordResetExpiryIntegrationTest`, `IdentityControllerPasswordResetTest`, `ChangePasswordIntegrationTest`, `IdentityJwtTest` |
| Web | xong | Đủ luồng đăng ký, xác minh (link và mã), đăng nhập, Google, quên/đặt lại mật khẩu, đổi mật khẩu, phiên. Token lưu cookie `httpOnly` (`features/auth/api/session.ts`). Playwright: `password-reset.spec.ts`, `password-reset-rate-limit.spec.ts` |
| Mobile | đang làm | Có đăng ký, xác minh bằng mã, đăng nhập, refresh, đăng xuất, quên/đặt lại bằng mã, phiên. Chưa có Google, xác minh bằng link, đổi mật khẩu. Chưa chạy trên thiết bị thật |

## 8. Câu hỏi mở

1. Giới hạn mật khẩu không thống nhất: đăng ký cho phép 12–128 **ký tự**, còn đặt lại/đổi mật khẩu là 12–72 **byte**; đăng ký cũng không chặn mật khẩu chứa phần email. Mật khẩu dài hơn 72 byte khi đăng ký có thể bị bcrypt cắt bớt. Chốt một chính sách chung?
2. Tài khoản tạo qua Google ghi consent `TERMS`/`PRIVACY` với phiên bản cứng `"v1"` (`GoogleAuthProvisioningService.java:74`), không theo phiên bản người dùng thật sự đã xem.
3. Đổi mật khẩu khi đã đăng nhập chưa có UC riêng trong catalog; hiện tạm đặt trong UC-03.
