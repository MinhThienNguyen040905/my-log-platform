# MyLog Backend

Spring Boot backend cho MyLog, được tổ chức theo modular monolith và có thể chạy API/worker bằng các profile riêng.

## Tổ chức source code

Backend dùng package-by-feature với layered architecture thực dụng:

```text
com.mylog
├── identity/{controller,dto,service,entity,repository,security,config}
├── journal/{controller,dto,service,entity,repository}
├── analysis/{controller,dto,service,entity,repository,provider,messaging,config}
└── common/{api,controller,config,exception,logging,messaging,outbox,security,web}
```

Luồng mặc định là `controller/messaging → service → repository/provider`. Controller không gọi repository; service không dùng HTTP DTO hoặc persistence framework trực tiếp; repository không phụ thuộc service; `common` không phụ thuộc feature. Các quy tắc này được khóa bằng ArchUnit. Không tạo package rỗng cho feature chưa triển khai. Xem [Code Organization](../docs/CODE_ORGANIZATION.md).

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
$env:JWT_SIGNING_KEY_BASE64 = 'YOUR_BASE64URL_ENCODED_RANDOM_KEY'
```

Tạo JWT signing key tối thiểu 32 byte cho shell hiện tại:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($keyBytes) } finally { $rng.Dispose() }
$env:JWT_SIGNING_KEY_BASE64 = [Convert]::ToBase64String($keyBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
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

Migration `V002` bật RLS không-policy và thu hồi quyền Data API trực tiếp; `V004` thu hồi thêm quyền kế thừa từ PostgreSQL pseudo-role `PUBLIC`. Mọi truy cập dữ liệu đi qua backend, nơi thực thi authentication và authorization.

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
integration-test  Testcontainers PostgreSQL, Redis và RabbitMQ; không dùng Supabase
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

## Identity API

```text
POST  /api/v1/auth/register
POST  /api/v1/auth/login
POST  /api/v1/auth/refresh
POST  /api/v1/auth/logout
GET   /api/v1/users/me
PATCH /api/v1/users/me
```

Register yêu cầu `email`, `password`, `displayName`, `acceptTerms=true` và `acceptPrivacy=true`. Login/register trả access token trong JSON; refresh token chỉ được gửi bằng cookie `HttpOnly`. Cookie dùng `SameSite=Strict`, `Secure=true` mặc định và được hạ xuống `false` chỉ trong profile local/integration-test.

Password dài 10–72 ký tự, tối đa 72 byte UTF-8 và phải có chữ hoa, chữ thường cùng chữ số. Email được chuẩn hóa bằng `trim + lowercase` trước khi kiểm tra unique.

## Journal API

```text
POST   /api/v1/journals
GET    /api/v1/journals?cursor=&limit=&from=&to=
GET    /api/v1/journals/{journalId}
PATCH  /api/v1/journals/{journalId}
DELETE /api/v1/journals/{journalId}
```

`POST` chấp nhận `Idempotency-Key` tùy chọn. `PATCH` yêu cầu `version` hiện tại trong JSON và trả `409 JOURNAL_VERSION_CONFLICT` nếu journal đã được sửa; create/get/update cũng trả `ETag`. History dùng opaque cursor theo `(created_at, id)`, mặc định 20 và tối đa 100 phần tử.

`occurredAt` được kết hợp với IANA `timezoneAtEntry` để tạo `entryDate`. Delete là soft-delete và mọi repository query đều scope theo authenticated user, vì vậy journal không tồn tại, đã xóa hoặc thuộc user khác đều trả `404 JOURNAL_NOT_FOUND`.

## Transactional outbox và RabbitMQ

Create/update/delete journal ghi event vào PostgreSQL trong cùng transaction. Publisher claim bằng `FOR UPDATE SKIP LOCKED`, chờ RabbitMQ publisher confirm rồi mới đánh dấu `PUBLISHED`; failure được retry exponential backoff và chuyển sang `mylog.dead` sau giới hạn cấu hình.

Các event Journal hiện có: `journal.created`, `journal.updated`, `journal.deleted` và `journal.analysis.requested`. Payload chỉ chứa ID/version, không chứa journal content. Chi tiết recovery và replay xem [Messaging Runbook](../docs/MESSAGING_RUNBOOK.md).

## AI analysis, safety và reflection

`journal.analysis.requested` được consumer idempotent chuyển thành `analysis_jobs`. Worker claim job bằng `FOR UPDATE SKIP LOCKED`, chạy rule safety trước provider, validate structured output rồi mới ghi analysis, emotion, topic, reflection và usage metadata. Kết quả của journal version cũ được đánh dấu `OBSOLETE` và không thể trở thành current result.

Local/test dùng adapter `mock` xác định. Production hỗ trợ một adapter OpenAI Responses API qua `AI_PROVIDER=openai`; API key chỉ được đọc từ environment. API runtime tắt AI consumer/scheduler, worker runtime bật chúng để có thể scale độc lập. Cấu hình và quy trình xử lý job lỗi xem [AI Operations Runbook](../docs/AI_OPERATIONS_RUNBOOK.md).

Các endpoint M4:

```text
GET   /api/v1/journals/{journalId}/analysis
POST  /api/v1/journals/{journalId}/analysis/retry
PATCH /api/v1/journals/{journalId}/corrections
GET   /api/v1/journals/{journalId}/reflections
POST  /api/v1/journals/{journalId}/reflections/regenerate
```

## Statistics, dashboard, insight và feedback

M5 dùng query deterministic trên PostgreSQL và chỉ dùng effective emotion/topic sau correction. Kết quả dashboard/statistics được cache theo user, range, timezone và calculation version; Redis lỗi sẽ fallback về PostgreSQL. Journal/analysis/correction event invalid cache rồi phát `statistics.updated` để Insight worker refresh evidence.

```text
GET /api/v1/dashboard?from=&to=&timezone=
GET /api/v1/statistics/mood?from=&to=&timezone=
GET /api/v1/statistics/emotions?from=&to=&timezone=
GET /api/v1/statistics/topics?from=&to=&timezone=
GET /api/v1/insights
GET /api/v1/insights/{insightId}
PUT /api/v1/feedback/{targetType}/{targetId}
```

Insight cần ít nhất ba ngày bằng chứng, lưu `calculationVersion`, phân loại `WEAK/MODERATE/STRONG` và luôn mô tả association thay vì causation. HIGH/CRITICAL evidence window không sinh suggested action.

## Xác minh

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

`verify` chạy unit test, ArchUnit và integration test trên hạ tầng Testcontainers. Bài kiểm tra schema Supabase thật được bảo vệ bằng biến `RUN_SUPABASE_IT=true`, nên CI thông thường không thể kết nối nhầm vào Supabase.

## Tài liệu kiến trúc

- [Backend Implementation Plan](../docs/BACKEND_IMPLEMENTATION_PLAN.md)
- [Backend Architecture](../docs/BACKEND_ARCHITECTURE.md)
- [Database Design](../docs/DATABASE_DESIGN.md)
- [AI Operations Runbook](../docs/AI_OPERATIONS_RUNBOOK.md)
- [Software Requirements](../docs/SOFTWARE_REQUIREMENTS_SPECIFICATION.md)
