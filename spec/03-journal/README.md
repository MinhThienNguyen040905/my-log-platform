# 03 — Nhật ký

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Viết, lưu, tìm, sửa, xóa nhật ký; nhãn (tag); ảnh trong bài; gợi ý viết.

- **Module backend:** `com.mylog.journal`
- **API gốc:** `/api/v1/journal-entries`, `/api/v1/journal-tags`, `/api/v1/journal-assets`, `/api/v1/journal-writing-suggestions`, `/api/v1/companion/title-suggestions`
- **Web:** [`features/journal`](../../front-end/my-app-web/features/journal), [`features/calendar`](../../front-end/my-app-web/features/calendar), [`app/api/journal-entries`](../../front-end/my-app-web/app/api/journal-entries)
- **Mobile:** [`src/features/journal`](../../front-end/my-app-mobile/src/features/journal)
- **Migration:** `V5`, `V20` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`adr/0002-application-envelope-encryption.md`](../../backend/docs/adr/0002-application-envelope-encryption.md), [`adr/0006-cloudinary-image-storage.md`](../../backend/docs/adr/0006-cloudinary-image-storage.md)

## 1. Mục tiêu và phạm vi

Nhật ký là dữ liệu cốt lõi của mylog. Người dùng là chủ dữ liệu; nội dung luôn được mã hóa và được lưu độc lập với AI.

- **Trong phạm vi:** CRUD bài viết (nội dung rich text TipTap), chỉ số tâm trạng đi kèm, yêu thích, nhãn, ảnh, tìm theo tiêu đề, đếm bài theo ngày cho lịch, gợi ý tiêu đề, gợi ý viết tiếp.
- **Ngoài phạm vi:** phân tích AI sau khi lưu ([05-ai-analysis](../05-ai-analysis.md)), sàng lọc an toàn chi tiết ([08-safety](../08-safety/README.md)), companion chi tiết ([10-companion](../10-companion.md)), check-in ([04-checkin](../04-checkin.md)).

## 2. Use case

- [UC-07 — Nhận gợi ý để viết tiếp](UC-07-writing-suggestions.md)
- [UC-08 — Viết và lưu nhật ký](UC-08-write-save-entry.md)
- [UC-09 — Đọc, tìm và xem lại nhật ký](UC-09-read-search-entries.md)
- [UC-10 — Sửa, đánh dấu và xóa nhật ký](UC-10-edit-favorite-delete.md) (gồm nhãn)
- [UC-11 — Thêm ảnh vào nhật ký](UC-11-entry-images.md)

## 3. Business rule và phân quyền chung

- Mọi endpoint cần bearer token, trừ `GET /journal-assets/raw/{assetId}` (dùng chữ ký trong URL). Mọi truy vấn lọc theo `userId` hiện tại; bài của người khác trả `404`.
- **Mã hóa:** tiêu đề, nội dung TipTap, văn bản thuần trích ra và địa điểm được mã hóa chung trong một payload (`JournalPayload`). Tên nhãn cũng được mã hóa. Chỉ số tâm trạng, ngày, trạng thái lưu rõ để thống kê.
- **Hai loại version:**
  - `rowVersion` (trả ở `ETag`): tăng với mọi thay đổi dòng; dùng cho `If-Match` khi sửa/xóa.
  - `contentVersion`: chỉ tăng khi sửa bài; kết quả phân tích chỉ có hiệu lực khi khớp `contentVersion`.
- **Sàng lọc an toàn:** mỗi lần tạo/sửa, `title + "\n" + plainText` được sàng lọc **trong cùng transaction** (`JournalService.java:87`, `253`). Kết quả quyết định `riskLevel` và `analysisStatus`:
  - cho phép phân tích → `PENDING` (tạo) hoặc `ANALYSIS_OUTDATED` (sửa), phát outbox `JournalEntrySubmitted`;
  - không cho phép → `BLOCKED_BY_SAFETY`; nếu là `FAIL_SAFE` (classifier lỗi) thì phát thêm `SafetyRescreenRequested` để sàng lọc lại sau.
  - Bài **luôn được lưu** dù kết quả sàng lọc thế nào.
- **Outbox:** sự kiện chỉ mang id và `contentVersion`, không mang nội dung: `JournalEntrySubmitted`, `JournalEntryChanged`, `JournalEntryDeleted`, `SafetyRescreenRequested`.
- **Quota ghi:** tạo, sửa, xóa, yêu thích bài; tạo, gắn, bỏ nhãn: 60 lần/15 phút/user (`JournalController.java:107`, `JournalTagController`). Companion: 40 lượt/15 phút.
- **Giới hạn trường:** tiêu đề ≤ 160, địa điểm ≤ 160, `moodCode` khớp `[a-z0-9-]{1,32}`, `moodScore`/`stressScore`/`energyScore` từ 1 đến 10 (tối đa 1 chữ số thập phân), `sleepMinutes` 0–1440, `occurredAt` không quá 1 ngày trong tương lai, tối đa 20 nhãn và 20 ảnh mỗi bài (`JournalService.java:425-437`).

## 4. API contract

