# 14 — Quản trị

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

Quản lý tài khoản, theo dõi vận hành, xử lý tác vụ lỗi, duyệt nguồn hỗ trợ và quy tắc safety. Chỉ có trên web.

- **Module backend:** `com.mylog.admin` (API, dashboard), `com.mylog.identity` (`AdminAccountService`), `com.mylog.analysis` (`AdminJobService`)
- **API gốc:** `/api/v1/admin`
- **Web:** [`features/admin`](../../front-end/my-app-web/features/admin), trang [`app/(app)/admin`](<../../front-end/my-app-web/app/(app)/admin/page.tsx>)
- **Mobile:** không áp dụng
- **Migration:** `V2` (RBAC, audit), `V7` (bảng safety), `V12` (vai trò và quyền quản trị) (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`M8_RELEASE_RUNBOOK.md`](../../backend/docs/M8_RELEASE_RUNBOOK.md), [`M8_THREAT_MODEL.md`](../../backend/docs/M8_THREAT_MODEL.md)

## 1. Mục tiêu và phạm vi

Cho nhân viên vận hành xử lý tài khoản và sự cố **mà không cần đọc nhật ký** của người dùng.

- **Trong phạm vi:** xem metadata tài khoản, đình chỉ và khôi phục; xem và retry job AI lỗi; dashboard số liệu tổng hợp có ngưỡng ẩn; (dự kiến) duyệt nguồn hỗ trợ và quy tắc safety.
- **Ngoài phạm vi:** đọc hoặc giải mã nhật ký; sửa dữ liệu người dùng; gán vai trò qua giao diện. Phản hồi người dùng thuộc [13-feedback](../13-feedback/README.md); duyệt kho kiến thức thuộc [11-knowledge](../11-knowledge.md).

## 2. Use case

- [UC-24 — Quản lý trạng thái tài khoản người dùng](UC-24-user-account-status.md)
- [UC-25 — Theo dõi vận hành và xử lý tác vụ lỗi](UC-25-operations-failed-jobs.md)
- [UC-28 — Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn](UC-28-safety-resources-rules.md) (**chưa làm**)

## 3. Business rule và phân quyền

Vai trò và quyền (V12, V14). Gán vai trò hiện chỉ làm trực tiếp trong DB (`user_roles`).

| Vai trò | Quyền |
|---|---|
| `SYSTEM_ADMIN` | `users:read-metadata`, `users:suspend`, `jobs:read`, `jobs:retry`, `admin:dashboard`, `feedback:read`, `feedback:manage` |
| `SUPPORT_AGENT` | `users:read-metadata`, `feedback:read`, `feedback:manage` |
| `SAFETY_ADMIN` | `safety:manage` (**chưa có endpoint nào dùng**) |
| `CONTENT_EDITOR` | `knowledge:write` |
| `CONTENT_APPROVER` | `knowledge:review` |
| `AUDITOR` | `audit:read` (**chưa có endpoint nào dùng**) |

- Mọi endpoint `/admin/*` có `@PreAuthorize` theo quyền: không token → `401`, thiếu quyền → `403`.
- Hồ sơ (`GET /me`) trả `isAdmin = true` khi người dùng có `SYSTEM_ADMIN`, `SAFETY_ADMIN`, `SUPPORT_AGENT` hoặc `admin:dashboard` (`UserController.java:112-120`); web dùng cờ này để hiện trang admin.
- Không endpoint quản trị nào trả email, nội dung nhật ký, payload job hay lỗi thô.
- Mọi hành động thay đổi đều ghi `audit_logs` với `actor_type = ADMIN`.

## 4. API contract

| Method | Path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|---|
| GET | `/admin/users?cursor=&limit=` | `users:read-metadata` | `limit` 1–100, mặc định 20 | `200 {items: [AdminUser], nextCursor}` | 400, 403 |
| GET | `/admin/users/{userId}/metadata` | `users:read-metadata` | — | `200 AdminUser` | 403, 404 |
| POST | `/admin/users/{userId}:suspend` | `users:suspend` | `{reasonCode}` | `200 AdminUser` | 400, 403, 409 |
| POST | `/admin/users/{userId}:restore` | `users:suspend` | `{reasonCode}` | `200 AdminUser` | 400, 403, 409 |
| GET | `/admin/ai-jobs?cursor=&limit=` | `jobs:read` | `limit` 1–100, mặc định 20 | `200 {items: [AdminJob], nextCursor}` | 400, 403 |
| POST | `/admin/ai-jobs/{jobId}:retry` | `jobs:retry` | — | `202` | 403, 409 |
| GET | `/admin/dashboard` | `admin:dashboard` | — | `200 AdminDashboard` | 403 |

- `AdminUser` = `{id, status, createdAt, lastLoginAt, version}`.
- `AdminJob` = `{id, userId, type, status, attempt, errorCode, createdAt, availableAt}`.
- `AdminDashboard` = `{suppressed, activeUsers, journalEntries, dailyCheckins, minimumCohortSize}`.
- Endpoint phản hồi (`/admin/feedback`) xem [13-feedback](../13-feedback/README.md); kiến thức (`/admin/knowledge-items`) xem [11-knowledge](../11-knowledge.md).

Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `roles`, `permissions`, `role_permissions`, `user_roles` | RBAC; `user_roles` có `expires_at` | V2, V12, V14 |
| `users` | `status`, `last_login_at`, `row_version` | V2 |
| `ai_jobs` | Job AI; chỉ đọc metadata đã lọc | V8 |
| `audit_logs` | `USER_SUSPENDED`, `USER_RESTORED` (kèm `reason_code`), `AI_JOB_RETRIED` | V2 |
| `safety_policy_versions`, `safety_resources` | Policy safety và nguồn hỗ trợ có trạng thái `DRAFT`/`APPROVED`/`RETIRED` (dùng cho UC-28) | V7 |

## 6. Safety, quyền riêng tư và consent

- Admin không đọc nhật ký; danh sách người dùng không có email; job không trả payload hay `last_error_summary` (test khẳng định).
- Dashboard ẩn số liệu khi nhóm nhỏ hơn 20 người để tránh nhận diện cá nhân.
- Danh sách job có `userId`, nên admin vẫn biết người dùng nào có bài bị lỗi phân tích.
- Audit log được giữ lại khi người dùng "bắt đầu lại". Khi xóa tài khoản, audit do admin thực hiện trên tài khoản đó được giữ; chỉ audit do chính người dùng tạo bị xóa (xem [UC-23](../12-export-deletion/UC-23-delete-account.md) mục 5).
- Chưa có MFA cho tài khoản quản trị (thuộc release gate M8).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đang làm | UC-24, UC-25 đã kiểm chứng (`M6IntegrationTest`); UC-28 chưa làm |
| Web | đang làm | `AdminConsoleView` có tab người dùng, job, phản hồi, kiến thức, chỉ số; chưa có phần safety; chưa có Playwright |
| Mobile | — | Không áp dụng |

## 8. Câu hỏi mở

1. Quyền `safety:manage` và `audit:read` đã tạo nhưng chưa dùng. Có cần màn hình xem audit trong MVP không?
2. Gán và thu hồi vai trò hiện phải sửa DB trực tiếp. Có cần API và audit cho việc này không?
3. MFA cho quản trị là điều kiện phát hành (M8). Chưa có thiết kế.
