# mylog backend

Backend modular monolith của **mylog**, được khởi tạo bằng Java 21 và Spring Boot 4.1.1.

Kiến trúc đầy đủ: [Backend architecture](../docs/BACKEND_ARCHITECTURE.md).

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
```

## Chạy local

Khởi động PostgreSQL/pgvector và Redis:

```powershell
cd backend
docker compose up -d
```

Chạy ứng dụng:

```powershell
./mvnw.cmd spring-boot:run
```

Kiểm tra:

```text
GET http://localhost:8080/actuator/health
```

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
