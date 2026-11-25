# UC-18 — Viết lời nhắn cho chính mình trong tương lai

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** **đề xuất**, chưa có code, API hay migration. Nội dung lấy từ catalog và tài liệu đề xuất ngày 2026-11-20, **chờ xác nhận** trước khi triển khai.

- **Feature:** [Tự chăm sóc](README.md)
- **Catalog:** UC-18 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)
- **Đề xuất gốc:** mục 1 "Thư gửi tương lai" trong [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](../../backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)

## 1. Tác nhân

Người dùng đã đăng nhập, đang cảm thấy ổn.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập.
- **Hậu điều kiện:** một lời nhắn do chính người dùng viết được lưu để tự mở lại sau.

## 3. Luồng chính

1. Khi cảm thấy ổn, người dùng viết một lời động viên cho chính mình và chọn lưu.
2. Về sau, người dùng tự mở lại lời nhắn bất cứ lúc nào.
3. Tùy chọn: người dùng cho phép ứng dụng **mời** xem lại khi phù hợp.
4. Người dùng sửa, xóa lời nhắn và tắt lời mời xem lại bất cứ lúc nào.

## 4. Luồng phụ và ngoại lệ

- Không tự gửi hay bật lời nhắn chỉ vì một chỉ số cảm xúc thấp.
- Không dùng thay cho luồng hỗ trợ an toàn khi có dấu hiệu khủng hoảng ([08-safety](../08-safety/README.md)).
- Người dùng không muốn nhận lời mời: tắt hẳn, không hỏi lại.

## 5. Business rule

- Lời nhắn là dữ liệu nhạy cảm: mã hóa, lọc theo chủ sở hữu, có trong export và bị xóa theo chính sách xóa.
- Lời mời xem lại là tùy chọn, có quyền chọn rõ ràng.

## 6. API liên quan

Chưa có. Cần thiết kế trước khi triển khai.

## 7. Giao diện web và mobile

- **Web:** chưa có.
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [ ] Chốt acceptance criteria, consent, retention, API/error contract, migration.
- [ ] Tạo/sửa/xóa, chỉ chủ sở hữu; có trong export và bị xóa theo chính sách.
- [ ] Kiểm thử riêng tình huống người dùng không muốn nhận lời mời.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | chưa làm | |
| Web | chưa làm | |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

- Lời mời xem lại xuất hiện ở đâu và khi nào (sau check-in thấp, trong companion, hay chỉ khi người dùng tự mở)?
- Roadmap xếp UC này vào P2 (sau MVP). Giữ nguyên?
