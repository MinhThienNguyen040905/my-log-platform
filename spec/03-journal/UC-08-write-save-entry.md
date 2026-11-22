# UC-08 — Viết và lưu nhật ký

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Nhật ký](README.md)
- **Catalog:** UC-08 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập.
- **Hậu điều kiện:** bài được lưu (mã hóa) với `entryStatus = SAVED`, `contentVersion = 1`, `rowVersion = 0`; nhãn và ảnh được gắn; một sự kiện safety được ghi; outbox được phát nếu bài được phép phân tích. Tất cả trong **một transaction**.

## 3. Luồng chính

1. Người dùng viết tiêu đề, nội dung (TipTap), tùy chọn chọn tâm trạng, chỉ số, giấc ngủ, địa điểm, nhãn, chèn ảnh.
2. Client gửi `POST /journal-entries` kèm header `Idempotency-Key` (8–160 ký tự `[A-Za-z0-9._:-]`).
3. Backend (`JournalService.java:65-105`):
   1. kiểm tra dữ liệu và giới hạn (xem [README](README.md#3-business-rule-và-phân-quyền-chung));
   2. giữ chỗ idempotency theo key + hash nội dung request;
   3. làm sạch nội dung TipTap, trích văn bản thuần;
   4. sàng lọc an toàn tiêu đề + văn bản;
   5. lưu bài mã hóa, `localDate` tính theo `timezone` của bài;
   6. gắn nhãn, gắn ảnh (ảnh đầu tiên là ảnh bìa);
   7. ghi sự kiện safety, phát outbox.
4. Trả `201` với bài đầy đủ, `ETag`, `Location`.

## 4. Luồng phụ và ngoại lệ

- **Gửi lại cùng `Idempotency-Key` và cùng nội dung:** trả lại bài đã tạo, không tạo bài mới.
- **Cùng key nhưng khác nội dung:** `409`.
- **Thiếu hoặc sai `Idempotency-Key`:** `400`.
- **Không có `occurredAt`:** dùng thời điểm hiện tại.
- **Quá 20 ảnh:** `400` "Mỗi bài viết chỉ được đính kèm tối đa 20 hình ảnh."
- **Ảnh không thuộc user hoặc đã gắn bài khác:** `400`; ảnh tạm đã hết hạn: `409` "Vui lòng tải lại ảnh."
- **Sàng lọc phát hiện nguy cơ:** bài vẫn lưu, `analysisStatus = BLOCKED_BY_SAFETY`, `riskLevel` tương ứng; client hiển thị hỗ trợ an toàn.
- **Classifier lỗi (`FAIL_SAFE`):** bài vẫn lưu, bị chặn phân tích, hệ thống sàng lọc lại sau qua outbox.
- **Quá quota ghi:** `429`.

## 5. Business rule

- Body: `title?`, `contentJson` (bắt buộc), `location?`, `occurredAt?`, `timezone` (bắt buộc), `moodCode?`, `moodScore?`, `stressScore?`, `energyScore?`, `sleepMinutes?`, `tagIds?` (≤ 20).
- Bài được lưu dù AI hay safety gặp lỗi; AI không chạy trong request.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/journal-entries` | `Idempotency-Key` + body | `201` JournalEntry | 400, 409, 429 |
| POST | `/api/v1/journal-assets/upload` | xem UC-11 | `201` | 400 |

## 7. Giao diện web và mobile

- **Web:** `app/(app)/journal-editor` (`JournalEditor`, `JournalTipTapEditor`, `JournalMoodBar`, `MoodBattery`, công cụ bình tĩnh). Bản nháp giữ trong `localStorage` (chỉ là tiện ích giao diện). `LegacyJournalImport` chuyển bài cũ từ trình duyệt lên backend.
- **Mobile:** `(app)/journal-editor.tsx`, editor văn bản thuần; bản nháp theo user trong SecureStore.

## 8. Tiêu chí chấp nhận

- [x] Bài được mã hóa, chỉ chủ sở hữu đọc được, idempotent và có cổng safety (`journalIsEncryptedOwnerScopedIdempotentAndSafetyGated`).
- [x] Nhãn được trả về cùng bài và thay thế được (`journalEntriesIncludeTagsInResponseAndSupportBatchLoadingAndReplacing`).
- [x] Làm sạch nội dung TipTap (`TipTapContentTest`).
- [x] Web: chèn ảnh (`journal-image-insert.spec.ts`), công cụ bình tĩnh (`journal-editor-calming-tools.spec.ts`).

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `JournalCheckinIntegrationTest`, `TipTapContentTest` |
| Web | đã kiểm chứng | Playwright mục 8 |
| Mobile | đang làm | Chỉ văn bản thuần, chưa có ảnh |

## 10. Câu hỏi mở

- Mobile sẽ dùng editor rich text tương thích TipTap hay giữ văn bản thuần?
