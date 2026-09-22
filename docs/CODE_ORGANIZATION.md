# MYLOG – CODE ORGANIZATION

## 1. Kiến trúc được chọn

MyLog sử dụng **pragmatic modular monolith**:

- Package cấp một theo feature: `identity`, `journal`, `analysis`.
- Layer quen thuộc bên trong từng feature.
- Ports/adapters chỉ dùng cho boundary có khả năng thay đổi, ví dụ AI provider.
- Hạ tầng dùng chung nằm trong `common`.

Đây không phải Clean Architecture tuyệt đối. JPA entity có thể đồng thời là domain entity trong MVP để tránh duplicate model và mapper không cần thiết.

## 2. Cấu trúc chuẩn

```text
feature/
├── controller/   REST controller và web-only helper
├── dto/          request/response contract
├── service/      use case, command/result nội bộ và transaction
├── entity/       persistent entity, enum và invariant
├── repository/   JPA/JDBC persistence
├── provider/     external provider boundary và adapter, nếu có
├── messaging/    RabbitMQ inbound/outbound adapter, nếu có
├── security/     feature-specific security adapter, nếu có
└── config/       typed properties và wiring, nếu có
```

Chỉ tạo folder khi có code thật. Feature tương lai không được biểu diễn bằng `package-info.java` rỗng.

## 3. Dependency rules

```text
controller ─┐
messaging  ─┴─→ service → repository → PostgreSQL/Redis
                         → provider   → external API
```

1. Controller chỉ validate/map HTTP DTO và gọi service.
2. Controller không gọi repository.
3. Service không import controller hoặc DTO.
4. Service không truy cập JPA/JDBC/Redis trực tiếp; dùng repository hoặc explicit port.
5. Repository không chứa HTTP concern và không phụ thuộc service.
6. Entity không phụ thuộc controller, DTO, service, repository hoặc messaging.
7. Feature không import repository/service/entity nội bộ của feature khác.
8. `common` không phụ thuộc feature.
9. Business event không chứa journal content hoặc secret.

ArchUnit kiểm tra tự động các rule có thể kiểm tra tĩnh.

## 4. Naming

| Loại | Quy ước | Ví dụ |
| --- | --- | --- |
| Controller | `*Controller` | `AuthController` |
| HTTP request | `*Request` | `LoginRequest` |
| HTTP response | `*Response` | `AuthResponse` |
| Service | `*Service`, `*Worker` | `JournalService` |
| Internal input | `*Command` | `CorrectionCommand` |
| Internal output | `*Result`, `*View` | `AnalysisView` |
| Entity | danh từ nghiệp vụ | `JournalEntry` |
| Repository | `*Repository` | `UserRepository` |
| External boundary | `*Port` | `AiAnalysisPort` |
| Provider adapter | `*Adapter` | `OpenAiAnalysisAdapter` |
| Message consumer | `*Consumer` | `AnalysisRequestedConsumer` |

## 5. Mapping boundary

HTTP DTO được map tại controller hoặc DTO factory:

```text
CorrectionRequest → CorrectionCommand → CorrectionService
AnalysisView → AnalysisResponse
```

Không truyền `CorrectionRequest` vào service. Điều này giúp service được dùng lại từ scheduler, RabbitMQ consumer hoặc test mà không phụ thuộc HTTP.

## 6. Module hiện tại

### Identity

Authentication, refresh-token rotation, user profile, preferences và account status.
Redis rate limit được đặt sau `RateLimitStore`; adapter cụ thể nằm tại `identity/security`.

### Journal

Journal CRUD, ownership, optimistic locking, idempotency và phát outbox event.

### Analysis

AI provider, job worker, safety, correction và reflection. Safety/reflection là capability bên trong Analysis ở MVP vì dùng chung version, transaction và provider lifecycle.

### Common

API error, exception handling, security framework, messaging topology, transactional outbox, logging và web filter. Không chứa business rule của feature.

## 7. Khi thêm M5

Chỉ tạo `statistics` và `insight` khi bắt đầu implementation. Mỗi module mới phải theo cùng cấu trúc và vượt qua architecture tests trước khi merge.
