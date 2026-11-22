# UC-07 — Nhận gợi ý để viết tiếp

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Nhật ký](README.md)
- **Catalog:** UC-07 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

Gợi ý viết tiếp được hiện thực bằng **companion** trong trình soạn thảo. File này mô tả phần người dùng thấy khi viết; chi tiết hội thoại, công cụ và thói quen nằm ở [10-companion](../10-companion.md).

## 1. Tác nhân

Người dùng đang viết nhật ký.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập. Để nhận câu trả lời thật: tính năng được bật (`mylog.writing-suggestions.enabled=true`) và đã bật consent `AI_PROCESSING`.
- **Hậu điều kiện:** không lưu gì vào bài viết; người dùng tự chọn có dùng gợi ý hay không.

## 3. Luồng chính

**Gợi ý tiêu đề**

1. Người dùng chọn tâm trạng.
2. Client gọi `GET /companion/title-suggestions?mood=<mood>`; backend trả danh sách tiêu đề tĩnh theo tâm trạng (`TitleSuggestionCatalog`), không dùng AI, không đọc bài viết.

**Gợi ý viết tiếp**

1. Người dùng mở companion hoặc gửi một câu.
2. Client gọi `POST /journal-writing-suggestions` với `intent` (`OPEN`, `FREE_TEXT`, `TOOL_COMPLETED`, `CLOSING`), `message`, bản nháp và tối đa 8 lượt hội thoại trước.
3. Backend lần lượt kiểm tra (`WritingSuggestionService.java:52-70`):
   - tính năng tắt → `UNAVAILABLE`;
   - thiếu consent → `CONSENT_REQUIRED`;
   - sàng lọc an toàn trên **cả cửa sổ hội thoại** (bản nháp và các lượt trước của người dùng): nguy cơ cao → `SAFETY_FLOW`; cần hạn chế → `BLOCKED` kèm câu trả lời trung tính; lỗi → `UNAVAILABLE`;
   - được phép → `REPLY` với câu trả lời, câu hỏi gợi mở, tiêu đề gợi ý, hành động tức thì (công cụ bình tĩnh), chips.

## 4. Luồng phụ và ngoại lệ

- Thiếu cả `message` lẫn `text` với `intent` cần nội dung: `400`.
- Quá 40 lượt/15 phút: `429`.
- `SAFETY_FLOW`: client mở luồng hỗ trợ an toàn thay vì hiển thị trả lời.
- Nội dung bản nháp không bao giờ được gửi tới bộ sinh câu trả lời khi sàng lọc không cho phép (`WritingSuggestionServiceTest.moderateAndFailureDoNotCallResponder`).

## 5. Business rule

- `text` ≤ 4.000 ký tự, `message` ≤ 300, `history` ≤ 8 lượt, `completedTool` ≤ 32.
- Gọi chỉ với `text` (không `intent`) là contract cũ, vẫn được hỗ trợ và tính là `FREE_TEXT`.
- `habitSuggestion` có thể có giá trị (gợi ý tạo thói quen từ mẫu hoặc ghi nhận thói quen đã có); quy tắc ở [10-companion](../10-companion.md). Javadoc của `WritingSuggestionResponse` ghi "luôn null" là đã cũ.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/companion/title-suggestions` | `mood?` | `200 {titles}` | 400, 401 |
| POST | `/api/v1/journal-writing-suggestions` | `text?`, `intent?`, `message?`, `draft?`, `context?`, `completedTool?`, `history?` | `200 {status, reply, questions, suggestedTitle, immediateActions, habitSuggestion, chips, nextState, source, promptVersion}` | 400, 401, 429 |

## 7. Giao diện web và mobile

- **Web:** panel companion trong editor (`features/journal/components/companion`, `useCompanionConversation.ts`). Có mock chỉ dùng khi dev bật rõ ràng (`features/journal/api/companion.mock.ts`).
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [x] Tắt tính năng hoặc thiếu consent thì không sàng lọc và không gửi bản nháp (`WritingSuggestionServiceTest`).
- [x] Chỉ gọi bộ sinh khi được phép, không lộ bản nháp (`WritingSuggestionServiceTest`).
- [x] Cửa sổ sàng lọc gồm cả các lượt trước (`WritingSuggestionServiceTest`).
- [x] Cần đăng nhập và input hợp lệ; gợi ý tiêu đề tĩnh theo tâm trạng (`JournalCheckinIntegrationTest`).
- [x] Web: `journal-editor-companion.spec.ts`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Mặc định **tắt** bằng cấu hình |
| Web | xong | |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

- Provider sinh câu trả lời (`LlmCompanionResponder`) đã được duyệt để xử lý nội dung nhật ký chưa? Xem [10-companion](../10-companion.md).
