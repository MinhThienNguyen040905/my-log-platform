# 12 — Xuất dữ liệu và xóa tài khoản

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

Quyền của người dùng đối với dữ liệu: xuất và xóa.

- **Module backend:** `com.mylog.export`
- **API gốc:** `/api/v1/exports`, `/api/v1/account-deletion-requests`
- **Web:** [`features/export`](../../front-end/my-app-web/features/export)
- **Mobile:** —
- **Migration:** `V14` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`adr/0007-encrypted-database-export-artifacts.md`](../../backend/docs/adr/0007-encrypted-database-export-artifacts.md)

## 1. Mục tiêu và phạm vi

<!-- cần điền: mục tiêu; trong phạm vi; ngoài phạm vi -->

## 2. Use case

- [UC-22 — Xuất dữ liệu cá nhân](UC-22-export-data.md)
- [UC-23 — Xóa tài khoản](UC-23-delete-account.md)

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
