# Hướng dẫn cho AI agent trong my-log-platform

Tài liệu này cung cấp ngữ cảnh khi bắt đầu một phiên làm việc mới. Các quy tắc dưới đây áp dụng cho `backend/`. Khi làm việc trong `front-end/my-app/`, đọc thêm `front-end/my-app/AGENTS.md`. Yêu cầu trực tiếp của người dùng trong phiên hiện tại được ưu tiên nếu khác với hướng dẫn ở đây.

## 1. Đọc gì trước khi sửa backend

1. Đọc phần liên quan trong `backend/docs/BACKEND_ARCHITECTURE.md` để hiểu module, luồng dữ liệu và quy ước code.
2. Đọc `backend/docs/BACKEND_DEVELOPMENT_PLAN.md` để biết dependency, acceptance/exit criteria và checklist tiến độ.
3. Đọc `backend/docs/DATABASE_OVERVIEW.md` và `backend/docs/database/mylog.dbml` trước khi sửa persistence; Flyway migration mới là schema thực thi.
4. Đọc `backend/docs/adr/README.md` và ADR liên quan trước khi thay đổi quyết định nền tảng. ADR `Accepted` chỉ bị thay thế bằng ADR mới, không sửa mất lịch sử quyết định.
5. Kiểm tra code, migration và test hiện tại trước khi kết luận một tính năng đã hoàn thành. Checklist trong kế hoạch là ảnh chụp tiến độ tại thời điểm viết, không thay thế việc kiểm tra repository.

Nếu tài liệu, code và migration không khớp, chỉ rõ sự khác biệt; không âm thầm coi cây thư mục minh họa trong tài liệu là code đã tồn tại. Một số tên file trong cây minh họa của `BACKEND_ARCHITECTURE.md` đã cũ.

## 2. Sản phẩm và các quyết định đã chốt

- Tên sản phẩm trong code/tài liệu: **mylog**. Đây là ứng dụng nhật ký và self-reflection với dữ liệu đặc biệt nhạy cảm; không chẩn đoán hoặc điều trị.
- Backend là Java 21, Spring Boot 4.1.x, Maven Wrapper, modular monolith theo feature. PostgreSQL/pgvector là source of truth; Redis chỉ dùng cho cache ngắn hạn, rate limit và lock, không làm queue duy nhất cho dữ liệu quan trọng.
- Identity MVP dùng email/password local, access JWT ngắn hạn, opaque refresh token chỉ lưu hash, rotation và token-family reuse detection (ADR-0001).
- Dữ liệu nhạy cảm được mã hóa ở application bằng envelope encryption AES-256-GCM; production dùng KMS/secret manager, local/test dùng adapter riêng (ADR-0002).
- Safety screening nhiều lớp và fail-safe trước phản hồi sinh bởi AI (ADR-0003).
- AI đi qua application ports; provider adapter ở infrastructure, tối thiểu hóa dữ liệu gửi đi và kiểm tra điều kiện retention/consent (ADR-0004).
- Ảnh journal dùng Cloudinary với quyền truy cập/delivery có ký, metadata trong DB; ADR-0006 thay ADR-0005 **đối với ảnh**. Storage cho export PDF/CSV chưa được chốt bởi ADR-0006.

## 3. Tiến độ: luôn xác minh trước khi triển khai

- `BACKEND_DEVELOPMENT_PLAN.md` ghi M0 foundation đã hoàn thành. M1 backend đã có identity/profile/consent API, V2–V4 và integration tests; mục tích hợp frontend và kiểm tra consent tại lúc AI worker chạy còn mở. Các milestone M2+ chưa triển khai.
- `platform` có security baseline, error contract, request ID, configuration, UUIDv7, OpenAPI, log redaction và Cloudinary configuration. Identity/user đã có use case và JPA adapter; các feature khác chủ yếu vẫn là package skeleton. Flyway có V1–V4.
- Không giả định các class trong cây ví dụ của tài liệu (như `JournalCommandService`) đã tồn tại. Mỗi phiên cần kiểm tra file/migration/test và `git status` thực tế, nhất là khi người dùng có thay đổi chưa lưu trong IDE.
- Thứ tự phụ thuộc mục tiêu: M0 foundation → M1 identity/profile → M2 journal/check-in/safety đầu vào → M3 outbox/AI → M4 insight/report; M5 self-care có thể theo sau M2; M6 knowledge/admin, M7 data rights, M8 hardening theo kế hoạch.

