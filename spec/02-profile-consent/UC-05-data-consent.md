# UC-05 — Chọn hoặc rút lại sự đồng ý xử lý dữ liệu

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Hồ sơ và đồng ý xử lý dữ liệu](README.md)
- **Catalog:** UC-05 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có hồ sơ.
- **Hậu điều kiện:** một dòng mới trong `user_consents` (nguồn `SETTINGS`); quyết định mới nhất của mỗi loại là quyết định có hiệu lực.

## 3. Luồng chính

1. Người dùng mở phần quyền riêng tư (cài đặt) hoặc bước consent trong onboarding.
2. Client gọi `GET /me/consents` để lấy quyết định mới nhất cho mỗi loại.
3. Người dùng bật hoặc tắt một loại → `PUT /me/consents/{type} {documentVersion, granted}`.
4. Backend ghi thêm quyết định, trả `204` (`UserProfileService.java:81-87`).

## 4. Luồng phụ và ngoại lệ

- `type` không thuộc danh sách hoặc `documentVersion` rỗng/dài hơn 40 ký tự: `400`.
- Loại chưa từng có quyết định (ví dụ `THINKING_PATTERNS`) không xuất hiện trong `GET /me/consents` và được coi là **tắt**.

## 5. Business rule

**Các loại consent và nơi dùng**

| Loại | Mặc định khi tạo tài khoản | Được kiểm tra ở |
|---|---|---|
| `TERMS` | đồng ý (bắt buộc khi đăng ký) | — |
| `PRIVACY` | đồng ý (bắt buộc khi đăng ký) | — |
| `AI_PROCESSING` | tắt | Phân tích nhật ký (`AnalysisJobHandler`, `AnalysisRetryService`), viết lại narrative insight (`NarrativeRewriter`), companion (`WritingSuggestionService`) |
| `THINKING_PATTERNS` | không có dòng (= tắt) | Nhận diện kiểu suy nghĩ (`PatternExtractionService`) |
| `ANALYTICS` | tắt | chưa có |
| `MODEL_TRAINING` | tắt | chưa có |

- Consent được kiểm tra **khi job/tính năng chạy**, nên rút lại có hiệu lực ngay cho các xử lý chưa chạy.
- Lịch sử chỉ thêm, không sửa hoặc xóa dòng cũ.
- Backend không chặn việc tắt `TERMS`/`PRIVACY`; giao diện web đánh dấu hai loại này là không tùy chọn.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/me/consents` | — | `200 [{type, documentVersion, granted, decidedAt}]` | 401 |
| PUT | `/api/v1/me/consents/{type}` | `documentVersion`, `granted` | `204` | 400, 401 |

## 7. Giao diện web và mobile

- **Web:** onboarding hỏi `AI_PROCESSING`, `ANALYTICS`, `MODEL_TRAINING` (`useOnboarding.ts`); cài đặt hiển thị cả 6 loại kèm mô tả dễ hiểu (`AccountAccessPanel.tsx`).
- **Mobile:** onboarding hỏi 3 loại tùy chọn như web; cài đặt hiển thị các consent đang có.

## 8. Tiêu chí chấp nhận

- [x] Đọc và ghi consent qua HTTP (`IdentityFlowIntegrationTest`).
- [x] Phân tích không chạy khi thiếu `AI_PROCESSING` (test của module analysis, xem [05-ai-analysis](../05-ai-analysis.md)).
- [x] Companion trả `CONSENT_REQUIRED` khi thiếu `AI_PROCESSING` (`WritingSuggestionServiceTest`).

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | |
| Web | xong | |
| Mobile | xong | Chưa có `THINKING_PATTERNS` |

## 10. Câu hỏi mở

- Backend có nên từ chối tắt `TERMS`/`PRIVACY` (hoặc coi đó là yêu cầu xóa tài khoản) không?
- Xem thêm câu hỏi mở 1–3 ở [README](README.md#8-câu-hỏi-mở).
