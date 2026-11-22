# 09 — Tự chăm sóc

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Mục tiêu, thói quen, đánh dấu hoàn thành, chuỗi ngày, liên hệ giữa thói quen và tâm trạng. Thêm một tính năng ở mức đề xuất: lời nhắn cho chính mình trong tương lai.

- **Module backend:** `com.mylog.selfcare`
- **API gốc:** `/api/v1/self-care`
- **Web:** [`features/selfcare`](../../front-end/my-app-web/features/selfcare) (`HabitTrackerCard`, `SelfCareGoalsChecklist` trên trang Insight; `CreateHabitFromTemplateModal` trong editor)
- **Mobile:** màn hình `(app)/self-care.tsx`
- **Migration:** `V10`, `V21` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`SELFCARE_M5_API.md`](../../backend/docs/SELFCARE_M5_API.md), [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](../../backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)

## 1. Mục tiêu và phạm vi

Giúp người dùng duy trì những việc nhỏ chăm sóc bản thân, trong phạm vi wellness. **Không** phải kế hoạch điều trị.

- **Trong phạm vi:** mục tiêu theo nhóm, thói quen hằng ngày/hằng tuần, đánh dấu hoàn thành theo ngày, chuỗi ngày, liên hệ thói quen – tâm trạng, gắn thói quen với công cụ bình tĩnh.
- **Ngoài phạm vi:** nhắc nhở/thông báo; công cụ bình tĩnh và gợi ý thói quen từ companion ([10-companion](../10-companion.md)); kế hoạch 3 tuần từ thẻ kiểu suy nghĩ ([06-thinking-patterns](../06-thinking-patterns.md)) dùng lại API của feature này.

## 2. Use case

- [UC-16 — Đặt mục tiêu và theo dõi thói quen tự chăm sóc](UC-16-goals-habits.md)
- [UC-18 — Viết lời nhắn cho chính mình trong tương lai](UC-18-future-self-letter.md) (đề xuất, chưa có code)

## 3. Business rule và phân quyền chung

- Mọi endpoint cần bearer token và lọc theo chủ sở hữu.
- **Mã hóa:** tiêu đề, mô tả mục tiêu và tiêu đề thói quen.
- **Phạm vi wellness** (`SelfCareService.java:304-315`): văn bản không được chứa ký tự điều khiển (kể cả xuống dòng) và không chứa các cụm (sau khi bỏ dấu) "chan doan", "dieu tri", "ke don", "thuoc dieu tri", "lieu trinh", "tri lieu", "diagnos", "prescri", "treatment plan", "medication plan" → `400`.
- Mục tiêu và thói quen có `version`; sửa cần `If-Match`.

## 4. API contract

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/self-care/goals` | `category`, `title`, `description?`, `startDate?`, `targetDate?` | `201` Goal + `ETag` | 400 |
| GET | `/self-care/goals` | — | `200 [Goal]` (kèm thói quen) | |
| PATCH | `/self-care/goals/{goalId}` | `If-Match`; `title?`, `description?`, `status?`, `startDate?`, `targetDate?` | `200` Goal + `ETag` | 400, 404, 409 |
| DELETE | `/self-care/goals/{goalId}` | — | `204` (xóa luôn thói quen và lần hoàn thành) | 404 |
| POST | `/self-care/goals/{goalId}/habits` | `title`, `targetValue`, `unit`, `frequencyType`, `daysOfWeek?`, `toolCode?` | `201` Habit + `ETag` | 400, 404, 409 |
| PATCH | `/self-care/habits/{habitId}` | `If-Match`; mọi trường tùy chọn, `toolCode: ""` để bỏ liên kết, `status?` | `200` Habit + `ETag` | 400, 404, 409 |
| DELETE | `/self-care/habits/{habitId}` | — | `204` | 404 |
| PUT | `/self-care/habits/{habitId}/completions/{localDate}` | `value` | `200` Habit | 400, 404, 409 |
| DELETE | `/self-care/habits/{habitId}/completions/{localDate}` | — | `204` | 404 |

- **Goal:** `{id, category, title, description, status, startDate, targetDate, version, createdAt, updatedAt, habits: [Habit]}`.
- **Habit:** `{id, goalId, title, targetValue, unit, frequencyType, daysOfWeek, timezone, toolCode, status, version, streak, completionCount, completedDates, moodAssociation}`.

Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `selfcare_goals` | Mục tiêu: nhóm, tiêu đề/mô tả mã hóa, trạng thái, ngày, `version` | V10 |
| `habits` | Thói quen: tiêu đề mã hóa, mục tiêu số, đơn vị, lịch, múi giờ, `tool_code` (V21), trạng thái, `version` | V10, V21 |
| `habit_completions` | Một lần hoàn thành mỗi thói quen mỗi ngày | V10 |

## 6. Safety, quyền riêng tư và consent

- Không cần consent AI: liên hệ thói quen – tâm trạng chỉ là thống kê trên dữ liệu của chính người dùng.
- Liên hệ không bao giờ được diễn đạt thành nguyên nhân (`HabitMoodAssociationTest`).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | UC-16: `M5IntegrationTest`, `HabitScheduleTest`, `HabitMoodAssociationTest` |
| Web | xong | UC-16: thẻ thói quen và danh sách mục tiêu trên trang Insight; chưa có Playwright riêng |
| Mobile | xong | UC-16: tạo mục tiêu/thói quen, đánh dấu hoàn thành |
| UC-18 | chưa làm | Chỉ có đề xuất |

## 8. Câu hỏi mở

1. Bộ lọc "phạm vi wellness" chỉ là danh sách từ khóa cơ bản. Kế hoạch M5 yêu cầu policy và nội dung được duyệt.
2. Web không có trang tự chăm sóc riêng; mục tiêu và thói quen nằm trong trang Insight. Có cần một trang riêng?
