# 13 — Phản hồi

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

Người dùng gửi góp ý, báo lỗi hoặc đề xuất; nhân viên hỗ trợ xem, phân công và xử lý.

- **Module backend:** `com.mylog.feedback`
- **API gốc:** `/api/v1/feedback`, `/api/v1/admin/feedback`
- **Web:** [`features/feedback`](../../front-end/my-app-web/features/feedback), [`features/admin/components/AdminFeedbackTable.tsx`](../../front-end/my-app-web/features/admin/components/AdminFeedbackTable.tsx)
- **Mobile:** —
- **Migration:** `V14` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** —

## 1. Mục tiêu và phạm vi

Thu thập ý kiến của người dùng để cải thiện sản phẩm, đồng thời bảo vệ nội dung phản hồi như dữ liệu nhạy cảm.

- **Trong phạm vi:** gửi phản hồi theo loại (`BUG`, `IDEA`, `OTHER`), xem lại phản hồi của mình và trạng thái xử lý; nhân viên xem hàng đợi, đổi trạng thái, phân công người xử lý.
- **Ngoài phạm vi (hiện tại):** phản hồi gắn với một mục cụ thể (một phản chiếu AI, một bài kiến thức, một gợi ý); đánh giá hữu ích / không hữu ích; trả lời người dùng. Phản hồi về kiểu suy nghĩ thuộc [06-thinking-patterns](../06-thinking-patterns.md).

## 2. Use case

- [UC-21 — Gửi phản hồi về nội dung hoặc gợi ý](UC-21-send-feedback.md)
- [UC-26 — Xem và xử lý phản hồi người dùng](UC-26-review-feedback.md)

## 3. Business rule và phân quyền

- Người dùng chỉ xem được phản hồi của mình; phản hồi của người khác → `404`.
- Quyền quản trị:

  | Quyền | Vai trò có quyền (V14) | Cho phép |
  |---|---|---|
  | `feedback:read` | `SYSTEM_ADMIN`, `SUPPORT_AGENT` | Xem danh sách và chi tiết, kể cả nội dung |
  | `feedback:manage` | `SYSTEM_ADMIN`, `SUPPORT_AGENT` | Đổi trạng thái, phân công |

- Mỗi lần nhân viên đọc một phản hồi (kể cả trong danh sách) ghi một audit `FEEDBACK_READ`; mỗi lần cập nhật ghi `FEEDBACK_UPDATED`.
- Chỉ phân công được cho tài khoản `ACTIVE` có vai trò `SUPPORT_AGENT` hoặc `SYSTEM_ADMIN` còn hiệu lực.
- Phản hồi bị xóa tự động **180 ngày** sau khi tạo, bất kể trạng thái (`JpaFeedbackStore.java:34`, job chạy mỗi 24 giờ).

## 4. API contract

| Method | Path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|---|
| POST | `/feedback` | người dùng | `{category, message}` | `202 {id}` | 400 |
| GET | `/feedback?limit=` | người dùng | `limit` mặc định 20, tối đa 50 | `200 [FeedbackResponse]` mới nhất trước | 401 |
| GET | `/feedback/{id}` | người dùng | — | `200 FeedbackResponse` | 404 |
| GET | `/admin/feedback?status=&limit=` | `feedback:read` | `status` tùy chọn, `limit` 1–50 (mặc định 50) | `200 [FeedbackResponse]` mới nhất trước | 400, 403 |
| GET | `/admin/feedback/{id}` | `feedback:read` | — | `200 FeedbackResponse` | 403, 404 |
| PATCH | `/admin/feedback/{id}` | `feedback:manage` | header `If-Match: <version>`, `{status, assignedTo?}` | `200 FeedbackResponse` | 400, 403, 404, 409 |

`FeedbackResponse` = `{id, userId, category, status, message, assignedTo, createdAt, updatedAt, resolvedAt, version}`. Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `feedback` | `category`, `status` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), nội dung mã hóa (`encrypted_message` + IV + wrapped key), `assigned_to`, `resolved_at`, `expires_at`, `row_version` | V14 |
| `permissions`, `role_permissions` | Thêm `feedback:read`, `feedback:manage` | V14 |
| `audit_logs` | `FEEDBACK_READ`, `FEEDBACK_UPDATED` | V2 |

## 6. Safety, quyền riêng tư và consent

- Nội dung phản hồi được mã hóa khi lưu, nhưng nhân viên có `feedback:read` **đọc được bản rõ** kèm `userId`. Việc đọc được ghi audit.
- Phản hồi có trong file export ([UC-22](../12-export-deletion/UC-22-export-data.md)), bị xóa khi "bắt đầu lại" và khi xóa tài khoản.
- Không có sàng lọc safety cho nội dung phản hồi: người dùng có thể viết nội dung nguy cơ vào đây mà không nhận được hướng dẫn an toàn.
- Không cần consent riêng.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M7IntegrationTest.feedbackEncryptedAndAdminReadRequiresPermission` (qua HTTP) |
| Web | xong | Tab "Phản hồi" trong cài đặt; bảng phản hồi trong trang admin. Chưa có Playwright |
| Mobile | chưa làm | |

## 8. Câu hỏi mở

1. Catalog mô tả UC-21 là phản hồi **về một mục cụ thể** (phản chiếu AI, nội dung, gợi ý) và đánh giá hữu ích hay không. Code chỉ có phản hồi chung theo loại. Giữ phạm vi hiện tại hay mở rộng?
2. Gửi phản hồi **không có quota**. Có cần giới hạn để chống spam không?
3. Người dùng có thể viết nội dung nguy cơ vào phản hồi. Có cần chạy safety screening không?
4. Có chấp nhận việc nhân viên hỗ trợ đọc được nội dung phản hồi kèm `userId` không?
