# 12 — Xuất dữ liệu, bắt đầu lại và xóa tài khoản

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code và quyết định ngày 2026-11-25, **chờ xác nhận**. Backend đã làm theo spec này (2026-11-25); phần web của xóa tài khoản và tài khoản Google khi tải export còn chưa làm.

Quyền của người dùng đối với dữ liệu của mình: lấy bản sao, xóa toàn bộ dữ liệu nhưng giữ tài khoản ("bắt đầu lại"), và xóa hẳn tài khoản.

- **Module backend:** `com.mylog.export` (xuất), `com.mylog.user` (bắt đầu lại, xóa tài khoản), `com.mylog.journal` (hàng đợi xóa file ảnh)
- **API gốc:** `/api/v1/exports`, `/api/v1/me/data:reset`, `/api/v1/account-deletion-requests`
- **Web:** [`features/export`](../../front-end/my-app-web/features/export), `StartOverCard` và mục "Dữ liệu" trong [`features/user/components/SettingsView.tsx`](../../front-end/my-app-web/features/user/components/SettingsView.tsx)
- **Mobile:** [`src/features/user/export-api.ts`](../../front-end/my-app-mobile/src/features/user/export-api.ts), [`src/app/(app)/settings.tsx`](<../../front-end/my-app-mobile/src/app/(app)/settings.tsx>)
- **Migration:** `V14`, `V23` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`adr/0007-encrypted-database-export-artifacts.md`](../../backend/docs/adr/0007-encrypted-database-export-artifacts.md)

## 1. Mục tiêu và phạm vi

Người dùng kiểm soát dữ liệu nhạy cảm của mình: lấy về được, và xóa thật được (kể cả ảnh đã lưu ở dịch vụ ngoài).

- **Trong phạm vi:** xuất CSV/PDF qua job bất đồng bộ; "bắt đầu lại"; xóa tài khoản có thời gian chờ 7 ngày, hủy bằng cách đăng nhập lại; hàng đợi xóa file ảnh dùng chung.
- **Ngoài phạm vi:** xuất JSON, nhập lại dữ liệu từ file export, xóa từng phần do người dùng chọn. Xóa một bài nhật ký thuộc [UC-10](../03-journal/UC-10-edit-favorite-delete.md).

## 2. Use case

- [UC-22 — Xuất dữ liệu cá nhân](UC-22-export-data.md)
- [UC-23 — Xóa tài khoản](UC-23-delete-account.md)
- [UC-29 — Bắt đầu lại](UC-29-start-over.md)

## 3. Business rule và phân quyền chung

- Mọi endpoint cần bearer token và lọc theo `userId` hiện tại; tài nguyên của người khác trả `404`.
- **Xác nhận lại danh tính** cho thao tác nhạy cảm (tải file export, bắt đầu lại, xóa tài khoản), dùng chung một quy tắc:
  - tài khoản có mật khẩu: nhập mật khẩu, tối đa 5 lần sai / 15 phút cho mỗi loại thao tác; sai → `403 PASSWORD_INCORRECT`, quá số lần → `429`;
  - tài khoản chỉ Google: phiên hiện tại phải được tạo trong vòng 10 phút; không thỏa → `403 RECENT_LOGIN_REQUIRED`, client hiện thông báo yêu cầu đăng nhập lại.
- Job xuất, job xóa tài khoản và job xóa file chạy trong worker (không chạy khi `mylog.app-profile=api`), claim bằng `FOR UPDATE SKIP LOCKED`.
- Cờ cấu hình:

  | Cờ | Mặc định | Tác dụng |
  |---|---|---|
  | `mylog.exports.enabled` | `true` | Worker xuất |
  | `mylog.account-deletion.enabled` | `false` | Bật cả endpoint lẫn worker xóa tài khoản. Tắt → endpoint `404` |

- Toàn bộ feature chỉ bật khi `mylog.identity.enabled=true`.
- Không có quyền quản trị nào đọc file export hay thực hiện thay người dùng.

## 4. API contract

