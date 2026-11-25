# UC-21 — Gửi phản hồi về nội dung hoặc gợi ý

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-25, **chờ xác nhận**.

- **Feature:** [Phản hồi](README.md)
- **Catalog:** UC-21 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có access token hợp lệ.
- **Hậu điều kiện:** phản hồi được lưu mã hóa với trạng thái `OPEN`, hạn xóa 180 ngày; người dùng thấy nó trong danh sách phản hồi của mình.

## 3. Luồng chính

1. Người dùng mở tab "Phản hồi" trong cài đặt, chọn loại: "Đề xuất cải tiến" (`IDEA`), "Báo lỗi" (`BUG`), "Góp ý khác" (`OTHER`).
2. Người dùng viết nội dung (tối đa 4.000 ký tự) và gửi → `POST /feedback` → `202 {id}`.
3. Backend cắt khoảng trắng hai đầu, mã hóa và lưu (`FeedbackService.java:29-36`).
4. Danh sách "phản hồi của tôi" (`GET /feedback`) hiện các phản hồi mới nhất trước, kèm trạng thái xử lý.

## 4. Luồng phụ và ngoại lệ

- `category` không thuộc `BUG`/`IDEA`/`OTHER`, nội dung rỗng hoặc dài hơn 4.000 ký tự: `400`.
- Xem phản hồi không phải của mình: `404`.

## 5. Business rule

- Nội dung được mã hóa; chỉ chủ sở hữu và nhân viên có `feedback:read` đọc được.
- Không gắn với bài nhật ký hay mục nội dung nào; không tự đưa nguyên văn nhật ký vào.
- Bị xóa tự động sau 180 ngày, khi "bắt đầu lại" và khi xóa tài khoản.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/feedback` | `{category, message}` | `202 {id}` | 400, 401 |
| GET | `/api/v1/feedback?limit=` | `limit` mặc định 20, tối đa 50 | `200 [FeedbackResponse]` | 401 |
| GET | `/api/v1/feedback/{id}` | — | `200 FeedbackResponse` | 401, 404 |

## 7. Giao diện web và mobile

- **Web:** `features/feedback/components/UserFeedbackCard.tsx` trong tab "Phản hồi" của `SettingsView`; đếm ký tự `x/4000`; danh sách phản hồi đã gửi kèm nhãn trạng thái.
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [x] Nội dung được mã hóa trong DB (`M7IntegrationTest`).
- [x] Người khác xem → `404`; danh sách chỉ có phản hồi của mình, mới nhất trước.
- [x] Phản hồi hết hạn bị xóa bởi job dọn dẹp.
- [ ] Test `400` cho loại sai và nội dung quá dài.
- [ ] Playwright cho luồng gửi phản hồi.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M7IntegrationTest` |
| Web | xong | Chưa có Playwright |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

1. Tên UC và catalog nói "phản hồi về nội dung hoặc gợi ý", nhưng code chỉ có phản hồi chung (xem [README](README.md) câu hỏi 1).
2. Không có quota gửi; không có safety screening cho nội dung (xem [README](README.md) câu hỏi 2, 3).
