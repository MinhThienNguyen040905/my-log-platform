# UC-09 — Đọc, tìm và xem lại nhật ký

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Nhật ký](README.md)
- **Catalog:** UC-09 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập.
- **Hậu điều kiện:** không thay đổi dữ liệu.

## 3. Luồng chính

**Danh sách và lọc**

1. Client gọi `GET /journal-entries` với bộ lọc tùy chọn: khoảng ngày (`from`, `to` theo `localDate`), nhãn (`tag`), yêu thích (`favorite`).
2. Backend trả trang bài sắp xếp mới nhất trước theo `occurredAt`, kèm `nextCursor` nếu còn (`JournalService.java:122-144`). Mỗi bài có nhãn và URL ảnh bìa (thumbnail 400×400).

**Tìm kiếm**

1. Người dùng nhập từ khóa → `GET /journal-entries/search?query=...`.
2. Backend giải mã tiêu đề của các bài gần nhất (tối đa 1.000 bài), so khớp **không phân biệt dấu tiếng Việt và hoa thường** (`VietnameseStringUtils`), trả trang kết quả (`JournalService.java:157-227`).

**Lịch**

1. Client gọi `GET /journal-entries/daily-counts?from=&to=` (tối đa 92 ngày).
2. Backend trả số bài và mã tâm trạng mới nhất cho mỗi ngày có bài, không giải mã nội dung.

**Xem chi tiết**

1. Client gọi `GET /journal-entries/{id}`; backend trả bài đầy đủ, ảnh trong nội dung được gắn URL có chữ ký mới.

## 4. Luồng phụ và ngoại lệ

- Bài không tồn tại, đã xóa hoặc của người khác: `404`.
- `limit` ngoài phạm vi, `from` sau `to`, cursor sai định dạng, khoảng lịch > 92 ngày: `400`.
- Từ khóa chỉ gồm ký tự bị bỏ khi chuẩn hóa: trả danh sách rỗng.

## 5. Business rule

- Cursor là base64url của `occurredAt|id`.
- `limit`: danh sách 1–100 (mặc định 20), tìm kiếm 1–50 (mặc định 5).
- Bài đã xóa mềm không xuất hiện ở bất kỳ endpoint nào.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/journal-entries` | `from?`, `to?`, `tag?`, `favorite?`, `cursor?`, `limit?` | `200 {items, nextCursor}` | 400 |
| GET | `/api/v1/journal-entries/search` | `query`, `cursor?`, `limit?` | `200 {items, nextCursor}` | 400 |
| GET | `/api/v1/journal-entries/daily-counts` | `from`, `to` | `200 {from, to, days}` | 400 |
| GET | `/api/v1/journal-entries/{id}` | — | `200` JournalEntry + `ETag` | 404 |

## 7. Giao diện web và mobile

- **Web:** `app/(app)/history-calendar` (lịch, `features/calendar`), tìm kiếm trong journal; chi tiết mở trong editor.
- **Mobile:** `(app)/journal.tsx`, `(app)/history.tsx` (lọc theo ngày), `(app)/journal-entry/[id].tsx`, tìm kiếm.

## 8. Tiêu chí chấp nhận

- [x] Đếm theo ngày chỉ trong phạm vi chủ sở hữu, theo `localDate`, bỏ bài đã xóa (`dailyCountsForCalendarAreOwnerScopedByLocalDateAndSkipDeleted`).
- [x] Tìm theo tiêu đề có phân trang và khớp không dấu (`searchByTitlePaginatesAndMatchesDiacritics`).
- [x] Web: `calendar-daily-counts.spec.ts`.
- [ ] Test phân trang nhiều trang trên mobile.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `JournalCheckinIntegrationTest` |
| Web | đã kiểm chứng | Lịch có Playwright |
| Mobile | xong | Chưa kiểm tra cursor nhiều trang |

## 10. Câu hỏi mở

- Xem câu hỏi 1 ở [README](README.md#8-câu-hỏi-mở) về tìm theo nội dung và giới hạn 1.000 bài.
