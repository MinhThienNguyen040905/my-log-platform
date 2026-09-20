# MyLog Backend

Spring Boot backend cho MyLog, được tổ chức theo modular monolith và có thể chạy API/worker bằng các profile riêng.

## Yêu cầu

- Java 21 trở lên.
- Docker và Docker Compose cho Redis, RabbitMQ và PostgreSQL local/test.
- Không cần cài Maven toàn cục; project sử dụng Maven Wrapper.
- Một Supabase project cho staging/production.

## Kết nối Supabase PostgreSQL

Backend kết nối trực tiếp tới PostgreSQL của Supabase bằng JDBC; không dùng Supabase Data API.

Trong Supabase Dashboard, chọn **Connect > Session pooler** rồi lấy chính xác host, username và database password. Session pooler phù hợp với API/worker chạy lâu dài và hoạt động trên mạng IPv4.

Thiết lập biến môi trường trong PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'api,supabase'
$env:DB_URL = 'jdbc:postgresql://YOUR_POOLER_HOST:5432/postgres'
$env:DB_USERNAME = 'postgres.YOUR_PROJECT_REF'
$env:DB_PASSWORD = 'YOUR_DATABASE_PASSWORD'
$env:DB_SSL_MODE = 'require'
```

Không commit database password, connection string chứa password hoặc file `.env`. File [.env.example](./.env.example) chỉ là mẫu tên biến.

Mặc định Flyway dùng cùng kết nối với application. Nếu server/CI hỗ trợ IPv6, có thể dùng direct connection riêng cho migration:

```powershell
$env:FLYWAY_URL = 'jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres'
$env:FLYWAY_USERNAME = 'postgres'
$env:FLYWAY_PASSWORD = 'YOUR_DATABASE_PASSWORD'
```

Chạy Redis và RabbitMQ local rồi khởi động API:

```powershell
docker compose up -d redis rabbitmq
.\mvnw.cmd spring-boot:run
```

Flyway tự động áp dụng migration trong `src/main/resources/db/migration` lên Supabase khi ứng dụng khởi động.

Migration `V002` thu hồi quyền Data API của các role `anon`/`authenticated` và bật RLS không-policy cho bảng nghiệp vụ. Mọi truy cập dữ liệu đi qua backend, nơi thực thi authentication và authorization.

## Phát triển offline với PostgreSQL local

PostgreSQL Docker vẫn được giữ cho development và integration test không phụ thuộc mạng:

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Profile mặc định `local` dùng database `jdbc:postgresql://localhost:5432/mylog`.

## Profiles

```text
local       Local development với PostgreSQL Docker
api         REST API runtime
worker      Background worker runtime không có HTTP server
supabase    Supabase datasource, SSL và connection pool
test        Automated tests
```

Chạy worker với Supabase:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'worker,supabase'
.\mvnw.cmd spring-boot:run
```

## Endpoint nền tảng

- API info: `http://localhost:8080/api/v1/system/info`
- Health: `http://localhost:8080/actuator/health`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- RabbitMQ UI: `http://localhost:15672`

## Xác minh

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Tài liệu kiến trúc

- [Backend Architecture](../docs/BACKEND_ARCHITECTURE.md)
- [Database Design](../docs/DATABASE_DESIGN.md)
- [Software Requirements](../docs/SOFTWARE_REQUIREMENTS_SPECIFICATION.md)
