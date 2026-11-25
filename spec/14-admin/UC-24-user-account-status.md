# UC-24 — Quản lý trạng thái tài khoản người dùng

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

- **Feature:** [Quản trị](README.md)
- **Catalog:** UC-24 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

- Xem metadata: `SYSTEM_ADMIN`, `SUPPORT_AGENT` (quyền `users:read-metadata`).
- Đình chỉ, khôi phục: `SYSTEM_ADMIN` (quyền `users:suspend`).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đăng nhập với quyền tương ứng.
- **Hậu điều kiện:**
  - Đình chỉ: tài khoản `SUSPENDED`, mọi phiên bị thu hồi (lý do `ADMIN_SUSPEND`), người dùng không đăng nhập được; ghi audit `USER_SUSPENDED` kèm `reasonCode`.
  - Khôi phục: tài khoản `ACTIVE`, người dùng đăng nhập lại được; ghi audit `USER_RESTORED`.

## 3. Luồng chính

1. Quản trị viên mở tab người dùng → `GET /admin/users` (phân trang cursor theo `id` giảm dần, bỏ qua tài khoản đã xóa).
2. Mở chi tiết → `GET /admin/users/{userId}/metadata`: chỉ có `id`, `status`, `createdAt`, `lastLoginAt`, `version`.
3. Chọn "Đình chỉ" hoặc "Khôi phục", chọn lý do → `POST :suspend` / `:restore` với `{reasonCode}`.
4. Backend đổi trạng thái có điều kiện (`ACTIVE → SUSPENDED` hoặc `SUSPENDED → ACTIVE`), thu hồi phiên khi đình chỉ, ghi audit (`AdminAccountService.java:28-45`).

## 4. Luồng phụ và ngoại lệ

- Tự đình chỉ chính mình: `409`.
- Tài khoản không ở trạng thái mong đợi (ví dụ đình chỉ một tài khoản đang `DELETION_PENDING`): `409`.
- `reasonCode` không khớp `[A-Z0-9_]{3,60}`: `400`.
- Xem metadata tài khoản không tồn tại hoặc đã xóa: `404`. Đình chỉ/khôi phục tài khoản không tồn tại: `409` (cùng nhánh "sai trạng thái").
- `limit` ngoài 1–100: `400`.

## 5. Business rule

- Không trả email hay bất kỳ nội dung người dùng nào.
- Backend nhận mọi `reasonCode` đúng định dạng; danh sách lý do cố định chỉ có ở web.
- Khôi phục không khôi phục các phiên đã bị thu hồi; người dùng phải đăng nhập lại.
- Đình chỉ không gửi thông báo cho người dùng.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/admin/users?cursor=&limit=` | — | `200 {items, nextCursor}` | 400, 401, 403 |
| GET | `/api/v1/admin/users/{userId}/metadata` | — | `200 {id, status, createdAt, lastLoginAt, version}` | 401, 403, 404 |
| POST | `/api/v1/admin/users/{userId}:suspend` | `{reasonCode}` | `200` như trên | 400, 401, 403, 409 |
| POST | `/api/v1/admin/users/{userId}:restore` | `{reasonCode}` | `200` như trên | 400, 401, 403, 409 |

## 7. Giao diện web và mobile

- **Web:** `AdminUserTable.tsx` (danh sách, tìm theo id hoặc trạng thái), `AdminUserMetadataDrawer.tsx`, `AdminUserActionModal.tsx`. Lý do đình chỉ: `SECURITY_VIOLATION`, `SUSPICIOUS_LOGIN_BURST`, `USER_ACCOUNT_LOCKED_REQUEST`, `MALICIOUS_INPUT_PATTERN`. Lý do khôi phục: `APPEAL_APPROVED`, `VERIFICATION_COMPLETED`, `MISTAKEN_SUSPENSION_RESOLVED`.
- **Mobile:** không áp dụng.

## 8. Tiêu chí chấp nhận

- [x] Không token → `401`; người dùng thường → `403`; có quyền → danh sách không có email (`M6IntegrationTest`).
- [x] Đình chỉ và khôi phục qua HTTP đổi đúng trạng thái.
- [ ] Test `GET /admin/users/{id}/metadata`.
- [ ] Test tự đình chỉ → `409`, sai trạng thái → `409`, `reasonCode` sai → `400`.
- [ ] Test đình chỉ thu hồi phiên và ghi audit kèm `reasonCode`.
- [ ] Playwright cho trang admin.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M6IntegrationTest`; một số nhánh lỗi chưa có test |
| Web | xong | Chưa có Playwright |
| Mobile | — | Không áp dụng |

## 10. Câu hỏi mở

1. Một `SYSTEM_ADMIN` có thể đình chỉ một `SYSTEM_ADMIN` khác. Có cần chặn hay yêu cầu hai người duyệt không?
2. Danh sách lý do nằm ở web, backend nhận mọi mã đúng định dạng. Có cần chốt danh sách ở backend không?
3. Có cần báo cho người dùng khi bị đình chỉ và cách khiếu nại không?
