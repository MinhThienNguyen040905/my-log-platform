# mylog backend

Backend của **mylog** được định hướng là modular monolith trên Java/Spring Boot. Thư mục này hiện là vị trí dành cho source backend; chưa bootstrap ứng dụng để tránh khóa team vào dependency/version trước khi chốt các quyết định nền tảng.

Blueprint đầy đủ: [Backend architecture](../docs/BACKEND_ARCHITECTURE.md).

## Stack mục tiêu

- Java 21, Spring Boot 3.x, Maven Wrapper
- PostgreSQL + pgvector, Flyway
- Redis cho cache/rate limit/lock ngắn hạn
- Spring Security với access JWT + rotating refresh token
- Database-backed job và transactional outbox cho MVP
- Testcontainers, JUnit 5, ArchUnit
- Docker cho local và deployment

## Nguyên tắc tổ chức

- Package theo nghiệp vụ: `journal`, `safety`, `analysis`, `insight`, `reporting`, `knowledge`...
- Bên trong mỗi nghiệp vụ: `api`, `application`, `domain`, `infrastructure`.
- Domain không phụ thuộc Spring/JPA/HTTP.
- Module khác không truy cập repository/entity nội bộ.
- AI không nằm trên critical path của thao tác lưu nhật ký.
- Safety screening chạy trước generative AI.
- Admin mặc định không thể đọc nội dung nhật ký.

## Thứ tự bootstrap đề xuất

1. Khởi tạo Spring Boot và local Docker Compose.
2. Thêm error contract, migration, security và observability nền tảng.
3. Làm identity/profile rồi journal/check-in.
4. Nối frontend với API thật.
5. Thêm safety, outbox worker và AI analysis.
6. Sau đó mới làm insight/reporting/RAG/admin.

## Package gốc

```text
com.mylog
```

Tên artifact gợi ý:

```text
groupId: com.mylog
artifactId: mylog-backend
name: mylog
```