| Method | Path | Request | Response | Lỗi chính |
|---|---|---|---|---|
| POST | `/journal-entries` | header `Idempotency-Key`; body xem UC-08 | `201` JournalEntry + `ETag` + `Location` | 400, 409, 429 |
| GET | `/journal-entries` | `from?`, `to?`, `tag?`, `favorite?`, `cursor?`, `limit` (1–100, mặc định 20) | `200 {items: [JournalSummary], nextCursor}` | 400 |
| GET | `/journal-entries/search` | `query`, `cursor?`, `limit` (1–50, mặc định 5) | `200` như list | 400 |
| GET | `/journal-entries/daily-counts` | `from`, `to` (≤ 92 ngày) | `200 {from, to, days: [{date, count, latestMoodCode}]}` | 400 |
| GET | `/journal-entries/{id}` | — | `200` JournalEntry + `ETag` | 404 |
| PATCH | `/journal-entries/{id}` | `If-Match`; body như tạo, mọi trường tùy chọn | `200` JournalEntry + `ETag` | 400, 404, 409, 429 |
| DELETE | `/journal-entries/{id}` | `If-Match` | `204` | 404, 409, 429 |
| PUT / DELETE | `/journal-entries/{id}/favorite` | — | `200` JournalEntry | 404, 429 |
| POST | `/journal-tags` | `name`, `color?` | `201` `{id, name, color}`; trùng tên thì trả nhãn có sẵn | 400, 429 |
| GET | `/journal-tags` | — | `200 [Tag]` | |
| GET | `/journal-entries/{id}/tags` | — | `200 [Tag]` | 404 |
| PUT / DELETE | `/journal-entries/{id}/tags/{tagId}` | — | `204` | 404 |
| POST | `/journal-assets/upload` | multipart `file` | `201 {id, format, mimeType, width, height, sizeBytes, url}` | 400 |
| POST | `/journal-assets/urls` | `{assetIds: []}` | `200 {urls: {assetId: url}}` | 400 |
| GET | `/journal-assets/{assetId}/url` | — | `200 {url}` | 404 |
| GET | `/journal-assets/raw/{assetId}` | `expires`, `sig` | `200` file ảnh (chỉ khi lưu local) | 403, 404, 410 |
| GET | `/companion/title-suggestions` | `mood?` (`happy`, `neutral`, `angry`, `sad`, `very_bad`) | `200 {titles: []}` | 400 |
| POST | `/journal-writing-suggestions` | xem UC-07 | `200` WritingSuggestion | 400, 429 |

**JournalEntry:** `{id, title, contentJson, location, occurredAt, localDate, timezone, moodCode, moodScore, stressScore, energyScore, sleepMinutes, favorite, entryStatus, riskLevel, analysisStatus, contentVersion, rowVersion, createdAt, updatedAt, tags[], photoUrl}`.
**JournalSummary:** `{id, title, occurredAt, localDate, moodCode, favorite, riskLevel, analysisStatus, rowVersion, tags[], photoUrl}`.

Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `journal_entries` | Bài viết: payload mã hóa, `occurred_at`, `local_date`, `timezone`, chỉ số tâm trạng, `favorite`, `entry_status`, `risk_level`, `analysis_status`, `content_version`, `row_version`, `deleted_at` | V5 |
| `journal_tags` | Nhãn của user: tên mã hóa + hash để chống trùng, màu | V5 |
| `journal_entry_tags` | Nối bài–nhãn | V5 |
| `journal_assets` | Ảnh: `status`, `position`, `is_cover`, `sha256`, kích thước, nơi lưu | V5, V20 |

**`analysis_status`:** `PENDING` → `ANALYZING` → `ANALYZED`; ngoài ra `ANALYSIS_OUTDATED`, `ANALYSIS_FAILED`, `BLOCKED_BY_SAFETY`, `NOT_REQUESTED`. Chuyển trạng thái do module analysis gọi qua `JournalAnalysisAccess` (`JournalService.java:314-351`).

## 6. Safety, quyền riêng tư và consent

- Sàng lọc an toàn chạy cho mọi bài, không phụ thuộc consent. Consent `AI_PROCESSING` chỉ quyết định bài có được AI phân tích hay không (kiểm tra ở worker).
- Bài `HIGH`/`CRITICAL` không đi vào phân tích thường; client hiển thị luồng hỗ trợ an toàn ([08-safety](../08-safety/README.md)).
- Ảnh được làm sạch metadata trước khi lưu; URL ảnh là URL có chữ ký, sống 1 giờ.
- Xuất dữ liệu đọc tối đa 10.000 bài (`JournalService.java:114-120`).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `JournalCheckinIntegrationTest`, `JournalTagControllerTest`, `TipTapContentTest`, `ImageInspectionServiceTest`, `LocalFileAssetStorageTest`, `WritingSuggestionServiceTest` |
| Web | xong | Editor TipTap, ảnh, nhãn, lịch, tìm kiếm, companion |
| Mobile | đang làm | CRUD văn bản, yêu thích, nhãn, tìm kiếm, lịch sử; chưa có rich text và ảnh |

## 8. Câu hỏi mở

1. Tìm kiếm chỉ khớp **tiêu đề** và chỉ quét tối đa 1.000 bài gần nhất (vì nội dung mã hóa, không tìm được bằng SQL) (`JournalService.java:157-227`). Có cần tìm theo nội dung không?
2. Giới hạn 30 ảnh/giờ lưu **trong bộ nhớ** của từng instance (`JournalAssetService.java:36`), không dùng chung khi chạy nhiều instance như các rate limit khác.
3. Xem thêm câu hỏi mở trong từng UC.
