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

`GET /api/v1/recommendations?topicCode=MINDFULNESS` lấy locale từ profile hiện tại và chỉ trả tối đa 3 excerpt nguyên văn của version APPROVED, còn hiệu lực, kèm `itemId`, `versionId`, `version`, `chunkId`, source. Khi `MYLOG_RAG_EMBEDDING_ENABLED=true` và provider data được phê duyệt, endpoint embed truy vấn theo topic rồi xếp hạng cosine trong cùng model signature. Nếu chưa có vector hoặc provider lỗi, endpoint fallback sang retrieval catalog đã duyệt. Không truyền journal content.

`POST /api/v1/recommendations/questions` nhận `{ "topicCode": "..." }`, retrieval tối đa 3 excerpt đã duyệt theo locale người dùng rồi gửi `topicCode`, text và citation nội bộ sang Gemini để tạo tối đa 3 câu hỏi theo system prompt. Journal text không được gửi. FE Journal Editor dùng endpoint này; API GET ở trên vẫn giữ contract excerpt. Generation dùng `MYLOG_RAG_EMBEDDING_ENDPOINT`, key riêng `MYLOG_RAG_GENERATION_API_KEY` và cờ `MYLOG_RAG_EMBEDDING_PROVIDER_DATA_APPROVED=true`; model sinh riêng cấu hình qua `MYLOG_RAG_GENERATION_MODEL` (mặc định `gemini-3.8-flash`), không dùng key hoặc model embedding. Response gồm `questions`; nếu không đủ căn cứ, `questions` rỗng và `message` là câu fallback đã quy định. Backend kiểm tra định dạng đánh số, dạng câu hỏi, số từ và số lượng; đây là kiểm tra cấu trúc, chưa phải đánh giá safety/ngữ nghĩa đầy đủ.

## Admin vận hành

- `GET /api/v1/admin/users?cursor=&limit=20` và `GET /api/v1/admin/users/{userId}/metadata`: chỉ ID, status, thời gian, version; không trả email, profile hoặc journal.
- `POST /api/v1/admin/users/{userId}:suspend|restore`: body `{ "reasonCode": "ABUSE_REVIEW" }`; suspend revoke sessions, cả hai thao tác có audit.
- `GET /api/v1/admin/ai-jobs?cursor=&limit=20`: chỉ metadata và `errorCode`; không trả payload, prompt, `lastErrorSummary`.
- `POST /api/v1/admin/ai-jobs/{jobId}:retry`: chỉ retry job `DEAD` do `PROVIDER_UNAVAILABLE`, sau cooldown 60 giây; job và journal cùng chuyển về `PENDING` trong một transaction. Worker vẫn kiểm tra consent/safety trước khi xử lý.
- `GET /api/v1/admin/dashboard`: ẩn toàn bộ khi dưới 20 active users, ẩn từng metric journal/check-in nếu dưới 20 người đóng góp.

Worker quét định kỳ các chunk thuộc version đã duyệt còn thiếu vector theo model signature và backfill theo lô; lỗi được ghi log theo loại lỗi và chunk vẫn có thể thử lại ở lần quét sau. `MYLOG_RAG_EMBEDDING_POLL_DELAY_MS` cấu hình chu kỳ quét. Semantic retrieval đã nối vào recommendation API nhưng vẫn mặc định tắt. Worker cần `MYLOG_IDENTITY_ENABLED=true`, embedding bật và `MYLOG_APP_PROFILE=worker`; recommendation API cũng gọi Gemini để embed truy vấn, nên API và worker đều cần cấu hình API key server-side khi semantic retrieval được bật. Ban đầu chỉ chạy một worker replica vì truy vấn lấy batch chưa claim/lock các chunk. Gemini chỉ được bật sau ADR-0004 provider review bằng cờ `MYLOG_RAG_EMBEDDING_PROVIDER_DATA_APPROVED`. Generation API đã được thêm; curated retrieval evaluation, generation evaluation, provider approval/operation và output safety validation vẫn còn mở.

The repository-owned `recommend-model/docs` corpus is imported by `recommend-model/tools/EmbedAllDocsToPgvector.java` as static system-managed knowledge. That importer publishes only its own `recommend-model-*` sources with a `SYSTEM` audit event, without assigning a human reviewer. This does not change the review workflow for knowledge items created through the admin API.

The Journal Editor uses locale vi. For the RECOMMEND_MODEL_DOCS topic, the recommendation service maps vi to the corpus locale vi-VN.
