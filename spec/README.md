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
