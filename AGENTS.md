# Hướng dẫn cho AI agent trong my-log-platform

File này áp dụng cho toàn bộ repository. Khi làm việc trong một thư mục có `AGENTS.md` riêng, đọc thêm file đó: hướng dẫn gần mã nguồn hơn quy định chi tiết cho phần ấy. Yêu cầu trực tiếp của người dùng trong phiên hiện tại được ưu tiên nếu khác với hướng dẫn trong các file này.

## Các phần của dự án

| Thư mục | Vai trò | Hướng dẫn cần đọc |
|---|---|---|
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

## Tài liệu và mức độ tin cậy

- Đọc README và `AGENTS.md` của thư mục sở hữu trước khi triển khai. Với luồng xuyên suốt, đối chiếu cả backend và client thực tế.
- Code, migration và test hiện tại quyết định trạng thái triển khai; ADR ghi lại quyết định kiến trúc; kế hoạch nêu mục tiêu và việc còn mở.
- Phân biệt rõ bản demo/mock ở frontend, adapter chưa bật và tính năng hoạt động với backend. Không coi model đã train hoặc provider đã cấu hình là được duyệt để xử lý dữ liệu người dùng.
