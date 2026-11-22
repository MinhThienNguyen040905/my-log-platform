# UC-16 — Đặt mục tiêu và theo dõi thói quen tự chăm sóc

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Tự chăm sóc](README.md)
- **Catalog:** UC-16 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập; tạo thói quen cần một mục tiêu đang `ACTIVE`.
- **Hậu điều kiện:** mục tiêu, thói quen và lần hoàn thành được lưu; chuỗi ngày và số lần hoàn thành được tính lại khi đọc.

## 3. Luồng chính

1. Người dùng tạo mục tiêu: chọn nhóm, đặt tiêu đề, mô tả, ngày bắt đầu và ngày đích.
2. Người dùng thêm thói quen vào mục tiêu: tiêu đề, mục tiêu số (ví dụ 15) và đơn vị (ví dụ `minutes`), lịch `DAILY` hoặc `WEEKLY` kèm các ngày trong tuần; tùy chọn gắn công cụ bình tĩnh.
3. Mỗi ngày đến lịch, người dùng đánh dấu hoàn thành → `PUT .../completions/{localDate} {value}`.
4. Backend trả thói quen kèm chuỗi ngày, số lần hoàn thành, các ngày đã hoàn thành và liên hệ với tâm trạng (nếu đủ dữ liệu).
5. Người dùng có thể bỏ đánh dấu, tạm dừng, lưu trữ, hoàn thành mục tiêu, sửa hoặc xóa.

**Cách khác để tạo**

- Từ companion: chọn mẫu thói quen đã duyệt (`CreateHabitFromTemplateModal`), xem [10-companion](../10-companion.md).
- Từ thẻ kiểu suy nghĩ: "Bắt đầu kế hoạch" tạo mục tiêu 21 ngày + thói quen hằng ngày, xem [06-thinking-patterns](../06-thinking-patterns.md).

## 4. Luồng phụ và ngoại lệ

- `If-Match` lệch (sửa ở nơi khác): `409`.
- Thêm thói quen vào mục tiêu không `ACTIVE`, hoặc đánh dấu hoàn thành cho thói quen/mục tiêu không `ACTIVE`: `409`.
- Đánh dấu ngày trong tương lai, trước ngày tạo thói quen, hoặc ngày không có trong lịch: `400`.
- Đánh dấu lại cùng ngày: ghi đè giá trị (idempotent).
- Văn bản ngoài phạm vi wellness: `400` (xem [README](README.md#3-business-rule-và-phân-quyền-chung)).
- Sửa lịch hoặc mục tiêu số: các lần hoàn thành cũ vẫn giữ nguyên.

## 5. Business rule

- **Nhóm mục tiêu:** `SLEEP`, `MINDFULNESS`, `EXERCISE`, `SOCIAL`, `CUSTOM`.
- **Trạng thái mục tiêu:** `ACTIVE`, `PAUSED`, `COMPLETED`, `ARCHIVED`. **Trạng thái thói quen:** `ACTIVE`, `PAUSED`, `ARCHIVED`.
- Tiêu đề ≤ 160, mô tả ≤ 2.000; `targetDate` không trước `startDate`.
- `targetValue` và `value` > 0, tối đa 2 chữ số thập phân; `unit` khớp `[A-Za-z0-9_]{1,32}`.
- `WEEKLY` cần 1–7 ngày (1 = thứ Hai … 7 = Chủ nhật); `DAILY` không được gửi ngày.
- `toolCode`: `BREATHING_2MIN`, `AMBIENT`, `VENT_PROMPT` (ràng buộc cả ở DB, V21).
- Thói quen lưu múi giờ của hồ sơ lúc tạo; ngày hoàn thành tính theo múi giờ đó.
- **Chuỗi ngày** (`HabitSchedule.streak`): đếm ngược các ngày đến lịch liên tiếp đã hoàn thành, bỏ qua ngày không đến lịch; ngày đến lịch hôm nay chưa làm không làm đứt chuỗi.
- **Liên hệ với tâm trạng** (`HabitMoodAssociation`): trong tối đa 90 ngày gần nhất, so điểm tâm trạng trung bình giữa ngày đã làm và ngày chưa làm (chỉ ngày đến lịch); cần ít nhất **7 ngày mỗi nhóm**, nếu không thì không trả.

## 6. API liên quan

Xem bảng API ở [README](README.md#4-api-contract).

## 7. Giao diện web và mobile

- **Web:** `HabitTrackerCard`, `SelfCareGoalsChecklist` trên trang Insight; tạo thói quen từ mẫu trong editor.
- **Mobile:** `(app)/self-care.tsx`.

## 8. Tiêu chí chấp nhận

- [x] Mã hóa, chỉ chủ sở hữu, khóa lạc quan, đánh dấu hoàn thành idempotent (`M5IntegrationTest`).
- [x] Sửa/xóa thói quen theo chủ sở hữu, có version, xóa lan (`M5IntegrationTest`).
- [x] Chuỗi ngày hằng tuần bỏ qua ngày không đến lịch; chuỗi hằng ngày dừng ở ngày bỏ lỡ (`HabitScheduleTest`).
- [x] Liên hệ tâm trạng cần 7 mẫu mỗi nhóm và không nói nguyên nhân (`HabitMoodAssociationTest`).
- [x] Mobile: tạo mục tiêu và thói quen (`e2e-expo-web.mjs`).
- [ ] Playwright web cho mục tiêu và thói quen.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | |
| Web | xong | |
| Mobile | xong | Chưa chạy trên thiết bị thật |

## 10. Câu hỏi mở

- Có cần nhắc nhở thói quen (thông báo) không? Kế hoạch đang xếp vào sau MVP.
