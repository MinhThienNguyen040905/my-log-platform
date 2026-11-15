# mylog

**mylog** là ứng dụng nhật ký và tự suy ngẫm. Dự án hướng tới việc giúp người dùng ghi chép, theo dõi cảm xúc và nhìn lại trải nghiệm của mình. Ứng dụng không chẩn đoán, điều trị hoặc thay thế chuyên gia sức khỏe tâm thần.

Repository gồm backend Spring Boot, giao diện web Next.js, ứng dụng mobile Expo và một pipeline thử nghiệm cho safety classifier. Các phần này đang ở những mức độ hoàn thiện khác nhau.

Tiến độ được đối chiếu với code ngày **2026-11-15** tại [PROJECT_STATUS.md](PROJECT_STATUS.md), gồm kết quả kiểm tra và các giới hạn chưa xác minh. Dự án chưa đạt release gate production.

## Cấu trúc repository

| Thư mục | Nội dung | Trạng thái hiện tại |
| --- | --- | --- |
| [`backend/`](backend/) | API, nghiệp vụ, PostgreSQL, background jobs và tích hợp AI/safety | Đã có các API và integration test; một số provider, chính sách safety và bước phát hành vẫn chưa hoàn tất. |
| [`front-end/my-app-web/`](front-end/my-app-web/) | Giao diện Next.js | Đã kết nối đăng ký, xác minh email, đăng nhập, nhật ký, hồ sơ và một số báo cáo với backend; vẫn cần kiểm chứng đầy đủ trên môi trường triển khai. |
| [`front-end/my-app-mobile/`](front-end/my-app-mobile/) | Ứng dụng Expo | Đã gọi backend cho auth/onboarding, hồ sơ, nhật ký văn bản, dashboard/report, self-care, export, reset mật khẩu, analysis và safety; còn thiếu tính năng và kiểm chứng native. |
| [`safety-model/`](safety-model/) | Pipeline train và dịch vụ inference classifier | Baseline có thể train, chưa phải model được phê duyệt để xử lý dữ liệu người dùng. |
| [`recommend-model/`](recommend-model/) | Công cụ biên tập card, demo câu hỏi và nhập corpus retrieval | Có công cụ và dữ liệu nguồn; trạng thái card, kiểm thử và cổng nhập backend được giải thích trong README riêng. |

## Trạng thái kết nối API

Backend có API cho xác thực, hồ sơ, nhật ký, check-in và các tính năng khác. Đăng ký tạo lượt tạm; xác minh email bằng liên kết hoặc mã 8 chữ số mới tạo tài khoản thật. Có thể đăng ký lại cùng email khi chưa xác minh. Web đã gọi backend qua các route API của Next.js cho xác thực, nhật ký và những luồng chính khác. `localStorage` dùng cho dữ liệu giao diện như bản nháp, không phải nơi lưu nhật ký chính. Web gọi `GET /api/v1/safety/resources` để lấy nguồn hỗ trợ đã được duyệt; backend có thể trả danh sách rỗng nếu chưa có nguồn được xác minh. Mobile gọi trực tiếp `/api/v1` qua API client riêng; xem [trạng thái mobile](front-end/my-app-mobile/PROJECT_STATUS.md).

Web đã có check-in hằng ngày, Companion qua BFF, các màn hình insight/report, quản trị và CRUD mục tiêu/thói quen. Companion có kịch bản dựng sẵn và adapter LLM với validator/fallback; việc bật AI cho dữ liệu thật còn phụ thuộc consent, classifier/policy và phê duyệt provider.

Không coi việc giao diện đã gọi API là bằng chứng luồng safety/AI đã hoạt động từ đầu đến cuối. Xem [hướng dẫn backend](backend/README.md) và [trạng thái web](front-end/my-app-web/README.md) trước khi nối thêm API.

## Chạy local

### Backend

Cần Java 21 và Docker có Compose. Repository đã có Maven Wrapper nên không cần cài Maven riêng. Ví dụ dưới đây dùng PostgreSQL/pgvector, Redis và Mailpit local; hãy kiểm tra cấu hình trong `.env` trước khi chạy.

```powershell
cd backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d
./mvnw.cmd spring-boot:run
```

Backend mặc định ở `http://localhost:8080`; có thể kiểm tra `http://localhost:8080/actuator/health`. Profile `local` bật API docs tại `http://localhost:8080/internal/swagger-ui`. Nếu dùng PostgreSQL do Supabase quản lý hoặc cấu hình dịch vụ khác, xem [backend/README.md](backend/README.md) để chọn service Compose và biến môi trường phù hợp. Không commit `.env` hoặc thông tin xác thực.

### Web

Cần Node.js và npm tương thích với Next.js 16. Từ thư mục gốc repository:

```powershell
cd front-end/my-app-web
npm install
npm run dev
```

Mở `http://localhost:3000`. Backend cần chạy để các luồng xác thực và dữ liệu hoạt động. Các route API của Next.js dùng `MYLOG_BACKEND_URL`, mặc định là `http://localhost:8080`. Yêu cầu lấy nguồn hỗ trợ an toàn dùng `NEXT_PUBLIC_BACKEND_URL`, cũng mặc định là `http://localhost:8080`; backend phải cho phép origin của web để yêu cầu đó thành công.

### Mobile

```powershell
cd front-end/my-app-mobile
npm ci
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
npm run start
```

Đặt `EXPO_PUBLIC_API_BASE_URL` trong `.env` thành URL backend mà thiết bị/emulator truy cập được, không kèm `/api/v1`. Expo hiển thị các cách mở ứng dụng trên thiết bị, emulator hoặc web. Các luồng MyLog đã có trong source; kết quả bundle hay Expo web không thay thế kiểm chứng Android/iOS native. Xem [README của mobile](front-end/my-app-mobile/README.md).

## Tài liệu

- [Tiến độ và bằng chứng kiểm tra ngày 2026-11-15](PROJECT_STATUS.md)
- [Backend: cấu hình, chạy local và API](backend/README.md)
- [Kiến trúc backend](backend/docs/BACKEND_ARCHITECTURE.md)
- [Tổng quan database](backend/docs/DATABASE_OVERVIEW.md)
- [Kế hoạch phát triển backend](backend/docs/BACKEND_DEVELOPMENT_PLAN.md)
- [AI analysis và safety](backend/docs/AI_ANALYSIS_GUIDE.md)
- [Safety classifier](safety-model/README.md)
- [Hướng dẫn làm việc trong repository](AGENTS.md)

Code, migration và test hiện tại quyết định trạng thái triển khai; các tài liệu kế hoạch mô tả cả phần còn mở.
