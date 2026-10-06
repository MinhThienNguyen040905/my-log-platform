# M5 Self-care API

Các endpoint dưới `/api/v1/self-care` yêu cầu access token. Mọi truy vấn goal/habit dùng `user_id` từ token; tài nguyên của user khác trả `404`.

## Goal

- `POST /goals`: body `{ "category": "MINDFULNESS", "title": "Thiền mỗi sáng", "description": "Tự chăm sóc", "startDate": "2026-10-05", "targetDate": null }`. Category: `SLEEP`, `MINDFULNESS`, `EXERCISE`, `SOCIAL`, `CUSTOM`. Trả `201`, `Location`, `ETag: "0"` và `GoalResponse`.
- `GET /goals`: trả danh sách goal mới nhất trước, mỗi goal có `habits` và progress. Chỉ có dữ liệu của user hiện tại.
- `PATCH /goals/{goalId}`: gửi `If-Match` theo `ETag` của goal và các field cần thay đổi (`title`, `description`, `status`, `startDate`, `targetDate`). Status: `ACTIVE`, `PAUSED`, `COMPLETED`, `ARCHIVED`. Stale version trả `409`.

Title/description bị từ chối nếu trống hoặc vượt giới hạn; bộ lọc từ khóa cơ bản từ chối một số dấu hiệu treatment plan. Cần đánh giá policy được duyệt trước khi coi bộ lọc này là đầy đủ. `PATCH` hiện không hỗ trợ xóa description hoặc ngày bằng `null`.

## Habit và completion

- `POST /goals/{goalId}/habits`: body `{ "title": "Thiền", "targetValue": 5, "unit": "minutes", "frequencyType": "DAILY", "daysOfWeek": null }`. Với `WEEKLY`, `daysOfWeek` là danh sách số ngày ISO 1–7, không rỗng. Habit chụp timezone từ profile lúc tạo.
- `PUT /habits/{habitId}/completions/{localDate}`: body `{ "value": 5 }`. Chỉ cho ngày đã tới, đúng lịch và từ ngày tạo habit. Gọi lặp cùng giá trị không tăng số completion; giá trị mới thay giá trị cũ.
- `DELETE /habits/{habitId}/completions/{localDate}`: bỏ completion; gọi lặp vẫn trả `204`.

`HabitResponse` có `streak`, `completionCount`, `completedDates`. Chỉ ngày có `value >= targetValue` mới tính vào progress. Streak đi qua các ngày theo lịch DAILY/WEEKLY, cho phép ngày đang đến hạn chưa được tick mà vẫn hiển thị chuỗi đến kỳ trước. `moodAssociation` là `null` cho đến khi có ít nhất 7 check-in có mood ở nhóm ngày hoàn thành và 7 ở nhóm ngày khác trong 90 ngày gần nhất; hiệu số trung bình chỉ là quan sát, không suy ra nguyên nhân.

Nội dung goal/habit được mã hóa trước khi lưu DB. `V10__selfcare.sql` là schema thực thi; không sửa migration sau khi đã áp dụng.
