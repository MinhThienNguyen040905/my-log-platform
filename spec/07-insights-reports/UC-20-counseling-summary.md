# UC-20 — Tạo bản tóm tắt mang đến buổi tham vấn

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** **đề xuất**, chưa có code, API hay migration. Nội dung lấy từ catalog và tài liệu đề xuất ngày 2026-11-20, **chờ xác nhận** trước khi triển khai.

- **Feature:** [Insight và báo cáo](README.md)
- **Catalog:** UC-20 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)
- **Đề xuất gốc:** mục 3 "Bản chuẩn bị buổi tham vấn" trong [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](../../backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)

## 1. Tác nhân

Người dùng muốn chuẩn bị thông tin để trao đổi với chuyên gia hoặc người hỗ trợ.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập.
- **Hậu điều kiện:** người dùng có một bản tóm tắt khoảng một trang do họ chọn lọc, để tải xuống hoặc tự chia sẻ. Không ai khác được quyền truy cập tài khoản qua tính năng này.

## 3. Luồng chính

1. Người dùng chọn khoảng thời gian và các mục muốn đưa vào.
2. Hệ thống tạo bản xem trước, có thể gồm:
   - số ngày có check-in;
   - xu hướng tâm trạng, giấc ngủ, năng lượng **do người dùng ghi nhận**;
   - chủ đề lặp lại (nếu phân tích đủ điều kiện);
   - câu hỏi hoặc điều muốn trao đổi do người dùng tự viết.
3. Người dùng bỏ bất kỳ mục nào trước khi xuất.
4. Người dùng xác nhận bản xem trước → tải xuống.

## 4. Luồng phụ và ngoại lệ

- Thiếu dữ liệu cho một mục: để trống hoặc ghi "chưa đủ dữ liệu".
- Không bao giờ gửi tự động cho chuyên gia, email hay bên thứ ba.

## 5. Business rule

- **Mặc định không đưa vào:** nguyên văn nhật ký, phản chiếu AI, sự kiện safety, suy đoán nguyên nhân, nhãn chẩn đoán.
- Ghi rõ nguồn dữ liệu, cỡ mẫu và ngày tạo.
- Không trình bày như hồ sơ hoặc kết luận lâm sàng.
- Khác với export toàn bộ dữ liệu ([12-export-deletion](../12-export-deletion/README.md)): đây là bản chọn lọc do người dùng kiểm soát. Nếu xuất PDF, áp dụng artifact mã hóa, TTL, tải có xác thực và xóa theo chính sách export hiện hành, hoặc một quyết định mới được ghi rõ.

## 6. API liên quan

Chưa có. Cần thiết kế trước khi triển khai.

## 7. Giao diện web và mobile

- **Web:** chưa có.
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [ ] Chốt các trường dữ liệu, bản xem trước và quyền loại từng mục.
- [ ] Kiểm thử thiếu dữ liệu, quyền sở hữu, xuất và xóa artifact.
- [ ] Không có nguyên văn nhật ký hay nội dung safety trong bản mặc định.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | chưa làm | |
| Web | chưa làm | |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

- Định dạng xuất: PDF, ảnh, hay chỉ bản in từ trình duyệt?
- Có tái sử dụng snapshot báo cáo tuần/tháng hiện có làm nguồn số liệu không?
