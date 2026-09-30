# mylog backend

Backend modular monolith của **mylog**, được khởi tạo bằng Java 21 và Spring Boot 4.1.1.

Tài liệu thiết kế:

- [Backend architecture](docs/BACKEND_ARCHITECTURE.md)
- [Database design](docs/DATABASE_OVERVIEW.md)
- [Database DBML](docs/database/mylog.dbml)
- [Backend development plan](docs/BACKEND_DEVELOPMENT_PLAN.md)
- [Architecture decisions](docs/adr/README.md)

## Yêu cầu

- Java 21+
- Docker Desktop hoặc Docker Engine có Compose
- Không cần cài Maven; repository đã có Maven Wrapper

## Biến môi trường với `.env`

mylog dùng Spring Boot Externalized Configuration. File `application.yml` import trực tiếp file `.env`:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Khởi tạo cấu hình local:

```powershell
cd backend
Copy-Item .env.example .env
```

Repository đã có một `.env` local để chạy ngay trên máy hiện tại. File này bị Git ignore; chỉ `.env.example` được commit.

Thứ tự ưu tiên quan trọng của Spring Boot vẫn được giữ nguyên. Biến môi trường thật của hệ điều hành hoặc container có thể override giá trị đọc từ `.env`. Vì vậy production nên inject secret từ secret manager/container environment, không đóng gói `.env` vào image.

Các nhóm biến hiện có:

```text
SPRING_PROFILES_ACTIVE
MYLOG_APP_PROFILE
MYLOG_SERVER_PORT
MYLOG_DB_*
MYLOG_REDIS_URL
MYLOG_ALLOWED_ORIGINS
MYLOG_OPENAPI_ENABLED
MYLOG_SWAGGER_UI_ENABLED
MYLOG_CLOUDINARY_*
```

Database local có thể dùng Docker Compose; môi trường được triển khai dùng PostgreSQL do Supabase quản lý qua `MYLOG_DB_URL`, `MYLOG_DB_USERNAME` và `MYLOG_DB_PASSWORD`. Ảnh nhật ký dùng Cloudinary; bật adapter bằng `MYLOG_CLOUDINARY_ENABLED=true` sau khi điền credential server-side.

## Chạy local

Nếu `.env` trỏ PostgreSQL tới Supabase, khởi động Redis và Mailpit để chạy identity/email verification:

```powershell
cd backend
docker compose up -d redis mailpit
```

Nếu muốn phát triển hoàn toàn offline bằng PostgreSQL/pgvector local, đổi `MYLOG_DB_*` về giá trị trong `.env.example` rồi chạy `docker compose up -d`. Các biến `MYLOG_LOCAL_DB_*` của Compose được tách riêng để credential Supabase không bị dùng cho container local.

Mailpit UI: `http://localhost:8025`. Nếu dùng PostgreSQL local thay Supabase, chạy `docker compose up -d` để khởi động cả ba service.

Chạy ứng dụng:

```powershell
./mvnw.cmd spring-boot:run
```

Kiểm tra:

```text
GET http://localhost:8080/actuator/health
```

OpenAPI trong profile local:

```text
GET http://localhost:8080/internal/openapi
GET http://localhost:8080/internal/swagger-ui
```

API docs mặc định bị tắt trong production.

## M1 Identity, profile và consent

Các endpoint M1 được bật trong profile `local`, `staging`, `prod`; profile `test` chỉ bật khi integration test yêu cầu. Luồng local:

1. `POST /api/v1/auth/register` với email, password (tối thiểu 12 ký tự), timezone IANA, locale, `termsVersion`, `privacyVersion`, `acceptTerms=true`, `acceptPrivacy=true`.
2. Lấy mã xác minh từ Mailpit rồi gọi `POST /api/v1/auth/email-verifications:confirm` với `{ "token": "..." }`.
3. `POST /api/v1/auth/login` trả access JWT 10 phút và refresh token opaque 30 ngày. `POST /api/v1/auth/refresh` đổi refresh token mỗi lần; dùng lại token cũ sẽ revoke session family.
4. Dùng `Authorization: Bearer <accessToken>` cho `GET/PATCH /api/v1/me`, consent và session APIs. `PATCH /me` dùng `If-Match` từ ETag của `GET /me`.