| Method | Path | Request | Response | Lỗi chính |
|---|---|---|---|---|
| POST | `/exports` | `{format: "CSV" \| "PDF"}` | `202` `ExportResponse` | 400, 409, 429 |
| GET | `/exports/{id}` | — | `200` `ExportResponse` | 404 |
| POST | `/exports/{id}:authorize-download` | `{password?}` | `200` `{url, expiresAt}` | 403, 404, 429 |
| GET | `/exports/{id}/file?expires=&signature=` | bearer | `200` file, `Cache-Control: no-store` | 401, 404 |
| POST | `/me/data:reset` | `{password?, confirm: true}` | `204` | 400, 403, 429 |
| POST | `/account-deletion-requests` | `{password?}` | `202` `DeletionResponse` | 403, 404 khi tắt, 409, 429 |

- `ExportResponse` = `{id, format, status, createdAt, completedAt, expiresAt}`.
- `DeletionResponse` = `{id, status, requestedAt, scheduledFor, completedAt}`.
- Đăng nhập (`/auth/login`, `/auth/google`) trả thêm `deletionCancelled`, xem [UC-23](UC-23-delete-account.md).
- `:status` và `:cancel` của yêu cầu xóa bị bỏ.

Đường dẫn tính từ `/api/v1`. Lỗi trả `application/problem+json`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `export_requests` | Yêu cầu xuất, file mã hóa, trạng thái, lease, `expires_at`. Mỗi user tối đa một yêu cầu `PENDING`/`PROCESSING`/`READY` | V14 |
| `deletion_requests` | Yêu cầu xóa; `user_id` set `NULL` khi xong; `subject_hash` giữ để đối chiếu. Mỗi user tối đa một yêu cầu `GRACE_PERIOD`/`PROCESSING`/`FAILED` | V14 |
| `asset_deletion_queue` | File ảnh chờ xóa khỏi kho lưu trữ: `public_id`, `asset_type`, `delivery_type`, `attempt`, `available_at`, `last_error_code`. Không có FK tới `users` để còn tồn tại sau khi tài khoản bị xóa. Bật RLS, thu hồi grant `anon`/`authenticated` (ADR-0008) | V23 |
| `auth_rate_limits` | Quota xuất (3/24 giờ), giới hạn nhập sai mật khẩu | V2 |
| `audit_logs` | `DATA_RESET`, `ACCOUNT_DELETION_CANCELLED` | V2 |

Bản ghi export `EXPIRED` bị xóa sau 30 ngày; `deletion_requests` đã `COMPLETED`/`CANCELLED` bị xóa sau 365 ngày.

### Hàng đợi xóa file

- "Bắt đầu lại" và xóa tài khoản ghi file cần xóa vào hàng đợi **trong cùng transaction** với việc xóa dữ liệu, nên không bao giờ mất dấu file.
- Job xóa file (mỗi lần một lô, `FOR UPDATE SKIP LOCKED`): xóa thành công hoặc kho trả "not found" → xóa dòng; lỗi → tăng `attempt`, thử lại với backoff tối đa 24 giờ. Sau 10 lần lỗi vẫn giữ dòng và hiện ở màn hình vận hành.
- Thay cho cách hiện tại: "bắt đầu lại" xóa file sau commit và bỏ qua lỗi (`JournalAssetService.java:178-185`); xóa tài khoản gọi Cloudinary trực tiếp trước khi purge (`CloudinaryJournalAssetDeletion.java`).

## 6. Safety, quyền riêng tư và consent

- File export chứa nhật ký và email dạng rõ: mã hóa khi lưu, chỉ chủ sở hữu tải được, URL có chữ ký sống 60 giây, hết hạn sau 24 giờ.
- Không cần consent riêng để xuất, bắt đầu lại hay xóa tài khoản.
- Backend không dùng Redis cho dữ liệu người dùng, nên không có cache cần xóa.
- Dữ liệu đã gửi provider AI không xóa được từ code; provider chỉ được duyệt khi không lưu dữ liệu hoặc có cơ chế xóa (release gate).
- Không log nội dung file, mật khẩu, email, URL có chữ ký, `public_id` của ảnh.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M7IntegrationTest`, `DataResetIntegrationTest`, `AccountDeletionDisabledTest`, `AssetDeletionQueueJobTest`, `GoogleAuthFlowTest`. Chưa thử với Cloudinary thật |
| Web | đang làm | Xuất và bắt đầu lại xong; xóa tài khoản chưa có |
| Mobile | đang làm | Có xuất; bắt đầu lại và xóa tài khoản chờ quyết định phát hành mobile |

## 8. Câu hỏi mở

1. Mobile làm bắt đầu lại và xóa tài khoản khi nào (phụ thuộc quyết định phát hành trên store)?
