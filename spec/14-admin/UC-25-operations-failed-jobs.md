# UC-25 — Theo dõi vận hành và xử lý tác vụ lỗi

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

- **Feature:** [Quản trị](README.md)
- **Catalog:** UC-25 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

`SYSTEM_ADMIN` (quyền `jobs:read`, `jobs:retry`, `admin:dashboard`).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đăng nhập với quyền tương ứng.
- **Hậu điều kiện (retry):** job trở lại `PENDING` với `attempt = 0`; bài nhật ký trở lại `PENDING` phân tích; ghi audit `AI_JOB_RETRIED`.

## 3. Luồng chính

### Xem job AI

1. Mở tab job → `GET /admin/ai-jobs` (mọi loại job AI, phân trang cursor theo `id` giảm dần).
2. Mỗi dòng có `id`, `userId`, `type`, `status`, `attempt`, `errorCode`, `createdAt`, `availableAt`. **Không** có payload hay mô tả lỗi thô.

### Retry job

3. Chọn một job lỗi → `POST /admin/ai-jobs/{jobId}:retry` → `202`.
4. Backend chỉ retry khi job thỏa **tất cả** điều kiện (`JpaAdminJobStore.java:27-42`):
   - loại `JOURNAL_ANALYSIS` trên `JOURNAL_ENTRY`, payload có `contentVersion`;
   - `status = DEAD`, `lastErrorCode = PROVIDER_UNAVAILABLE`;
   - kết thúc cách đây hơn 60 giây.
5. Bài nhật ký phải còn ở trạng thái cho phép retry với đúng `contentVersion`; khi đó bài chuyển lại `PENDING`.

### Dashboard

6. Mở tab chỉ số → `GET /admin/dashboard`: tổng số tài khoản `ACTIVE`, tổng bài nhật ký chưa xóa, tổng check-in (`JpaAdminDashboardStore.java`).
7. Nếu ít hơn 20 tài khoản active → `suppressed = true`, mọi số là `null`. Nếu ít hơn 20 người có viết nhật ký (hoặc check-in) thì số tương ứng là `null` và `suppressed = true` (`AdminDashboardService.java:13-19`).

## 4. Luồng phụ và ngoại lệ

- Job không thỏa điều kiện retry, hoặc bài đã đổi nội dung / không còn cho retry: `409`.
- Thiếu quyền: `403`. `limit` ngoài 1–100: `400`.

## 5. Business rule

- Chỉ retry được lỗi tạm thời từ provider; lỗi policy, schema hay safety không retry thủ công được.
- Dashboard là số tổng tính từ đầu, không theo khoảng thời gian.
- Ngưỡng ẩn số liệu là 20 người.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/admin/ai-jobs?cursor=&limit=` | — | `200 {items, nextCursor}` | 400, 401, 403 |
| POST | `/api/v1/admin/ai-jobs/{jobId}:retry` | — | `202` | 401, 403, 409 |
| GET | `/api/v1/admin/dashboard` | — | `200 {suppressed, activeUsers, journalEntries, dailyCheckins, minimumCohortSize}` | 401, 403 |

## 7. Giao diện web và mobile

- **Web:** `AdminJobTable.tsx` (tìm theo id, loại, mã lỗi; nút retry), `AdminDashboardMetrics.tsx` trong `AdminConsoleView`.
- **Mobile:** không áp dụng.

## 8. Tiêu chí chấp nhận

- [x] Danh sách job không lộ payload và lỗi thô (`M6IntegrationTest`).
- [x] Người dùng thường retry → `403`; có `jobs:retry` → `202`, job và bài về `PENDING`, ghi audit.
- [x] Dashboard ẩn số liệu khi nhóm nhỏ.
- [ ] Test retry job không đủ điều kiện → `409`.
- [ ] Playwright cho tab job và chỉ số.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M6IntegrationTest` |
| Web | xong | Chưa có Playwright |
| Mobile | — | Không áp dụng |

## 10. Câu hỏi mở

1. Catalog nói màn hình vận hành giúp biết hệ thống có xử lý được **xuất, xóa và phân tích** hay không, nhưng hiện chỉ xem được job AI. Yêu cầu xuất `FAILED`, yêu cầu xóa tài khoản `DELETION_STUCK` và file kẹt trong hàng đợi xóa ([12-export-deletion](../12-export-deletion/README.md) mục 5) không hiện ở đâu. Có cần thêm trong MVP không?
2. Danh sách job có `userId`. Có cần ẩn không?
3. Dashboard chỉ có số tổng. Có cần theo ngày/tuần, hoặc chỉ số sức khỏe hệ thống (độ dài hàng đợi, tỷ lệ lỗi) không?
