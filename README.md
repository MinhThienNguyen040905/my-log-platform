# mylog

mylog là nền tảng nhật ký thông minh hỗ trợ self-reflection và theo dõi mental wellness bằng AI. Sản phẩm không phải công cụ chẩn đoán hoặc thay thế chuyên gia sức khỏe tâm thần.

Repository gồm frontend prototype, backend Spring Boot đã bootstrap và tài liệu kiến trúc.

## Backend

Backend được thiết kế theo modular monolith với Java/Spring Boot, PostgreSQL/pgvector, Redis và background jobs. Thiết kế ưu tiên privacy, safety, khả năng truy vết insight và khả năng mở rộng.

- [Tổng quan backend](backend/README.md)
- [Kiến trúc backend chi tiết](backend/docs/BACKEND_ARCHITECTURE.md)
- [Thiết kế database](backend/docs/DATABASE_OVERVIEW.md)
- [Lộ trình phát triển backend](backend/docs/BACKEND_DEVELOPMENT_PLAN.md)

Chạy backend local:

```powershell
cd backend
docker compose up -d
./mvnw.cmd spring-boot:run
```

## Frontend

Frontend nằm tại `front-end/my-app` và sử dụng:

- Next.js 16 (App Router)
- React 19
- TypeScript 5
- Tailwind CSS v4
- Motion
- i18next

## Chạy frontend

Yêu cầu Node.js 20 hoặc mới hơn.

```bash
cd front-end/my-app
npm install
npm run dev
```

Mở `http://localhost:3000` trong trình duyệt.

## Cấu trúc repository

```text
my-log-platform/
├── README.md
├── backend/
│   ├── src/
│   ├── compose.yaml
│   ├── Dockerfile
│   ├── pom.xml
│   ├── docs/
│   │   └── BACKEND_ARCHITECTURE.md
│   └── README.md
└── front-end/
    └── my-app/
```
