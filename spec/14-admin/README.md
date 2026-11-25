# 14 — Quản trị

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

Quản lý tài khoản, vận hành, tác vụ lỗi, nguồn hỗ trợ và quy tắc safety.

- **Module backend:** `com.mylog.admin`, `com.mylog.audit`, `com.mylog.platform`
- **API gốc:** `/api/v1/admin`
- **Web:** [`features/admin`](../../front-end/my-app-web/features/admin)
- **Mobile:** —
- **Migration:** `V6`, `V12` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`M8_RELEASE_RUNBOOK.md`](../../backend/docs/M8_RELEASE_RUNBOOK.md), [`M8_THREAT_MODEL.md`](../../backend/docs/M8_THREAT_MODEL.md)

## 1. Mục tiêu và phạm vi

<!-- cần điền: mục tiêu; trong phạm vi; ngoài phạm vi -->

## 2. Use case

- [UC-24 — Quản lý trạng thái tài khoản người dùng](UC-24-user-account-status.md)
- [UC-25 — Theo dõi vận hành và xử lý tác vụ lỗi](UC-25-operations-failed-jobs.md)
- [UC-28 — Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn](UC-28-safety-resources-rules.md)

## 3. Business rule và phân quyền

<!-- cần điền: quy tắc nghiệp vụ, ai được làm gì -->

## 4. API contract

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| | | | | |

## 5. Dữ liệu

<!-- cần điền: bảng, cột chính, migration liên quan -->

## 6. Safety, quyền riêng tư và consent

<!-- cần điền: dữ liệu nhạy cảm, consent cần có, hành vi khi có dấu hiệu nguy cơ -->

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | chưa điền | <!-- cần điền: chưa làm / đang làm / xong / đã kiểm chứng --> |
| Web | chưa điền | |
| Mobile | chưa điền | |

## 8. Câu hỏi mở

<!-- cần điền: điểm chưa thống nhất -->
