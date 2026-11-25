# mylog

**mylog** là ứng dụng nhật ký và tự suy ngẫm: ghi chép, theo dõi cảm xúc và nhìn lại trải nghiệm của mình. Ứng dụng không chẩn đoán, điều trị hoặc thay thế chuyên gia sức khỏe tâm thần.

- Sản phẩm và lý do: [mission.md](mission.md)
- Quyết định kỹ thuật: [tech-stack.md](tech-stack.md)
- Các giai đoạn triển khai: [roadmap.md](roadmap.md)
- Hành vi từng tính năng: [spec/mucluc.md](spec/mucluc.md), nguồn sự thật duy nhất về feature

## Chạy local

### Backend

Cần Java 21 và Docker có Compose (đã có Maven Wrapper). Lệnh dưới dùng PostgreSQL/pgvector, Redis và Mailpit local; kiểm tra `.env` trước khi chạy.

```powershell
cd backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d
./mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080`, health: `/actuator/health`, Swagger (profile `local`): `/internal/swagger-ui`.
- Dùng Supabase hoặc dịch vụ khác: xem [backend/README.md](backend/README.md). Không commit `.env` hoặc thông tin xác thực.

### Web

Cần Node.js và npm tương thích Next.js 16.

```powershell
cd front-end/my-app-web
npm install
npm run dev
```

- Mở `http://localhost:3000`; backend phải đang chạy.
- `MYLOG_BACKEND_URL` (route API của Next.js) và `NEXT_PUBLIC_BACKEND_URL` (lấy nguồn hỗ trợ an toàn) đều mặc định `http://localhost:8080`. Backend phải cho phép origin của web.

### Mobile

```powershell
cd front-end/my-app-mobile
npm install
npm run start
```

- Đặt `EXPO_PUBLIC_API_BASE_URL` trong `.env` tới backend mà emulator hoặc điện thoại truy cập được (không kèm `/api/v1`). Chi tiết: [README của mobile](front-end/my-app-mobile/README.md).

## Tài liệu

- [Spec feature — mục lục](spec/mucluc.md)
- [Hướng dẫn làm việc trong repository (cho người và AI agent)](AGENTS.md)
- [Backend: cấu hình, chạy local và API](backend/README.md)
- [Kiến trúc backend](backend/docs/BACKEND_ARCHITECTURE.md)
- [Tổng quan database](backend/docs/DATABASE_OVERVIEW.md)
- [AI analysis và safety](backend/docs/AI_ANALYSIS_GUIDE.md)
- [Kế hoạch phát triển backend](backend/docs/BACKEND_DEVELOPMENT_PLAN.md)
- [Safety classifier](safety-model/README.md)
