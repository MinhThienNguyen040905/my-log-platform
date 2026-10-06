# M6 knowledge và admin API

M6 dùng V11 cho knowledge/prompt schema, V12 cho role/permission seed và V13 sửa trigger để `ON DELETE SET NULL` của author/reviewer hoạt động khi xóa user, vẫn giữ content đã duyệt bất biến. Cả ba đã áp dụng lên Supabase ngày 2026-10-01. M7 tiếp tục ở V14. Không seed nội dung knowledge, nguồn hỗ trợ hay prompt chưa được duyệt.

## Permission matrix

| Role | Permission | API |
|---|---|---|
| CONTENT_EDITOR | `knowledge:write` | tạo draft, tạo version, sửa draft, submit review |
| CONTENT_APPROVER | `knowledge:review` | đọc, approve/reject/archive version |
| SYSTEM_ADMIN | `users:read-metadata`, `users:suspend`, `jobs:read`, `jobs:retry`, `admin:dashboard` | vận hành user/job và aggregate |
| SUPPORT_AGENT | `users:read-metadata` | metadata user giới hạn |
| SAFETY_ADMIN | `safety:manage` | dành cho workflow safety sau khi policy được duyệt |
| AUDITOR | `audit:read` | dành cho audit read API sau này |

Không có role nào được quyền đọc plaintext journal. Role admin không tự động có `knowledge:review`. Chưa có endpoint gán role; bootstrap role phải được kiểm soát ngoài API. Production admin cần MFA ở M8.

Để bootstrap trên môi trường được phép, lấy UUID tài khoản đã xác minh rồi gán role trong một transaction quản trị có ghi nhận người phê duyệt:

```sql
INSERT INTO user_roles(user_id, role_id, assigned_by, assigned_at)
SELECT :target_user_id, id, :approver_user_id, CURRENT_TIMESTAMP
FROM roles WHERE code = 'CONTENT_EDITOR'
ON CONFLICT (user_id, role_id) DO NOTHING;
```

Thay role code theo phân công; tách editor và approver thành hai người khác nhau.

## Knowledge workflow

- `POST /api/v1/admin/knowledge-items`: tạo item + version 1 `DRAFT`.
- `GET /api/v1/admin/knowledge-items/{itemId}/versions/{version}`: đọc bản đầy đủ cho editor/reviewer.
- `POST /api/v1/admin/knowledge-items/{itemId}/versions`: tạo draft version tiếp theo.
- `PATCH /api/v1/admin/knowledge-items/{itemId}/versions/{version}`: sửa title/content khi còn DRAFT. Checksum không đổi thì không chunk lại.
- `POST /api/v1/admin/knowledge-items/{itemId}/versions/{version}:submit`: chunk theo `paragraph-800-v1`, chuyển `IN_REVIEW`.
- `POST ...:approve`, `POST ...:reject`, `POST ...:archive`: reviewer khác creator; reject/archive cần body `{ "reasonCode": "RETIRED" }`.

Một item chỉ có một version APPROVED cùng lúc. Archive version cũ trước khi approve version thay thế. Approved/archived content và chunk bất biến ở database; archive giữ citation lịch sử. Audit approve/archive chỉ lưu ID, action và reason code. Nguồn có license hạn chế chỉ được nhập nội dung phù hợp với quyền sử dụng.

`GET /api/v1/recommendations?topicCode=MINDFULNESS` lấy locale từ profile hiện tại và chỉ trả tối đa 3 excerpt nguyên văn của version APPROVED, còn hiệu lực, kèm `itemId`, `versionId`, `version`, `chunkId`, source. Chưa có embedding, semantic ranking hoặc LLM generation; không gọi provider và không truyền journal content.

## Admin vận hành

- `GET /api/v1/admin/users?cursor=&limit=20` và `GET /api/v1/admin/users/{userId}/metadata`: chỉ ID, status, thời gian, version; không trả email, profile hoặc journal.
- `POST /api/v1/admin/users/{userId}:suspend|restore`: body `{ "reasonCode": "ABUSE_REVIEW" }`; suspend revoke sessions, cả hai thao tác có audit.
- `GET /api/v1/admin/ai-jobs?cursor=&limit=20`: chỉ metadata và `errorCode`; không trả payload, prompt, `lastErrorSummary`.
- `POST /api/v1/admin/ai-jobs/{jobId}:retry`: chỉ retry job `DEAD` do `PROVIDER_UNAVAILABLE`, sau cooldown 60 giây; job và journal cùng chuyển về `PENDING` trong một transaction. Worker vẫn kiểm tra consent/safety trước khi xử lý.
- `GET /api/v1/admin/dashboard`: ẩn toàn bộ khi dưới 20 active users, ẩn từng metric journal/check-in nếu dưới 20 người đóng góp.

M6 vẫn mở ở embedding provider/model, partial vector index, curated retrieval eval, RAG generation và output safety validation. Chỉ bật các phần đó sau ADR-0004 provider review và nội dung/policy được duyệt.