## 4. Tổ chức package và chiều phụ thuộc

Mỗi feature sống dưới `backend/src/main/java/com/mylog/<feature>/` và chỉ tạo các lớp cần thiết:

```text
api/             HTTP controller, request/response DTO, mapping HTTP
application/     use case, transaction boundary, ports, command/query
domain/          invariant, value object, policy, domain event
infrastructure/  JPA, Redis, crypto/provider adapter, external SDK
```

- Chiều phụ thuộc: `api → application → domain`; infrastructure triển khai port do application sở hữu. Domain không phụ thuộc Spring, JPA, servlet, HTTP, Redis hoặc AI SDK.
- Module khác chỉ gọi public application facade/use case hoặc nhận event; không import JPA entity, repository hay adapter nội bộ của module khác.
- Controller lấy current user, validate HTTP, gọi use case và map response; không chứa business rule hoặc gọi repository trực tiếp.
- Với endpoint có JSON body, đặt mỗi request record vào một file riêng trong `<feature>/api/request/`; đặt mỗi response record vào một file riêng trong `<feature>/api/response/`. Controller không khai báo record DTO lồng bên trong và không trả trực tiếp application result, domain object hay JPA entity. Endpoint chỉ trả status, không có body thì không cần tạo response DTO rỗng.
- Dữ liệu trả về từ use case thuộc `application/query/` hoặc `application/result/` khi cần một kiểu riêng; API map rõ ràng sang response DTO. Ví dụ hiện có: `IdentityController` dùng `identity/api/request` và `identity/api/response`; `UserController` dùng `user/api/request` và `user/api/response`, còn `ProfileView`/`ConsentView` nằm trong `user/application/query`. Giữ tên field JSON và validation contract khi chỉ chuyển vị trí class.
- Application service điều phối use case và đặt transaction boundary. Domain giữ invariant. JPA entity chỉ ở infrastructure, không trả ra API. Query đơn giản có thể dùng projection qua query port; không ép mọi feature có đủ lớp của ví dụ journal.
- Persistence PostgreSQL của dự án dùng JPA/Hibernate. Đặt `@Entity`, khóa ghép và repository adapter trong `<feature>/infrastructure/persistence/`; application chỉ phụ thuộc repository port và kiểu dữ liệu application. M1 `identity`/`user` dùng `EntityManager` và entity trong các package `infrastructure/persistence`, không dùng `JdbcTemplate` trong production code. Với thao tác cần tính năng PostgreSQL hoặc tính nguyên tử rõ ràng (`ON CONFLICT`, `DISTINCT ON`, worker claim `FOR UPDATE SKIP LOCKED`), dùng native SQL qua JPA và test bằng PostgreSQL thật. Không đổi sang JDBC chỉ vì JPQL không biểu đạt được thao tác đó.
- Đặt tên class JPA ngắn gọn theo đối tượng/bảng, không thêm hậu tố `Entity`: `User`, `AuthSession`, `UserProfile`, `UserConsent`. Tên biến cũng theo đối tượng (`User user = new User()`, `AuthSession session = ...`), không dùng biến chung chung `entity`. Package `infrastructure/persistence` và annotation `@Entity` đã chỉ rõ vai trò của class; giữ application record và API DTO tách biệt.
- Flyway là nguồn schema thực thi; `spring.jpa.hibernate.ddl-auto=validate` chỉ kiểm tra ánh xạ, không tạo hoặc sửa bảng. Migration đã áp dụng lên Supabase là bất biến; khi đổi schema, thêm migration mới. Entity chỉ thuộc module sở hữu và không được trả trực tiếp qua API.
- Khi thiết kế bảng mới, mặc định dùng `id UUID` tạo ở application (UUIDv7). Ngoại lệ có chủ đích: bảng nối thuần túy có thể dùng khóa ghép FK; quan hệ một–một có thể dùng FK chung làm PK; bảng trạng thái một dòng/subject có thể dùng subject key làm PK. Bản ghi có vòng đời như `auth_refresh_history` dùng `id UUID` và giữ `UNIQUE(token_hash)` cho lookup/replay. Nếu thay khóa nghiệp vụ bằng `id`, giữ ràng buộc unique tương ứng; cập nhật DBML, migration, JPA mapping và test cùng lúc. Không sửa migration đã áp dụng.
- `platform` là nơi đặt **cơ chế kỹ thuật dùng chung**: config, web/error contract, security primitives, ID, observability, jobs. Không đặt business rule của một feature vào `platform`; không tạo thêm `common`/`shared` chỉ để gom code chưa rõ chủ sở hữu.
- Thành phần chỉ phục vụ identity/journal, kể cả crypto adapter, nên ở module sở hữu. Chỉ chuyển primitive sang `platform` khi nhiều module thực sự dùng cùng một contract kỹ thuật.
- Test đi theo feature/package tương ứng. ArchUnit hiện mới kiểm tra một phần ranh giới; tuân thủ toàn bộ quy tắc trong tài liệu kể cả khi test chưa bắt được.

