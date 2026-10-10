# mylog

**mylog** là ứng dụng nhật ký và tự suy ngẫm. Dự án hướng tới việc giúp người dùng ghi chép, theo dõi cảm xúc và nhìn lại trải nghiệm của mình. Ứng dụng không chẩn đoán, điều trị hoặc thay thế chuyên gia sức khỏe tâm thần.

Repository gồm backend Spring Boot, giao diện web Next.js, ứng dụng mobile Expo và một pipeline thử nghiệm cho safety classifier. Các phần này đang ở những mức độ hoàn thiện khác nhau.

## Cấu trúc repository

| Thư mục | Nội dung | Trạng thái hiện tại |
| --- | --- | --- |
| [`backend/`](backend/) | API, nghiệp vụ, PostgreSQL, background jobs và tích hợp AI/safety | Đã có các API và integration test; một số provider, chính sách safety và bước phát hành vẫn chưa hoàn tất. |
| [`front-end/my-app-web/`](front-end/my-app-web/) | Giao diện Next.js | Prototype; đăng nhập, nhật ký và nhiều màn hình dùng dữ liệu demo trong trình duyệt. |
| [`front-end/my-app-mobile/`](front-end/my-app-mobile/) | Ứng dụng Expo | Hiện chủ yếu là ứng dụng khởi tạo của Expo. |
| [`safety-model/`](safety-model/) | Pipeline train và dịch vụ inference classifier | Baseline có thể train, chưa phải model được phê duyệt để xử lý dữ liệu người dùng. |

## Trạng thái kết nối API

Backend có API cho xác thực, hồ sơ, nhật ký, check-in và các tính năng khác. **Web chưa được nối với các luồng API chính**: đăng nhập và lưu nhật ký vẫn dùng trạng thái demo/`localStorage`. Hiện web gọi `GET /api/v1/safety/resources` để lấy các nguồn hỗ trợ đã được duyệt khi mở hộp hỗ trợ an toàn; backend có thể trả danh sách rỗng nếu chưa có nguồn được xác minh. Mobile chưa gọi backend API.

Đừng dùng giao diện demo và `localStorage` để lưu nhật ký thật hoặc đánh giá rằng luồng safety/AI đã hoạt động từ đầu đến cuối. Xem [hướng dẫn backend](backend/README.md) và [trạng thái web](front-end/my-app-web/README.md) trước khi nối thêm API.

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

Mở `http://localhost:3000`. Lệnh này chạy giao diện demo ngay cả khi chưa chạy backend. Riêng yêu cầu lấy nguồn hỗ trợ an toàn dùng `NEXT_PUBLIC_BACKEND_URL`, mặc định là `http://localhost:8080`; backend phải chạy và cho phép origin của web để yêu cầu đó thành công.

### Mobile

```powershell
cd front-end/my-app-mobile
npm install
npm run start
```

Expo sẽ hiển thị các cách mở ứng dụng trên thiết bị, emulator hoặc web. Đây vẫn là màn hình khởi tạo; chưa có luồng nghiệp vụ mylog hoặc kết nối API. Xem [README của mobile](front-end/my-app-mobile/README.md) để biết thêm về môi trường Expo.

## Tài liệu

- [Backend: cấu hình, chạy local và API](backend/README.md)
- [Kiến trúc backend](backend/docs/BACKEND_ARCHITECTURE.md)
- [Tổng quan database](backend/docs/DATABASE_OVERVIEW.md)
- [Kế hoạch phát triển backend](backend/docs/BACKEND_DEVELOPMENT_PLAN.md)
- [AI analysis và safety](backend/docs/AI_ANALYSIS_GUIDE.md)
- [Safety classifier](safety-model/README.md)
- [Hướng dẫn làm việc trong repository](AGENTS.md)

Code, migration và test hiện tại quyết định trạng thái triển khai; các tài liệu kế hoạch mô tả cả phần còn mở.
