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
MYLOG_JOBS_ENABLED
MYLOG_JOBS_POLL_DELAY_MS
MYLOG_AI_FAKE_ENABLED
MYLOG_INSIGHTS_MINIMUM_SAMPLES
MYLOG_REPORTS_ENABLED
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

## M3 Outbox và AI analysis

Flyway V8 tạo bảng job/analysis và bổ sung lease cho outbox; migration đã áp dụng lên Supabase.
Worker mặc định tắt (`MYLOG_JOBS_ENABLED=false`). Chỉ bật khi môi trường có schema V8 và
đã chốt classifier, safety policy cùng provider theo ADR-0003/0004. `MYLOG_AI_FAKE_ENABLED=true`
chỉ phục vụ local/test với dữ liệu synthetic; adapter này tạo reflection cố định và không gọi mạng.

Worker kiểm tra lại consent `AI_PROCESSING` và safety policy ngay lúc xử lý job. API đọc trạng thái
và reflection: `GET /api/v1/journal-entries/{entryId}/analysis`; retry có cooldown 60 giây:
`POST /api/v1/journal-entries/{entryId}/analysis:retry`. `GET` trả `status` kể cả khi reflection
chưa sẵn sàng; client không nên chờ vô hạn. Các metric `mylog.outbox.*` và `mylog.ai.*` chỉ gồm
metadata queue, kết quả, token, chi phí và độ trễ.

## M1 Identity, profile và consent

Các endpoint M1 được bật trong profile `local`, `staging`, `prod`; profile `test` chỉ bật khi integration test yêu cầu. Luồng local:

1. `POST /api/v1/auth/register` với email, password (tối thiểu 12 ký tự), timezone IANA, locale, `termsVersion`, `privacyVersion`, `acceptTerms=true`, `acceptPrivacy=true`.
2. Lấy mã xác minh từ Mailpit rồi gọi `POST /api/v1/auth/email-verifications:confirm` với `{ "token": "..." }`.
3. `POST /api/v1/auth/login` trả access JWT 10 phút và refresh token opaque 30 ngày. `POST /api/v1/auth/refresh` đổi refresh token mỗi lần; dùng lại token cũ sẽ revoke session family.
4. Dùng `Authorization: Bearer <accessToken>` cho `GET/PATCH /api/v1/me`, consent và session APIs. `PATCH /me` dùng `If-Match` từ ETag của `GET /me`.

M1 dùng BCrypt cost 12, mã hóa email/profile bằng AES-GCM envelope, HMAC có khóa cho email lookup và token hash. Khóa AES dùng để mã hóa payload có thể rotate bằng `MYLOG_IDENTITY_PREVIOUS_KEYS`; `MYLOG_IDENTITY_LOOKUP_KEY` phải giữ ổn định. JWT có thể chuyển khóa ký bằng `MYLOG_JWT_PREVIOUS_PUBLIC_KEYS`. Local/test có khóa phát triển mặc định; staging/prod yêu cầu khóa và SMTP từ secret manager/environment, không có fallback.

Persistence M1 dùng JPA entity và `EntityManager` trong `identity`/`user` `infrastructure/persistence`. Flyway quản lý schema PostgreSQL; Hibernate chạy ở chế độ `validate`. Các thao tác PostgreSQL đặc thù có thể dùng native SQL qua JPA.

## M2 Journal, check-in và safety đầu vào

Sau khi đăng nhập, gửi `Authorization: Bearer <accessToken>` cho các endpoint sau:

| Tài nguyên | Endpoint |
|---|---|
| Journal | `POST/GET /api/v1/journal-entries`, `GET/PATCH/DELETE /api/v1/journal-entries/{entryId}` |
| Favorite | `PUT/DELETE /api/v1/journal-entries/{entryId}/favorite` |
| Tag | `POST/GET /api/v1/journal-tags`, `PUT/DELETE /api/v1/journal-entries/{entryId}/tags/{tagId}` |
| Check-in | `PUT/GET /api/v1/check-ins/{localDate}`, `GET /api/v1/check-ins?from=&to=` |

`POST journal` cần `Idempotency-Key` dài 8–160 ký tự và `contentJson` là TipTap document đã allowlist. `PATCH/DELETE journal` cần `If-Match` bằng ETag trả từ GET/create; version xung đột trả `409`. Danh sách journal dùng cursor, `limit` 1–100, và lọc `from`, `to`, `tag`, `favorite`. Ngày được tính từ `occurredAt` với timezone IANA gửi trong request. Check-in dùng một bản ghi cho mỗi user/ngày; `PUT` thay cả metrics, note và activities của ngày đó. Note và journal payload được mã hóa trước khi ghi DB.

