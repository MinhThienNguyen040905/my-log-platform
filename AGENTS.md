# Hướng dẫn cho AI agent trong my-log-platform

File này áp dụng cho toàn bộ repository. Khi làm việc trong một thư mục có `AGENTS.md` riêng, đọc thêm file đó: hướng dẫn gần mã nguồn hơn quy định chi tiết cho phần ấy. Yêu cầu trực tiếp của người dùng trong phiên hiện tại được ưu tiên nếu khác với hướng dẫn trong các file này.

## Các phần của dự án

| Thư mục | Vai trò | Hướng dẫn cần đọc |
|---|---|---|
| `spec/` | **Nguồn sự thật duy nhất** về feature: hành vi, business rule, API contract, dữ liệu, use case | [`spec/mucluc.md`](spec/mucluc.md), [`spec/README.md`](spec/README.md) |
| `backend/` | API, nghiệp vụ, PostgreSQL, job và tích hợp AI; Java/Spring Boot | [`backend/AGENTS.md`](backend/AGENTS.md), [`backend/README.md`](backend/README.md) |
| `front-end/my-app-web/` | Ứng dụng web Next.js | [`front-end/my-app-web/AGENTS.md`](front-end/my-app-web/AGENTS.md), README của web |
| `front-end/my-app-mobile/` | Ứng dụng mobile Expo | [`front-end/my-app-mobile/AGENTS.md`](front-end/my-app-mobile/AGENTS.md), README của mobile |
| `safety-model/` | Pipeline train và dịch vụ inference cho classifier safety | [`safety-model/README.md`](safety-model/README.md), [`backend/docs/SAFETY_CLASSIFIER_MODEL_PLAN.md`](backend/docs/SAFETY_CLASSIFIER_MODEL_PLAN.md) |

Không áp quy ước package Java của backend cho frontend hoặc Python; không áp quy ước Next.js cho mobile. Trước khi sửa một phần, kiểm tra cấu trúc, code, test và tài liệu thực tế của phần đó. Tài liệu kế hoạch và sơ đồ có thể chứa phần chưa triển khai; không trình bày chúng như tính năng đã hoạt động.

## Nguyên tắc chung

- **mylog** là ứng dụng nhật ký và tự suy ngẫm, không chẩn đoán hoặc điều trị. Nội dung nhật ký, email, token và dữ liệu sức khỏe tinh thần là nhạy cảm.
- Giữ ranh giới giữa các phần: frontend gọi API theo contract; backend sở hữu business rule, authorization, safety và quyền truy cập dữ liệu; `safety-model` chỉ cung cấp kết quả classifier theo contract được duyệt. Khi đổi contract, cập nhật bên sử dụng và tài liệu liên quan.
- Không log, commit hoặc đưa vào fixture dữ liệu người dùng thật, nội dung nhật ký, `.env`, khóa, token, mật khẩu, raw prompt/response hoặc URL có chữ ký. Dùng dữ liệu synthetic trong test và ví dụ.
- Kiểm tra `git status` trước khi sửa và giữ nguyên thay đổi không thuộc yêu cầu của người dùng. Không sửa migration đã áp dụng; các quy tắc schema chi tiết nằm trong `backend/AGENTS.md`.
- Chạy kiểm tra phù hợp với phạm vi thay đổi; báo rõ kiểm tra nào đã chạy và phần nào chưa kiểm chứng. Cập nhật tài liệu khi hành vi hoặc contract thay đổi.

## Đặc tả feature (nguồn sự thật duy nhất)

- `spec/` là nguồn uy tín duy nhất về hành vi, business rule, phân quyền, API contract và dữ liệu của mọi feature. `backend/docs/`, README và kế hoạch chỉ là tài liệu bổ trợ; nếu khác spec thì theo spec.
- **Trước khi thực thi** một feature hoặc UC: đọc [`spec/mucluc.md`](spec/mucluc.md), rồi đọc file hoặc thư mục spec tương ứng.
- **Khi feature thay đổi** (thêm, sửa, bỏ hành vi hoặc contract): cập nhật spec trong cùng commit/PR, gồm mục "Trạng thái triển khai". Không merge thay đổi feature khi spec chưa cập nhật.
- **Yêu cầu khác spec:** chỉ ra điểm khác, cập nhật spec theo quyết định của người dùng trước, rồi mới sửa code.
- **Code lệch spec:** báo cho người dùng; không tự sửa bên nào khi chưa rõ bên nào đúng.
- **Spec còn rỗng** (giai đoạn chuyển tiếp): điền spec từ code và test hiện tại, nhờ người dùng xác nhận trước khi dựa vào nó.

## Tài liệu và mức độ tin cậy

- Đọc README và `AGENTS.md` của thư mục sở hữu trước khi triển khai. Với luồng xuyên suốt, đối chiếu cả backend và client thực tế.
- `spec/` là nguồn sự thật về feature; code, migration và test là hiện thực của spec và cho biết đã làm đến đâu; ADR ghi lại quyết định kiến trúc; kế hoạch nêu việc còn mở.
- Phân biệt rõ bản demo/mock ở frontend, adapter chưa bật và tính năng hoạt động với backend. Không coi model đã train hoặc provider đã cấu hình là được duyệt để xử lý dữ liệu người dùng.

## Tài khoản test local

Tài khoản synthetic được tạo bằng `backend/scripts/seed-demo-journal.mjs` (xem mục "Seed dữ liệu demo" trong `backend/README.md`). Tài khoản này chỉ tồn tại trong database Docker local của máy đã chạy script; máy khác cần tự chạy script để tạo tài khoản riêng. Không dùng thông tin này cho staging/prod.

Email và mật khẩu nằm trong `backend/.env` (đã gitignore) ở hai biến `MYLOG_TEST_ACCOUNT_EMAIL` và `MYLOG_TEST_ACCOUNT_PASSWORD`; script cũng lưu bản sao ở `backend/scratch/seed-demo-account.txt`. Không ghi mật khẩu vào file được commit.

| Tài khoản | Dữ liệu |
|---|---|
| `MYLOG_TEST_ACCOUNT_EMAIL` trong `backend/.env` | ~120 ngày nhật ký (~200 chữ/bài), tag, check-in synthetic (seed 42); 30 ngày gần nhất có cảm xúc/chủ đề mẫu (`LOCAL_FAKE`) và đã bật consent `AI_PROCESSING`; bài cũ hơn ở `PENDING` |
