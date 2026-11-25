# UC-04 — Hoàn thành thiết lập ban đầu và sửa hồ sơ

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Hồ sơ và đồng ý xử lý dữ liệu](README.md)
- **Catalog:** UC-04 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có hồ sơ mặc định (tạo cùng tài khoản).
- **Hậu điều kiện (onboarding):** `onboardingCompletedAt` có giá trị, `version` tăng 1; client cho vào ứng dụng chính.
- **Hậu điều kiện (sửa hồ sơ):** các trường gửi lên được cập nhật, trường không gửi giữ nguyên, `version` tăng 1.

## 3. Luồng chính

**Onboarding**

1. Sau đăng nhập, client đọc `GET /me`. Nếu `onboardingCompletedAt` là `null`, chuyển sang màn onboarding.
2. Người dùng nhập tên hiển thị, chọn mục tiêu và các consent tùy chọn.
3. Client gọi `PATCH /me` (kèm `If-Match`) để lưu hồ sơ, `PUT /me/consents/{type}` cho từng consent, rồi `POST /me/onboarding:complete` với version mới.
4. Backend đặt `onboardingCompletedAt` và trả hồ sơ mới.

**Sửa hồ sơ**

1. Người dùng mở cài đặt, sửa tên, bút danh, mục tiêu, múi giờ, ngôn ngữ hoặc linh vật.
2. Client gọi `PATCH /me` với `If-Match` là version đang hiển thị.
3. Backend kiểm tra và lưu (`UserProfileService.java:47-71`), trả hồ sơ cùng `ETag` mới.

## 4. Luồng phụ và ngoại lệ

- `If-Match` không khớp version hiện tại (sửa ở thiết bị khác): `409`; client cần tải lại hồ sơ.
- `If-Match` không phải số: `400`.
- Dữ liệu không hợp lệ (xem mục 5): `400`.
- Gọi `onboarding:complete` lần nữa: vẫn thành công nếu version đúng; DB giữ thời điểm hoàn tất ban đầu (`coalesce`, `UserRepository.java:66`), nhưng response trả thời điểm của lần gọi mới (`UserProfileService.java:78`).

## 5. Business rule

- `displayName`, `penName`: tối đa 120 ký tự sau khi trim.
- `onboardingGoals`: tối đa 5, không trùng, thuộc `REFLECTION`, `SLEEP`, `MINDFULNESS`, `EXERCISE`, `SOCIAL`.
- `mascot`: `sprout`, `cat`, `coffee` hoặc `off`; giá trị cũ không còn hỗ trợ được đọc thành `sprout`.
- `timezone`: ZoneId hợp lệ. `locale`: dạng `vi` hoặc `vi-VN`.
- Trường gửi `null` hoặc không gửi = giữ nguyên.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/me` | — | `200` ProfileResponse + `ETag` | 401, 404 |
| PATCH | `/api/v1/me` | `If-Match` + các trường | `200` + `ETag` | 400, 409 |
| POST | `/api/v1/me/onboarding:complete` | `If-Match` | `200` | 400, 409 |

## 7. Giao diện web và mobile

- **Web:** `app/onboarding` (`OnboardingWizard`), chặn truy cập qua `app/_components/AuthGate.tsx`; sửa hồ sơ ở `app/(app)/setting` (`SettingsView`).
- **Mobile:** `(onboarding)/onboarding.tsx`, điều hướng trong `src/app/index.tsx` và `use-protected-profile.ts`; sửa hồ sơ ở `(app)/profile.tsx`.

## 8. Tiêu chí chấp nhận

- [x] Đọc hồ sơ, hoàn tất onboarding với `If-Match` qua HTTP (`IdentityFlowIntegrationTest`).
- [x] Mobile: cập nhật hồ sơ với `If-Match`, version sai trả `409` (script `e2e-api-smoke.mjs`).
- [ ] Test backend riêng cho các ràng buộc `onboardingGoals` và `mascot`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Một phần, xem mục 8 |
| Web | xong | |
| Mobile | xong | Chưa thử sửa đồng thời giữa web và mobile |

## 10. Câu hỏi mở

- Có cho người dùng bỏ qua onboarding không?
- Gọi lại `onboarding:complete` trả `onboardingCompletedAt` khác với giá trị lưu trong DB. Sửa response cho khớp DB?
