# Spec của mylog

`spec/` là **nguồn dữ liệu uy tín duy nhất** về feature của mylog: hành vi, business rule, phân quyền, API contract và dữ liệu. Bắt đầu từ [mucluc.md](mucluc.md).

## Nguyên tắc

- Code, migration và test là hiện thực của spec và cho biết đã làm đến đâu. Khi code khác spec thì đó là lỗi cần xử lý, không mặc định code đúng.
- `backend/docs/`, README và các file kế hoạch chỉ là tài liệu bổ trợ. Nếu khác spec thì theo spec.
- Khi feature thay đổi (thêm, sửa, bỏ hành vi hoặc contract), cập nhật spec **trong cùng commit/PR**. Không merge thay đổi feature khi spec chưa cập nhật.
- Spec rỗng (giai đoạn chuyển tiếp): điền từ code và test hiện tại, nhờ người phụ trách xác nhận trước khi dựa vào.
- Chỉ dùng ví dụ synthetic. Không ghi dữ liệu người dùng thật, nội dung nhật ký, token, mật khẩu hoặc URL có chữ ký.

## Quy trình khi đổi feature

1. Đọc [mucluc.md](mucluc.md) và spec của feature/UC.
2. Sửa spec theo yêu cầu đã thống nhất.
3. Sửa code và test cho khớp spec.
4. Cập nhật mục **Trạng thái triển khai** (Backend / Web / Mobile).

## Cấu trúc

- Feature có từ 2 UC trở lên: thư mục `NN-feature/` gồm `README.md` (tổng quan) và mỗi UC một file `UC-xx-slug.md`.
- Feature có 0 hoặc 1 UC: một file `NN-feature.md`; UC (nếu có) nằm trong mục "Use case" của file.
- Khi feature đơn có thêm UC thứ hai, tách thành thư mục.
- Mã UC theo [`backend/docs/USE_CASE_CATALOG.md`](../backend/docs/USE_CASE_CATALOG.md). UC mới lấy mã tiếp theo và thêm vào [mucluc.md](mucluc.md).

## Thêm feature hoặc UC

- Feature mới: chép [`_templates/feature.md`](_templates/feature.md).
- UC mới: chép [`_templates/use-case.md`](_templates/use-case.md).
- Thêm dòng vào [mucluc.md](mucluc.md).

Trạng thái triển khai dùng một trong: **chưa làm**, **đang làm**, **xong**, **đã kiểm chứng** (đã có test hoặc kiểm tra thực tế).

## Trạng thái spec và cách xác nhận

Mỗi file spec có một dòng `> **Trạng thái spec:** ...` ngay dưới tiêu đề. Dòng này cho biết **nội dung spec** đã được chốt hay chưa, khác với mục "Trạng thái triển khai" (cho biết **code** đã làm đến đâu).

| Trạng thái spec | Nghĩa | Được dựa vào để code không? |
|---|---|---|
| **đề xuất** | Tính năng chưa có code; nội dung lấy từ catalog hoặc tài liệu đề xuất | Không. Cần người phụ trách quyết định làm hay không |
| **chờ xác nhận** | Đã điền đủ, chủ yếu mô tả lại những gì code đang làm; chưa ai duyệt | Chỉ để tham khảo. Code đang làm vậy, không có nghĩa là nên làm vậy |
| **đã xác nhận** | Người phụ trách sản phẩm đã đọc, trả lời hoặc ghi nhận các câu hỏi mở | Có. Đây mới là nguồn sự thật |
| **hoãn** | Người phụ trách quyết định chưa làm hoặc tạm tắt; file ghi quyết định, code còn tồn tại và hành vi cần có khi bật lại | Có, theo nghĩa: không bật tính năng này. Phải xác nhận lại trước khi bật |
| **đã bỏ** | Người phụ trách quyết định không làm, hoặc gỡ hẳn tính năng; file giữ lại để ghi quyết định | Có, theo nghĩa: không được triển khai tính năng này |

Một spec chuyển sang **đã xác nhận** khi đủ cả ba điều kiện:

1. Người phụ trách sản phẩm đã đọc spec và đồng ý với nội dung (hoặc yêu cầu sửa, và spec đã được sửa).
2. Mỗi câu hỏi mở đã được trả lời và đưa vào nội dung, hoặc được ghi rõ là "để sau" kèm lý do.
3. Thay đổi đó được merge vào `main` qua PR.

Khi đó dòng trạng thái đổi thành `> **Trạng thái spec:** đã xác nhận ngày YYYY-MM-DD.` Nếu một câu trả lời khác với code hiện tại, ghi điểm lệch vào mục "Trạng thái triển khai" để sửa code sau; không sửa spec cho khớp code.
