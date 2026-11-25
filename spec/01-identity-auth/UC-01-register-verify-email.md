# UC-01 — Đăng ký và xác minh email

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Định danh và xác thực](README.md)
- **Catalog:** UC-01 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Khách chưa đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** email chưa thuộc tài khoản nào trong `users`.
- **Hậu điều kiện:** có bản ghi `users` trạng thái `ACTIVE`, vai trò người dùng, hồ sơ mặc định (`user_profiles`) và consent `TERMS`/`PRIVACY` đã đồng ý; lượt chờ bị xóa. Khách **chưa** được đăng nhập tự động, phải đăng nhập ở UC-02.

## 3. Luồng chính

1. Khách nhập email, mật khẩu, đồng ý điều khoản và chính sách; client gửi kèm `timezone`, `locale`, `termsVersion`, `privacyVersion`.
2. Backend kiểm tra timezone/locale hợp lệ **trước** khi gửi email, rồi tạo hoặc thay thế lượt chờ trong `pending_registrations` (`IdentityService.java:89-112`).
3. Backend gửi một email chứa cả **link** (token 32 byte, hạn 24 giờ) và **mã 8 chữ số** (hạn 10 phút). Trả `202 {registrationId, emailVerificationRequired: true}`.
4. Khách xác minh bằng một trong hai cách:
   - bấm link → `POST /auth/email-verifications:confirm {token}`;
   - nhập mã → `POST /auth/email-verifications:confirm-code {email, code}`.
5. Backend tạo tài khoản thật, gán vai trò, tạo hồ sơ mặc định và consent, xóa lượt chờ (`IdentityService.java:322-336`). Trả `204`.

## 4. Luồng phụ và ngoại lệ

- **Email đã có tài khoản:** `409 conflict` "Email đã được đăng ký."
- **Đăng ký lại khi chưa xác minh:** dùng lại `registrationId` cũ, cấp link và mã mới, link/mã cũ mất hiệu lực.
- **Gửi lại email:** `POST /auth/email-verifications {email}` luôn trả `202`, chỉ gửi khi có lượt chờ (không tiết lộ email có tồn tại hay không). Link và mã cũ bị thay.
- **Mã sai:** tăng bộ đếm; sai 5 lần thì mã khóa, phải gửi lại email. Mã hết hạn không làm link hết hạn.
- **Link đã dùng rồi:** trả `204` (idempotent) nếu token đã được tiêu thụ cho `VERIFY_EMAIL`.
- **Link/mã hết hạn hoặc không đúng:** `401 unauthorized`.
- **Timezone hoặc locale sai:** `400`, không gửi email.
- **Vượt rate limit:** `429` (xem bảng ở [README](README.md#3-business-rule-và-phân-quyền-chung)).

## 5. Business rule

- Mật khẩu đăng ký: 12–128 ký tự (xem câu hỏi mở 1 trong [README](README.md#8-câu-hỏi-mở)).
- `locale` khớp `[a-z]{2}(-[A-Z]{2})?`; `timezone` là ZoneId hợp lệ.
- `acceptTerms` và `acceptPrivacy` phải là `true`.
- Đăng ký đồng thời cùng email chỉ giữ một lượt chờ.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | xem README | `202` | 400, 409, 429 |
| POST | `/api/v1/auth/email-verifications` | `email` | `202` | 429 |
| POST | `/api/v1/auth/email-verifications:confirm` | `token` | `204` | 401 |
| POST | `/api/v1/auth/email-verifications:confirm-code` | `email`, `code` | `204` | 401, 429 |

## 7. Giao diện web và mobile

- **Web:** `app/auth/register`, `app/auth/verify-email` (nhận link và nhập mã), gọi qua route `app/api/auth/register`, `verify`, `verify-code`, `resend`.
- **Mobile:** `(auth)/register.tsx`, `(auth)/verify-code.tsx`; chỉ xác minh bằng mã, chưa xử lý link (cần Universal Links/App Links).

## 8. Tiêu chí chấp nhận

- [x] Đăng ký tạo lượt chờ, xác minh mới tạo tài khoản (`registrationVerificationLoginRotationAndOwnership`).
- [x] Mã đúng kích hoạt tài khoản và vô hiệu link (`verificationCodeActivatesAccountAndInvalidatesLink`).
- [x] Sai mã 5 lần thì khóa (`verificationCodeLocksAfterFiveWrongAttempts`).
- [x] Gửi lại thu hồi link và mã cũ (`resendingVerificationRevokesPreviousLinkAndCode`).
- [x] Mã hết hạn không làm link hết hạn (`expiredCodeDoesNotExpireTheVerificationLink`).
- [x] Lượt chờ hết hạn có thể được thay mà không tạo user (`expiredUnverifiedRegistrationCanBeReplacedWithoutCreatingAUser`).
- [x] Timezone/locale sai bị từ chối trước khi gửi email (`invalidPreferencesAreRejectedBeforeEmailIsSent`).
- [x] Đăng ký đồng thời chỉ giữ một lượt chờ (`concurrentRegistrationKeepsOnePendingAttempt`).

Các test trên nằm trong `IdentityFlowIntegrationTest`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `IdentityFlowIntegrationTest`, `SmtpVerificationDeliveryTest` |
| Web | xong | Có cả link và mã |
| Mobile | đang làm | Chỉ có mã |

## 10. Câu hỏi mở

- Mobile có cần mở link xác minh (Universal Links/App Links) hay chỉ dùng mã?