## 5. API, exception và lỗi

- API v1 dùng `/api/v1`, JSON `camelCase`, UUID string, thời gian ISO-8601; enum contract theo tài liệu. Request và response DTO thuộc `api` của feature, tách khỏi domain/JPA entity.
- Lỗi HTTP dùng `application/problem+json`. `platform/web` đã có `ApiProblem`, `ErrorCode`, `ApplicationException`, `GlobalExceptionHandler` và các exception chung. Dùng mã lỗi ổn định; cập nhật contract/test nếu thêm mã mới.
- Domain exception diễn đạt lỗi nghiệp vụ mà không phụ thuộc `platform/web` hoặc HTTP. Ánh xạ lỗi sang HTTP ở ranh giới application/API; không để controller tự tạo response lỗi tùy tiện.
- Chỉ dùng `safeDetail` cho nội dung được phép lộ cho client. Không đưa journal text, email, token, SQL, ciphertext, secret, stack trace hoặc raw provider response vào error response/log.
- User flow truy cập tài nguyên cá nhân phải lọc theo `resourceId + currentUserId`. Không dùng query chỉ theo ID rồi kiểm tra ownership sau. Với tài nguyên không thuộc user, tuân theo contract not found/forbidden đã chốt, tránh tiết lộ sự tồn tại của dữ liệu.
- Resource/job creation cần idempotency khi contract yêu cầu; cập nhật có version/`If-Match`, xung đột trả `409`. Phân trang bằng cursor cho journal/audit/job.
- Không áp một wrapper `ApiResponse<T>` cho mọi success response nếu chưa có contract chung được quyết định; error contract hiện có là phần dùng chung.

## 6. Privacy, security và safety

- Security mặc định deny; endpoint mới phải có authorization rõ ràng và test cả allow lẫn deny. RBAC không thay thế owner check. Admin thông thường không đọc hoặc giải mã journal thô.
- Mã hóa journal, email, profile, tag, AI narrative, goal/habit text theo encryption matrix. Email/tag equality lookup dùng HMAC có khóa, không dùng hash trần. Structured metrics có thể plaintext để aggregate nhưng luôn owner-scoped.
- Không log request/response body mặc định; không log journal, prompt chứa nội dung user, token/password/key, raw AI request/response, email, location, signed URL. Audit chỉ chứa metadata đã allowlist.
- Safety đầu vào: validate → curated rules → classifier → versioned policy. `HIGH/CRITICAL` dùng safety response đã duyệt và không enqueue reflection/recommendation thường. Classifier lỗi/timeout thì vẫn lưu journal nhưng chặn generative response cho đến khi screening thành công.
- Output AI cũng phải qua validation/safety. LLM không được tự tạo hotline hoặc nguồn hỗ trợ khẩn cấp. Test safety bằng synthetic Vietnamese/English cases, không dùng nhật ký thật.
- Kiểm tra consent tại thời điểm AI job thực thi, không chỉ khi enqueue. Provider không đáp ứng chính sách dữ liệu thì không gửi journal content.

