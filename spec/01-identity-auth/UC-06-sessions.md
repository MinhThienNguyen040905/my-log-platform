# UC-06 — Xem và thu hồi phiên đăng nhập

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Định danh và xác thực](README.md)
- **Catalog:** UC-06 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có access token hợp lệ.
- **Hậu điều kiện:** phiên bị chọn (hoặc mọi phiên trừ phiên hiện tại) bị thu hồi; refresh token của chúng không còn dùng được; ghi audit `SESSION_REVOKED` hoặc `OTHER_SESSIONS_REVOKED`.

## 3. Luồng chính

1. Người dùng mở phần bảo mật trong cài đặt → `GET /me/sessions`.
2. Backend trả danh sách phiên còn hiệu lực của chính người dùng; phiên đang dùng có `current: true` (so với claim `sid`).
3. Người dùng thu hồi một phiên → `DELETE /me/sessions/{sessionId}`, hoặc thu hồi tất cả phiên khác → `DELETE /me/sessions?exceptCurrent=true`.
4. Backend thu hồi và trả `204` (`IdentityService.java:232-244`).

## 4. Luồng phụ và ngoại lệ

- `sessionId` không thuộc người dùng hoặc đã hết hiệu lực: `404` (không tiết lộ phiên của người khác).
- Thu hồi chính phiên hiện tại: được phép; access token hiện tại vẫn dùng được tới khi hết hạn (tối đa 10 phút), nhưng không refresh được.
- JWT không có `sid` khi gọi `exceptCurrent=true`: `400`.

## 5. Business rule

- Chỉ liệt kê phiên chưa thu hồi và chưa hết hạn.
- Mọi truy vấn lọc theo `userId` hiện tại.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/me/sessions` | bearer | `200 [{id, deviceName, createdAt, lastUsedAt, expiresAt, current}]` | 401 |
| DELETE | `/api/v1/me/sessions/{sessionId}` | bearer | `204` | 401, 404 |
| DELETE | `/api/v1/me/sessions?exceptCurrent=true` | bearer | `204` | 400, 401 |

## 7. Giao diện web và mobile

- **Web:** `features/user/components/AccountAccessPanel.tsx` trong trang cài đặt.
- **Mobile:** `(app)/settings.tsx`.

## 8. Tiêu chí chấp nhận

- [x] Liệt kê và thu hồi phiên qua HTTP, chỉ trong phạm vi của chủ sở hữu (`IdentityFlowIntegrationTest`).
- [ ] Test riêng cho `exceptCurrent=true` giữ lại phiên hiện tại.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `IdentityFlowIntegrationTest` |
| Web | xong | Chưa có Playwright riêng |
| Mobile | xong | Kiểm chứng bằng script smoke API; chưa chạy trên thiết bị |

## 10. Câu hỏi mở

- Khi thu hồi phiên hiện tại, client có cần tự đăng xuất ngay không?
