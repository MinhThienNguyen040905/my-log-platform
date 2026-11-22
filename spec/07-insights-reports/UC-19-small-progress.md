# UC-19 — Xem lại những tiến bộ nhỏ

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** **đề xuất**, chưa có code, API hay migration. Nội dung lấy từ catalog và tài liệu đề xuất ngày 2026-11-20, **chờ xác nhận** trước khi triển khai.

- **Feature:** [Insight và báo cáo](README.md)
- **Catalog:** UC-19 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)
- **Đề xuất gốc:** mục 2 "Điểm sáng nhỏ (Micro-Wins Vault)" trong [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](../../backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập. Nếu dùng gợi ý tự động: consent còn hiệu lực, bài được safety cho phép, `contentVersion` còn khớp.
- **Hậu điều kiện:** một "điểm sáng" được lưu trong bộ sưu tập riêng của người dùng, chỉ khi người dùng xác nhận.

## 3. Luồng chính

**Giai đoạn 1 — nhập thủ công (làm trước)**

1. Người dùng tự ghi một điểm sáng (việc nhỏ có ý nghĩa với họ).
2. Hệ thống lưu vào bộ sưu tập.
3. Người dùng xem, sửa, xóa bất kỳ mục nào.

**Giai đoạn 2 — gợi ý từ nhật ký (chỉ sau khi có đánh giá)**

1. Sau khi bài được lưu và đủ điều kiện phân tích, hệ thống **có thể** đề xuất một đoạn ngắn lấy từ chính bài đó.
2. Người dùng xem câu gốc và ngữ cảnh, sửa, chấp nhận hoặc bỏ qua.
3. Chỉ khi chấp nhận mới lưu.

## 4. Luồng phụ và ngoại lệ

- Không tự thêm vào bộ sưu tập; không tạo thành tích không có trong bài; không gắn nhãn "chiến thắng" cho câu mơ hồ.
- Không nhắc lại điểm sáng ngoài ngữ cảnh người dùng chọn.

## 5. Business rule

- Nội dung điểm sáng là dữ liệu nhạy cảm: mã hóa, lọc theo chủ sở hữu, có trong export và bị xóa khi xóa tài khoản / bắt đầu lại.
- `sentiment`, `emotions`, `topics` hiện có **không đủ** để suy ra điểm sáng; trích xuất là tác vụ AI riêng, cần đánh giá chất lượng và safety.
- Không dùng số lượng điểm sáng làm bằng chứng sức khỏe tinh thần cải thiện.

## 6. API liên quan

Chưa có. Cần thiết kế trước khi triển khai.

## 7. Giao diện web và mobile

- **Web:** chưa có.
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [ ] Chốt acceptance criteria, consent, retention, API/error contract, migration.
- [ ] Tạo/sửa/xóa thủ công, chỉ chủ sở hữu.
- [ ] Có trong export và bị xóa theo chính sách xóa.
- [ ] (Giai đoạn 2) Tập đánh giá synthetic cho trích xuất; bước xác nhận bắt buộc.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | chưa làm | |
| Web | chưa làm | |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

- UC này nên thuộc feature Insight (nhìn lại) hay Tự chăm sóc ([09-selfcare](../09-selfcare/README.md))?
- Ưu tiên P1 hay P2? Roadmap đang xếp vào nhóm "nên có sau khi kiểm chứng với người dùng".