Safety ingress luôn chạy lúc tạo/sửa journal. Rule HIGH/CRITICAL đặt `analysisStatus=BLOCKED_BY_SAFETY`; nếu classifier không khả dụng thì trạng thái cũng bị chặn và outbox chỉ có ID/version cho lần screen lại. Chưa có classifier hoặc bộ nội dung/nguồn hỗ trợ được duyệt; các bản ghi `safety_resources` không được tự điền hotline. Frontend hiện là prototype dùng auth mock và `localStorage`; chưa kết nối API M1/M2.

`GET /api/v1/safety/resources?locale=vi-VN&country=VN` là API công khai, chỉ trả nguồn hỗ trợ đã được duyệt, có `verified_at` và nguồn HTTPS. Frontend safety modal dùng `NEXT_PUBLIC_BACKEND_URL` để đọc API này (mặc định `http://localhost:8080` khi phát triển local) và chỉ hiện liên hệ khi API trả dữ liệu đã xác minh. Classifier trả mức rủi ro thấp cũng không mở ordinary analysis nếu `safety_policy_versions` chưa có policy `APPROVED` đang hiệu lực với rule version, classifier provider/version và ngưỡng confidence khớp. Hiện chưa có policy được duyệt nên API nguồn hỗ trợ trả danh sách rỗng và journal vẫn ở chế độ fail-safe.

## M4 Dashboard, insight và report

Flyway V9 đã áp dụng lên Supabase. Các API cần Bearer token: `GET /api/v1/dashboard?range=7d|30d|90d`,
`GET /api/v1/insights?from=&to=&cursor=`, `GET /api/v1/reports?type=WEEKLY&cursor=` và
`GET /api/v1/reports/{reportId}`. Dashboard dùng check-in của ngày trước, journal mới nhất làm
fallback và kèm `source`; current journal streak đếm ngày có journal SAVED, không đếm check-in đơn lẻ.

Insight sleep–mood chỉ xuất hiện khi đủ cặp dữ liệu (`MYLOG_INSIGHTS_MINIMUM_SAMPLES`, mặc định 7),
có correlation strength, sample size và evidence cấu trúc; narrative không khẳng định nguyên nhân.
Report tuần/tháng được scheduler enqueue theo timezone user, worker tạo snapshot bất biến và
regenerate tạo version mới. `MYLOG_REPORTS_ENABLED` mặc định false; bật rõ khi cần scheduler/worker.
Narrative hiện là template từ metric, chưa dùng provider AI. Testcontainers local đo dashboard 30 ngày
trên 10.000 journal synthetic/user p95 45 ms (30 lần gọi sau warmup); cần đo lại trên staging.

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

M8 release preparation: xem [`docs/M8_THREAT_MODEL.md`](docs/M8_THREAT_MODEL.md) và
[`docs/M8_RELEASE_RUNBOOK.md`](docs/M8_RELEASE_RUNBOOK.md). Staging/prod mặc định không chạy
Flyway trong từng app instance; cần migration process riêng trước rollout.
Chạy image một lần với `--spring.profiles.active=staging,migrate` hoặc `prod,migrate`;
profile `migrate` không mở HTTP, không chạy worker và thoát sau Flyway.
`MYLOG_APP_PROFILE=api` ngăn scheduled worker, `worker` chỉ mở health HTTP,
`all` dành cho local/test. Journal/tag writes được giới hạn 60 lần/15 phút/user,
export creation 3 lần/ngày/user. Readiness staging/prod kiểm tra PostgreSQL;
liveness không gọi provider ngoài. M8 chưa đạt release gate cho đến khi hoàn thành
staging, restore drill và các phê duyệt privacy/safety.

- Chỉ `/actuator/health` được truy cập công khai.
- Mọi endpoint khác bị deny mặc định.
- CORS chỉ cho phép origin khai báo bởi `MYLOG_ALLOWED_ORIGINS`.
- JWT/resource server dependency đã có, nhưng decoder và auth endpoints sẽ được cấu hình trong phase Identity.

Đây là fail-closed baseline: endpoint nghiệp vụ mới phải khai báo authorization rõ ràng trước khi có thể truy cập.
