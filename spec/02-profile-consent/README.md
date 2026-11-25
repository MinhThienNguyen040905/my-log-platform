# 02 — Hồ sơ và đồng ý xử lý dữ liệu

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Thiết lập ban đầu (onboarding), hồ sơ cá nhân và các lựa chọn đồng ý (consent) cho việc xử lý dữ liệu.

- **Module backend:** `com.mylog.user`
- **API gốc:** `/api/v1/me`, `/api/v1/me/consents`
- **Web:** [`features/onboarding`](../../front-end/my-app-web/features/onboarding), [`features/user`](../../front-end/my-app-web/features/user), [`app/api/me`](../../front-end/my-app-web/app/api/me)
- **Mobile:** [`src/features/user`](../../front-end/my-app-mobile/src/features/user), [`src/app/(onboarding)`](<../../front-end/my-app-mobile/src/app/(onboarding)>)
- **Migration:** `V3` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** —

## 1. Mục tiêu và phạm vi

Cho người dùng đặt cách được gọi, mục tiêu khi dùng mylog, múi giờ, ngôn ngữ, và quyết định rõ ràng dữ liệu của mình được xử lý thế nào.

- **Trong phạm vi:** đọc/sửa hồ sơ, hoàn tất onboarding, đọc và thay đổi consent.
- **Ngoài phạm vi:** đổi email, ảnh đại diện. Phiên và đổi mật khẩu thuộc [01-identity-auth](../01-identity-auth/README.md). "Bắt đầu lại" (`POST /me/data:reset`, đưa hồ sơ về mặc định) và xóa tài khoản thuộc [12-export-deletion](../12-export-deletion/README.md).

## 2. Use case

- [UC-04 — Hoàn thành thiết lập ban đầu và sửa hồ sơ](UC-04-onboarding-profile.md)
- [UC-05 — Chọn hoặc rút lại sự đồng ý xử lý dữ liệu](UC-05-data-consent.md)

## 3. Business rule và phân quyền chung

- Mọi endpoint `/me*` cần bearer token và chỉ thao tác trên hồ sơ của user hiện tại.
- Hồ sơ có `version`; sửa hồ sơ và hoàn tất onboarding bắt buộc header `If-Match: "<version>"`. Sai version → `409 conflict` "Hồ sơ đã được cập nhật ở nơi khác."
- `displayName`, `penName`, `onboardingGoals`, `mascot` được **mã hóa** chung trong một payload (`user_profiles.profile`); `timezone`, `locale` lưu rõ để dùng tính ngày.
- Hồ sơ mặc định được tạo khi tài khoản được tạo (xác minh email ở UC-01 hoặc lần đầu đăng nhập Google), cùng với consent ban đầu (`UserProfileService.java:30-40`).
- `ProfileResponse` có thêm: `isAdmin` (user có vai trò `SYSTEM_ADMIN`, `SAFETY_ADMIN`, `SUPPORT_AGENT` hoặc quyền `admin:dashboard`), `canChangePassword` (tài khoản có mật khẩu), `email` (đã giải mã, chỉ trả cho chính chủ).

## 4. API contract

| Method | Path | Request | Response | Lỗi chính |
|---|---|---|---|---|
| GET | `/me` | — | `200` ProfileResponse, header `ETag` | 401, 404 |
| PATCH | `/me` | `If-Match`; `displayName?`, `penName?`, `onboardingGoals?`, `timezone?`, `locale?`, `mascot?` | `200` ProfileResponse, `ETag` mới | 400, 409 |
| POST | `/me/onboarding:complete` | `If-Match` | `200` ProfileResponse | 400, 409 |
| GET | `/me/consents` | — | `200 [{type, documentVersion, granted, decidedAt}]` | 401 |
| PUT | `/me/consents/{type}` | `documentVersion`, `granted` | `204` | 400 |

ProfileResponse: `{userId, displayName, penName, onboardingGoals[], timezone, locale, onboardingCompletedAt, version, isAdmin, canChangePassword, mascot, email}`. Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `user_profiles` | Một dòng/user: payload mã hóa, `timezone`, `locale`, `onboarding_completed_at`, `version` | V3 |
| `user_consents` | Lịch sử quyết định consent (chỉ thêm, không sửa): `type`, `document_version`, `granted`, `source` (`ONBOARDING`/`SETTINGS`), thời điểm | V3 |

## 6. Safety, quyền riêng tư và consent

- Consent được kiểm tra **tại thời điểm xử lý**, không chỉ lúc bật (xem UC-05).
- Rút `AI_PROCESSING` không xóa dữ liệu đã phân tích trước đó; chỉ chặn xử lý mới.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `IdentityFlowIntegrationTest` gọi `/me`, `/me/consents`, `onboarding:complete` |
| Web | xong | Onboarding wizard, cài đặt hồ sơ, bảng consent |
| Mobile | xong | Onboarding, hồ sơ, consent; kiểm chứng bằng smoke API và Playwright trên Expo web, chưa chạy thiết bị thật |

## 8. Câu hỏi mở

1. Consent `ANALYTICS` và `MODEL_TRAINING` được thu thập nhưng **chưa có code nào đọc** chúng. Giữ lại hay ẩn khỏi giao diện cho đến khi có tính năng?
2. Consent `THINKING_PATTERNS` không được tạo lúc đăng ký và không có trong onboarding (web lẫn mobile); chỉ bật được trong cài đặt web. Có cần đưa vào onboarding không?
3. Consent tùy chọn mặc định ghi phiên bản cứng `"v1"` (`UserProfileService.java:39`). Ai quản lý phiên bản tài liệu consent?