M1 dùng BCrypt cost 12, mã hóa email/profile bằng AES-GCM envelope, HMAC có khóa cho email lookup và token hash. Khóa AES dùng để mã hóa payload có thể rotate bằng `MYLOG_IDENTITY_PREVIOUS_KEYS`; `MYLOG_IDENTITY_LOOKUP_KEY` phải giữ ổn định. JWT có thể chuyển khóa ký bằng `MYLOG_JWT_PREVIOUS_PUBLIC_KEYS`. Local/test có khóa phát triển mặc định; staging/prod yêu cầu khóa và SMTP từ secret manager/environment, không có fallback.

Persistence M1 dùng JPA entity và `EntityManager` trong `identity`/`user` `infrastructure/persistence`. Flyway quản lý schema PostgreSQL; Hibernate chạy ở chế độ `validate`. Các thao tác PostgreSQL đặc thù có thể dùng native SQL qua JPA.

## Supabase PostgreSQL

Project `mylog` dùng Supabase PostgreSQL 17 và kết nối qua IPv4 session pooler với SSL. Flyway vẫn là nguồn quản lý schema; không sửa schema production trực tiếp bằng Table Editor.

Credential nằm trong `.env` local hoặc secret manager khi deploy. Không đưa database password, service-role key hoặc connection string chứa password vào Git.

## Cloudinary

Cloudinary chỉ dùng cho ảnh journal. Backend đã có Cloudinary Java SDK và fail-fast validation khi `MYLOG_CLOUDINARY_ENABLED=true`. Cần cấu hình bốn biến server-side:

```text
MYLOG_CLOUDINARY_CLOUD_NAME
MYLOG_CLOUDINARY_API_KEY
MYLOG_CLOUDINARY_API_SECRET
MYLOG_CLOUDINARY_FOLDER=mylog
```

Không gửi `MYLOG_CLOUDINARY_API_SECRET` xuống frontend. Quy tắc signed delivery, metadata và deletion được chốt trong [ADR-0006](docs/adr/0006-cloudinary-image-storage.md).

Tắt hạ tầng local nhưng giữ dữ liệu:

```powershell
docker compose down
```

## Test và build

Test integration dùng Testcontainers với PostgreSQL/pgvector và Redis:

```powershell
./mvnw.cmd test
```

Build artifact:

```powershell
./mvnw.cmd clean package
```

Kiểm tra tên và thứ tự Flyway migration:

```powershell
./scripts/check-migrations.ps1
```

Chạy dependency vulnerability scan:

```powershell
./mvnw.cmd -Psecurity-checks verify
```

Report được tạo tại `target/dependency-check-report.html`. CI còn chạy Gitleaks, xuất OpenAPI JSON và build container trên mỗi pull request.

Build container:

```powershell
docker build -t mylog-backend:local .
```

## Cấu trúc ban đầu

```text
src/main/java/com/mylog/
├── MylogApplication.java
├── identity/
├── user/
├── journal/
├── checkin/
├── safety/
├── analysis/
├── insight/
├── reporting/
├── selfcare/
├── knowledge/
├── prompt/
├── export/
├── admin/
├── audit/
├── feedback/
└── platform/
    ├── config/
    └── security/
```

Mỗi module nghiệp vụ sẽ được phát triển theo `api/application/domain/infrastructure`. Các package rỗng hiện được giữ bằng `package-info.java` để thể hiện ranh giới ngay từ đầu.

## Trạng thái security ban đầu

- Chỉ `/actuator/health` được truy cập công khai.
- Mọi endpoint khác bị deny mặc định.
- CORS chỉ cho phép origin khai báo bởi `MYLOG_ALLOWED_ORIGINS`.
- JWT/resource server dependency đã có, nhưng decoder và auth endpoints sẽ được cấu hình trong phase Identity.

Đây là fail-closed baseline: endpoint nghiệp vụ mới phải khai báo authorization rõ ràng trước khi có thể truy cập.
