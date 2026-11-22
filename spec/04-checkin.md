# 04 — Check-in cảm xúc

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Ghi nhận nhanh tâm trạng, căng thẳng, năng lượng, giấc ngủ và hoạt động trong ngày, không cần viết bài.

- **Module backend:** `com.mylog.checkin`
- **API gốc:** `/api/v1/check-ins`
- **Web:** [`features/checkin`](../front-end/my-app-web/features/checkin) (`DailyCheckinCard`, `features/dashboard/components/CheckinDialog.tsx`)
- **Mobile:** —
- **Migration:** `V5` (trong [`db/migration`](../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** —

## 1. Mục tiêu và phạm vi

Check-in là **điểm dữ liệu chính của một ngày** cho dashboard, insight và báo cáo. Quan sát từ nhật ký chỉ là nguồn dự phòng khi ngày đó không có check-in.

- **Trong phạm vi:** tạo/sửa check-in của một ngày, xem theo ngày, xem theo khoảng ngày, hoạt động trong ngày, ghi chú ngắn.
- **Ngoài phạm vi:** nhắc nhở check-in, xóa check-in (hiện không có API xóa; xóa khi "bắt đầu lại" hoặc xóa tài khoản).

## 2. Use case

### UC-12 — Check-in cảm xúc hằng ngày

Catalog: UC-12 trong [`USE_CASE_CATALOG.md`](../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên).

#### 1. Tác nhân

Người dùng đã đăng nhập.

#### 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập.
- **Hậu điều kiện:** mỗi người dùng có tối đa **một** check-in cho mỗi `localDate`; `version` tăng 1 sau mỗi lần lưu.

#### 3. Luồng chính

1. Người dùng mở thẻ check-in trên dashboard, chọn tâm trạng, các chỉ số, giấc ngủ, hoạt động, có thể ghi chú.
2. Client gửi `PUT /check-ins/{localDate}` với `localDate` tính theo múi giờ của hồ sơ.
3. Backend kiểm tra dữ liệu (`CheckinService.java:24-41`), tạo mới nếu ngày chưa có, ngược lại ghi đè (upsert), mã hóa ghi chú.
4. Trả `200` với check-in và `ETag`.
5. Dashboard đọc lại để cập nhật dòng thời gian.

#### 4. Luồng phụ và ngoại lệ

- `localDate` sau ngày mai (theo giờ máy chủ), timezone sai, giá trị ngoài phạm vi: `400`.
- Lưu nhiều lần trong ngày: lần sau ghi đè lần trước (không có `If-Match`, lần ghi cuối thắng).
- Không gửi `activities` (`null`): giữ nguyên hoạt động đã lưu. Gửi `[]`: xóa hết hoạt động.
- `GET /check-ins/{localDate}` khi ngày chưa có: `404`.
- Khoảng `from`–`to` dài hơn 366 ngày hoặc `from` sau `to`: `400`.
- Mất mạng khi lưu: web giữ nguyên dữ liệu người dùng đã nhập (`daily-checkin.spec.ts` W2-09).

#### 5. Business rule

- Mọi trường trừ `activities` được **thay toàn bộ** mỗi lần `PUT`: trường không gửi sẽ thành `null` (`JpaCheckinStore`, dòng 44-49).
- `moodCode` khớp `[a-z0-9-]{1,32}`; `moodScore`, `stressScore`, `energyScore` từ 1 đến 10, tối đa 1 chữ số thập phân; `sleepMinutes` 0–1440; `note` ≤ 4.000 ký tự (rỗng = xóa ghi chú).
- `activities`: tối đa 20, không trùng mã; `code` khớp `[a-z0-9-]{1,40}`; `durationMinutes` 0–1440; `intensity` là `LOW`, `MODERATE` hoặc `HIGH`.
- Danh mục hoạt động web đưa ra: `walking`, `running`, `exercise`, `stretching`, `meditation`, `reading`, `studying`, `journaling`, `cooking`, `housework`, `family-time`, `social`, `eating-out`, `nap`, `meeting`, `work-overtime` (`checkin-activities.ts`). Backend chấp nhận mã khác; web giữ lại mã lạ do client khác đã lưu.
- Lưu nhật ký có chỉ số tâm trạng **không** tạo check-in (`daily-checkin.spec.ts` W2-10).
- **Ưu tiên dữ liệu trong ngày:** khi tổng hợp, nếu ngày có check-in thì mọi chỉ số của ngày lấy từ check-in (`source = CHECKIN`), kể cả khi check-in để trống chỉ số đó; nếu không, lấy chỉ số của bài nhật ký mới nhất trong ngày (`source = JOURNAL_FALLBACK`) (`JpaDailyAggregateSource.java:38-70`).

#### 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| PUT | `/api/v1/check-ins/{localDate}` | `timezone` (bắt buộc), `moodCode?`, `moodScore?`, `stressScore?`, `energyScore?`, `sleepMinutes?`, `note?`, `activities?: [{code, durationMinutes?, intensity?}]` | `200` Checkin + `ETag` | 400, 401 |
| GET | `/api/v1/check-ins/{localDate}` | — | `200` Checkin | 401, 404 |
| GET | `/api/v1/check-ins?from=&to=` | — | `200 [Checkin]` | 400, 401 |

Checkin: `{id, localDate, timezone, moodCode, moodScore, stressScore, energyScore, sleepMinutes, note, activities[], version, createdAt, updatedAt}`.

#### 7. Giao diện web và mobile

- **Web:** `DailyCheckinCard` trên dashboard, hộp thoại `CheckinDialog`.
- **Mobile:** chưa có màn hình check-in.

#### 8. Tiêu chí chấp nhận

- [x] Upsert theo chủ sở hữu và ngày, ghi chú được mã hóa (`JournalCheckinIntegrationTest.checkinUpsertsPerOwnerAndDateWithEncryptedNote`).
- [x] Web: tạo, hiển thị, sửa như một upsert theo ngày của hồ sơ; hoạt động; từ chối giá trị ngoài phạm vi; giữ dữ liệu khi mất mạng; nhật ký không tạo check-in (`daily-checkin.spec.ts`).
- [x] Dashboard ưu tiên check-in, dùng nhật ký làm dự phòng (`M4IntegrationTest`).
- [ ] Test backend riêng cho `activities: null` giữ nguyên và `[]` xóa hết.

## 3. Business rule và phân quyền chung

- Cần bearer token; mọi truy vấn lọc theo user hiện tại.
- Ghi chú được mã hóa (`daily_checkins.note`); chỉ số lưu rõ để tổng hợp.

## 4. API contract

Xem mục 6 của UC-12 ở trên.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `daily_checkins` | Một dòng/user/ngày (`UNIQUE(user_id, local_date)`): chỉ số, ghi chú mã hóa, `source`, `row_version` | V5 |
| `checkin_activities` | Hoạt động của check-in: mã, thời lượng, cường độ | V5 |

## 6. Safety, quyền riêng tư và consent

- Ghi chú check-in **không** đi qua sàng lọc an toàn như nhật ký.
- Không cần consent AI; check-in không được gửi tới provider. Hoạt động được dùng trong insight thống kê ([07-insights-reports](07-insights-reports/README.md)).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `JournalCheckinIntegrationTest`, `M4IntegrationTest` |
| Web | đã kiểm chứng | `daily-checkin.spec.ts` |
| Mobile | chưa làm | |

## 8. Câu hỏi mở

1. Ghi chú check-in có nội dung tự do (tới 4.000 ký tự) nhưng không được sàng lọc an toàn. Có cần sàng lọc như nhật ký không?
2. `PUT` thiếu trường sẽ xóa giá trị cũ, và không có `If-Match`: hai thiết bị cùng lưu có thể ghi đè nhau. Có cần khóa lạc quan không?
3. Có cần API xóa check-in của một ngày không?
4. Mobile có làm check-in không? Nếu có, dùng chung danh mục hoạt động với web.
