# UC-26 — Xem và xử lý phản hồi người dùng

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

- **Feature:** [Phản hồi](README.md)
- **Catalog:** UC-26 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Nhân viên có vai trò `SUPPORT_AGENT` hoặc `SYSTEM_ADMIN` (quyền `feedback:read`, `feedback:manage`).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đăng nhập với tài khoản có quyền tương ứng.
- **Hậu điều kiện:** phản hồi được đổi trạng thái và/hoặc người phụ trách; `version` tăng 1; mỗi lần đọc và cập nhật đều có audit.

## 3. Luồng chính

1. Nhân viên mở trang admin, tab phản hồi → `GET /admin/feedback` (lọc theo trạng thái nếu cần).
2. Backend trả tối đa 50 phản hồi mới nhất, kèm nội dung, và ghi một audit `FEEDBACK_READ` cho **mỗi** phản hồi trong danh sách (`FeedbackService.java:56-63`).
3. Nhân viên chọn một phản hồi → `GET /admin/feedback/{id}` (thêm một audit `FEEDBACK_READ`).
4. Nhân viên chọn trạng thái mới và người phụ trách → `PATCH /admin/feedback/{id}` với `If-Match: <version>`.
5. Backend cập nhật, ghi audit `FEEDBACK_UPDATED`, trả bản mới.

## 4. Luồng phụ và ngoại lệ

- Thiếu quyền: `403`. Không có token: `401`.
- `status` lọc hoặc cập nhật không thuộc `OPEN`/`IN_PROGRESS`/`RESOLVED`/`CLOSED`, `limit` ngoài 1–50: `400`.
- `If-Match` không phải số: `400`. `version` không khớp: `409`.
- `assignedTo` không phải tài khoản `ACTIVE` có vai trò `SUPPORT_AGENT`/`SYSTEM_ADMIN`: `400`.
- Phản hồi không tồn tại hoặc đã hết hạn bị xóa: `404`.

## 5. Business rule

- Chuyển trạng thái **tự do** giữa bốn giá trị, không có máy trạng thái.
- Chuyển sang `RESOLVED` hoặc `CLOSED` thì ghi `resolvedAt`; chuyển về `OPEN`/`IN_PROGRESS` thì xóa `resolvedAt` (`JpaFeedbackStore.java:57`).
- Gửi `assignedTo = null` giữ nguyên người phụ trách cũ; hiện **không có cách bỏ phân công**.
- Quyền xem nội dung phản hồi tách khỏi quyền quản trị chung: chỉ vai trò có `feedback:read` mới xem được.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/admin/feedback?status=&limit=` | — | `200 [FeedbackResponse]` | 400, 401, 403 |
| GET | `/api/v1/admin/feedback/{id}` | — | `200 FeedbackResponse` | 401, 403, 404 |
| PATCH | `/api/v1/admin/feedback/{id}` | `If-Match`, `{status, assignedTo?}` | `200 FeedbackResponse` | 400, 401, 403, 404, 409 |

## 7. Giao diện web và mobile

- **Web:** `features/admin/components/AdminFeedbackTable.tsx` trong `AdminConsoleView` (trang `/admin`): lọc theo trạng thái, chọn phiếu, đổi trạng thái, nhập id người phụ trách.
- **Mobile:** không áp dụng (chức năng quản trị chỉ có trên web).

## 8. Tiêu chí chấp nhận

- [x] Không có quyền → `403`; có `feedback:read` → đọc được nội dung (`M7IntegrationTest`).
- [x] Mỗi lần đọc ghi `FEEDBACK_READ`; cập nhật ghi `FEEDBACK_UPDATED`.
- [x] Chỉ có `feedback:read` thì `PATCH` → `403`; có `feedback:manage` → cập nhật, `version` tăng.
- [x] `If-Match` cũ → `409`.
- [ ] Test `assignedTo` không hợp lệ → `400`.
- [ ] Playwright cho bảng phản hồi admin.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M7IntegrationTest` |
| Web | xong | Chưa có Playwright |
| Mobile | — | Không áp dụng |

## 10. Câu hỏi mở

1. Danh sách ghi audit cho mọi phản hồi trong trang (tối đa 50 dòng mỗi lần mở). Có cần danh sách không kèm nội dung để giảm việc đọc dữ liệu nhạy cảm và giảm audit không?
2. Có cần bỏ phân công và máy trạng thái (ví dụ không mở lại phiếu `CLOSED`) không?
3. Có cần trả lời người dùng hoặc báo cho họ khi phiếu được xử lý không?