## 7. Database, async và kết quả dẫn xuất

- PostgreSQL/Flyway là nguồn schema thực thi. Migration đã merge/deploy là immutable; thay đổi bằng migration mới. Dùng Testcontainers/PostgreSQL thật cho query, constraint và migration quan trọng, không thay bằng H2.
- ID nghiệp vụ là UUID, application ưu tiên UUIDv7; timestamp lưu UTC, ngày theo người dùng có `local_date` và timezone snapshot. User-owned table/query cần `user_id` và index phù hợp. Constraint/range/unique business rule phải có ở database khi áp dụng được.
- Journal được lưu độc lập với AI. Journal + safety event tối thiểu + outbox event được commit cùng transaction; outbox/job payload chỉ có ID/version, không có raw content. Không gọi provider trong DB transaction.
- Worker claim bằng lease/lock có recovery, handler idempotent, retry lỗi tạm thời với backoff; lỗi policy/schema vĩnh viễn không retry vô hạn. Kết quả phân tích chỉ active nếu `content_version` còn khớp.
- Dashboard không gọi LLM trên request path. Insight/report dựa trên structured metrics, sample size, evidence và version; LLM chỉ diễn giải dữ liệu đã tính. Không diễn đạt correlation thành causation.
- Daily check-in là điểm dữ liệu chính của ngày; journal observation mới nhất chỉ là fallback có đánh dấu nguồn, không đếm trùng.
- Xóa tài khoản và export cần job idempotent, retention/expiry, cleanup object/provider/cache và audit tối thiểu theo tài liệu.

## 8. Quy trình làm một feature

1. Xác định acceptance criteria, ownership/permission, ảnh hưởng privacy/safety và failure mode.
2. Chốt API request/response/error rồi domain rule; thêm migration/constraint/index nếu cần.
3. Triển khai repository adapter, application use case, controller/security; cập nhật OpenAPI.
4. Viết test có ý nghĩa: domain/unit, controller contract và authorization, PostgreSQL integration cho SQL/migration, regression cho lỗi privacy/safety. Fixture chỉ dùng dữ liệu synthetic.
5. Thêm observability/audit phù hợp, cập nhật tài liệu và frontend contract nếu có thay đổi.
6. Chỉ đánh dấu task `DONE` khi code, migration, API, test và tài liệu liên quan đã hoàn tất. Không sửa trực tiếp migration đã merge.

Lệnh thường dùng (chạy trong `backend/`, PowerShell): `./mvnw.cmd test`, `./mvnw.cmd verify`, `./scripts/check-migrations.ps1`. Integration test cần Docker/Testcontainers. Chạy thêm kiểm thử chuyên biệt theo phạm vi thay đổi; báo rõ test nào không chạy được và lý do.

## 9. Khi kết thúc một phiên làm việc

- Nói rõ đã thay đổi gì, vì sao, đã kiểm tra bằng cách nào và giới hạn còn lại.
- Nếu tài liệu kế hoạch hoặc sơ đồ kiến trúc lệch với code sau thay đổi, cập nhật phần liên quan. Không ghi một milestone đã xong khi thiếu acceptance criteria hoặc test bắt buộc.
- Không commit `.env`, private key, credential, dữ liệu người dùng thật hoặc raw provider payload. Không in nội dung secret khi điều tra lỗi.
