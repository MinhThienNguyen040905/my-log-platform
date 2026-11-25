# UC-14 — Nhận hướng dẫn an toàn khi có dấu hiệu nguy cơ

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Safety](README.md)
- **Catalog:** UC-14 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đang viết hoặc vừa lưu nội dung có dấu hiệu nguy cơ.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** người dùng lưu bài nhật ký hoặc gửi tin nhắn cho companion.
- **Hậu điều kiện:**
  - nội dung **vẫn được lưu** (nhật ký);
  - không có phản hồi AI thông thường cho nội dung đó;
  - người dùng thấy thông điệp hỗ trợ và nguồn hỗ trợ đã duyệt (nếu có);
  - người dùng tự quyết định có liên hệ hay không.

## 3. Luồng chính

**Khi lưu nhật ký**

1. Người dùng lưu bài (UC-08). Backend sàng lọc trong cùng transaction.
2. Bài rủi ro `HIGH`/`CRITICAL` được lưu với `analysisStatus = BLOCKED_BY_SAFETY`.
3. Web nhận `riskLevel` trong response:
   - `HIGH`/`CRITICAL` → mở `SafetyModal` và **ở lại** trang (không chuyển sang lịch);
   - mức khác → về trang lịch như bình thường (`useJournalEditor.ts:241-247`).
4. `SafetyModal` hiển thị: lời trấn an, nhắc rằng AI không thay thế hỗ trợ trực tiếp, gợi ý liên hệ người tin cậy hoặc dịch vụ khẩn cấp nơi mình sống, và danh sách nguồn đã xác minh lấy từ `GET /safety/resources?locale=vi-VN&country=VN`.
5. Mở lại bài sau đó: panel phân tích hiển thị trạng thái bị chặn, **không** có nút thử lại, có lối vào nguồn hỗ trợ.

**Khi trò chuyện với companion**

1. Backend sàng lọc cả cửa sổ hội thoại; nguy cơ cao → `status = SAFETY_FLOW`, không gọi bộ sinh câu trả lời.
2. Client mở luồng hỗ trợ an toàn thay vì hiển thị câu trả lời.

**Từ dashboard**

- Web có nút mở `SafetyModal` bất cứ lúc nào trên dashboard.

## 4. Luồng phụ và ngoại lệ

- **Chưa có nguồn đã duyệt** (hiện tại): danh sách rỗng; modal vẫn hiện lời nhắc chung và ghi "Thông tin liên hệ cụ thể sẽ được hiển thị khi đã được xác minh."
- **Classifier lỗi/không có:** bài thành `FAIL_SAFE` (`riskLevel = UNKNOWN`), không mở modal, chỉ chặn phân tích; hệ thống sàng lọc lại sau.
- **Mức `MODERATE`:** không mở modal; phân tích thường bị chặn, companion trả câu trung tính.

## 5. Business rule

- Hệ thống không tự bịa số điện thoại, không chẩn đoán.
- Nguồn hỗ trợ chỉ hiển thị khi đã duyệt, đã xác minh, có `sourceUrl` HTTPS (xem UC-17).
- Mức rủi ro hiển thị cho client chỉ dùng để chọn giao diện; không hiển thị nhãn rủi ro cho người dùng.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST / PATCH | `/api/v1/journal-entries[/{id}]` | xem [03-journal](../03-journal/README.md) | `riskLevel`, `analysisStatus` trong response | |
| POST | `/api/v1/journal-writing-suggestions` | xem [10-companion](../10-companion.md) | `status = SAFETY_FLOW` | |
| GET | `/api/v1/safety/resources` | `locale`, `country` | danh sách nguồn | 400 |

## 7. Giao diện web và mobile

- **Web:** `SafetyModal` (sau khi lưu, trên dashboard, trong `EntryAnalysisPanel`).
- **Mobile:** `safety-support-modal.tsx`, mở khi `riskLevel` là `HIGH`/`CRITICAL` (`needsSafetySupport`).

## 8. Tiêu chí chấp nhận

- [x] Bộ câu synthetic tiếng Việt/Anh đi đúng luồng; classifier lỗi không mở được phân tích; `MODERATE` không vào luồng phản chiếu tự do (`SafetyScreeningCorpusTest`).
- [x] Luật từ khóa vẫn đưa nội dung khủng hoảng vào `SAFETY_FLOW` khi bật dev-allow (`DevSafetyAllowConfigurationTest`).
- [x] Web: bài bị chặn không có nút thử lại, mở được nguồn hỗ trợ (`entry-analysis.spec.ts`).
- [x] Mobile: luồng safety và phân tích qua API (`scripts/e2e-safety-analysis.mjs`).
- [ ] Nội dung thông điệp hỗ trợ được người có chuyên môn duyệt, theo ngôn ngữ.
- [ ] Safety eval đạt ngưỡng đã chốt.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Thiếu policy và nội dung đã duyệt |
| Web | xong | Lời nhắn viết cứng trong code |
| Mobile | xong | Chưa chạy trên thiết bị thật |

## 10. Câu hỏi mở

- Xem câu hỏi 1, 2, 4 ở [README](README.md#8-câu-hỏi-mở).
- `locale=vi-VN&country=VN` đang cố định trong web; có lấy theo hồ sơ người dùng không?
