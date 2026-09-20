# MyLog Backend

Spring Boot backend cho MyLog, được tổ chức theo modular monolith và có thể chạy API/worker bằng các profile riêng.

## Requirements

- Java 21 trở lên.
- Docker và Docker Compose.
- Không cần cài Maven toàn cục; project sử dụng Maven Wrapper.

## Quick start

Sao chép biến môi trường mẫu nếu cần tùy chỉnh:

```powershell
Copy-Item .env.example .env
```

Khởi động infrastructure:

```powershell
docker compose up -d
```

Chạy backend local:

```powershell
.\mvnw.cmd spring-boot:run
```

Các endpoint nền tảng:

- API info: `http://localhost:8080/api/v1/system/info`
- Health: `http://localhost:8080/actuator/health`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- RabbitMQ UI: `http://localhost:15672`

## Profiles

```text
local  Local development, API enabled, verbose application logging
api    REST API runtime
worker Background worker runtime without HTTP server
test   Automated tests
```

Chạy API profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'api'
.\mvnw.cmd spring-boot:run
```

Chạy worker profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'worker'
.\mvnw.cmd spring-boot:run
```

## Verification

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Architecture references

- [Backend Architecture](../docs/BACKEND_ARCHITECTURE.md)
- [Database Design](../docs/DATABASE_DESIGN.md)
- [Software Requirements](../docs/SOFTWARE_REQUIREMENTS_SPECIFICATION.md)
