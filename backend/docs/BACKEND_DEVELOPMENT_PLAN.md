# Kế hoạch phát triển backend mylog

> Development roadmap v1  
> Phạm vi: MVP backend có thể demo, kiểm thử và triển khai  
> Kiến trúc: modular monolith, Java 21, Spring Boot 4.1.x, PostgreSQL/pgvector, Redis

Tài liệu liên quan:

- [Kiến trúc backend](BACKEND_ARCHITECTURE.md)
- [Thiết kế database](DATABASE_OVERVIEW.md)
- [Database DBML](database/mylog.dbml)

## 1. Mục tiêu của kế hoạch

Kế hoạch này biến blueprint kiến trúc thành các increment có thể chạy và kiểm thử. Thứ tự ưu tiên:

1. Privacy và safety trước AI personalization.
2. Journal hoạt động độc lập với AI.
3. Mỗi milestone tạo ra một vertical slice có thể demo.
4. Database migration, API contract, code và test được hoàn thành cùng feature.
5. Không triển khai microservice hoặc hạ tầng phức tạp khi chưa có nhu cầu đo được.

## 2. Giả định và cách dùng

Ước lượng tham khảo giả định hai backend developer part-time/full-time tương đương và có frontend phối hợp. Nếu chỉ có một developer, giữ nguyên thứ tự dependency và tăng thời gian khoảng 1.5–2 lần.

| Ký hiệu | Ý nghĩa |
|---|---|
| P0 | Bắt buộc cho MVP an toàn |
| P1 | Cần cho bản demo hoàn chỉnh |
| P2 | Có thể chuyển sau MVP |
| S | Khoảng 0.5–1 ngày |
| M | Khoảng 1–3 ngày |
| L | Khoảng 3–5 ngày |
| XL | Phải tách nhỏ trước khi bắt đầu |

Ước lượng không bao gồm thời gian chờ duyệt nội dung wellness/safety từ người có chuyên môn.

## 3. Trạng thái hiện tại

### Đã hoàn thành

- [x] Bootstrap Spring Boot 4.1.1 và Java 21.
- [x] Maven Wrapper, Dockerfile và build JAR.
- [x] PostgreSQL/pgvector + Redis qua Docker Compose.
- [x] Spring Boot Externalized Configuration bằng `.env`.
- [x] Profile `local`, `test`, `prod`.
- [x] Flyway V1 bật `vector` và `pgcrypto`.
- [x] Security fail-closed: chỉ health endpoint public.
- [x] CORS cấu hình bằng biến môi trường.
- [x] Package/module skeleton.
- [x] ArchUnit rule nền tảng.
- [x] Testcontainers configuration.
- [x] Backend architecture và database blueprint.
- [x] M1 backend: identity/profile/consent API, V2–V4 migrations và PostgreSQL integration tests.

### Chưa triển khai

- [ ] Entity/repository nghiệp vụ ngoài identity/user.
- [ ] Journal/check-in API.
- [ ] Mã hóa journal cấp application.
- [ ] Safety engine.
- [ ] Outbox/job worker.
- [ ] AI provider/RAG.
- [ ] Dashboard/insight/report.
- [ ] Admin/export/account deletion.

## 4. Milestone tổng quan

| Milestone | Thời lượng tham khảo | Kết quả demo được | Phụ thuộc |
|---|---:|---|---|
| M0 — Foundation closure | 1 tuần | CI, error contract, observability và quyết định nền | hiện trạng |
| M1 — Identity & profile | 2 tuần | đăng ký, đăng nhập, refresh, profile, session | M0 |
| M2 — Journal & check-in | 2 tuần | CRUD journal mã hóa, tag, check-in, safety đầu vào | M1 |
| M3 — Async AI analysis | 2 tuần | outbox worker, emotion/topic/reflection, retry | M2 |
| M4 — Dashboard & reports | 2 tuần | timeline, trend, insight evidence, weekly report | M3 |
| M5 — Self-care | 1 tuần | goal, habit, completion, streak | M2 |
| M6 — Knowledge/RAG & admin | 2 tuần | review knowledge, embedding, safe recommendation, admin ops | M3 |
| M7 — Data rights | 1–2 tuần | CSV/PDF export, feedback, account deletion | M1–M6 |
| M8 — Hardening & release | 2 tuần | security/performance/safety test, staging release | tất cả P0/P1 |

Tổng tham khảo: 14–16 tuần với team nhỏ. M5 có thể chạy song song M4 nếu có người thứ hai; các milestone khác nên giữ dependency.

## 5. Dependency map

```text
M0 Foundation
   ↓
M1 Identity/Profile
   ↓
M2 Journal/Check-in/Safety ingress
   ├──────────────► M5 Self-care
   ↓
M3 Outbox/AI Analysis
   ├──────────────► M4 Insight/Dashboard/Report
   └──────────────► M6 Knowledge/RAG/Admin
                         ↓
                  M7 Export/Delete/Feedback
                         ↓
                  M8 Hardening/Release
```

## 6. M0 — Foundation closure

### Mục tiêu

Biến project skeleton thành nền tảng mà các module có thể phát triển nhất quán.

### Công việc

#### FND-001 — Chốt ADR nền tảng (P0, M)

- [x] ADR authentication: local credential + JWT hay external IdP.
- [x] ADR encryption: local key provider và production KMS contract.
- [x] ADR safety classifier/rule ownership.
- [x] ADR AI provider và data retention.
- [x] ADR object storage.

Không cần chốt model AI cuối cùng để làm journal; cần chốt provider interface và data handling trước M3.

#### FND-002 — Error contract (P0, M)

- [x] `ApiProblem` theo `application/problem+json`.
- [x] `GlobalExceptionHandler`.
- [x] Stable error codes: validation, unauthorized, forbidden, not found, conflict, rate limited, dependency unavailable.
- [x] Không trả stack trace, SQL hoặc provider response.
- [x] Controller test cho từng nhóm lỗi.

#### FND-003 — Request context và logging (P0, M)

- [x] Nhận hoặc tạo `X-Request-Id`.
- [x] Đưa request ID/trace ID vào MDC.
- [x] JSON logging cho staging/prod.
- [x] Redaction test cho Authorization, cookie, email và journal fields.
- [x] Không log request/response body mặc định.

#### FND-004 — Clock, UUID và current user ports (P0, S)

- [x] Inject `Clock` thay vì gọi thời gian trực tiếp.
- [x] `IdGenerator` sinh UUIDv7.
- [x] `CurrentUser` abstraction, chưa phụ thuộc controller.
- [x] Fixed test implementations.

#### FND-005 — CI baseline (P0, M)

- [x] Compile và unit/architecture test trên mỗi pull request.
- [x] Integration test khi Docker/Testcontainers khả dụng.
- [x] Dependency vulnerability scan.
- [x] Kiểm tra secret và migration naming.
- [x] Build container nhưng chưa push production registry.

#### FND-006 — OpenAPI baseline (P1, S)

- [x] Cấu hình API title/version/server.
- [x] Khai báo bearer authentication scheme.
- [x] Hide actuator/internal endpoints.
- [x] Export OpenAPI JSON làm contract artifact.

### Exit criteria M0

- Build sạch bằng Java 21.
- `mvn test` pass; integration test pass khi Docker bật.
- Error response và request ID có contract test.
- ADR P0 được merge.
- Không có secret trong Git history mới.

### Bằng chứng hoàn thành M0

- ADR đã được chốt tại `docs/adr/` với trạng thái `Accepted`.
- `mvn test` và `mvn verify` chạy contract, security, request-context, redaction, UUIDv7 và architecture tests.
- Integration test dùng PostgreSQL/pgvector và Redis Testcontainers; tự chạy khi Docker khả dụng và được CI bắt buộc bằng Docker daemon.
- CI kiểm tra Flyway naming, secret bằng Gitleaks, dependency bằng OWASP Dependency-Check, xuất OpenAPI artifact và build container.
- OpenAPI chỉ bật theo cấu hình; profile production tắt API docs/Swagger UI và dùng structured JSON logging.
- `.env` local được Git ignore; repository chỉ lưu `.env.example` không chứa secret thật.

## 7. M1 — Identity, RBAC và profile

### Database

- [x] `V2__identity_and_rbac.sql`.
- [x] `V3__user_profile_and_consent.sql`.
- [x] `V4__auth_refresh_history_id.sql`: khóa chính UUID cho lịch sử refresh, `token_hash` tiếp tục unique.
- [x] V2 seed role `USER` tối thiểu; admin role/permission mở rộng được bổ sung khi codes ổn định.
- [x] Repository integration test trên PostgreSQL thật.

### IDN-001 — User registration (P0, L)

- [x] Normalize email theo policy cố định.
- [x] HMAC email lookup + encrypted email.
- [x] Hash password bằng BCrypt cost 12; benchmark local ban đầu bên dưới.
- [x] Tạo user/profile/default USER role trong một transaction.
- [x] Idempotent email verification token.
- [x] Rate limit theo IP/email pseudonym.

API:

```text
POST /api/v1/auth/register
POST /api/v1/auth/email-verifications
POST /api/v1/auth/email-verifications:confirm
```

### IDN-002 — Login/token rotation (P0, L)

- [x] Access JWT sống ngắn.
- [x] Refresh token opaque, chỉ lưu hash.
- [x] Rotation và token-family reuse detection.
- [x] Lock/rate limit sau nhiều lần đăng nhập sai.
- [x] Revoke family khi phát hiện reuse.

API:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

### IDN-003 — Session management (P1, M)

- [x] Danh sách session không lộ raw user agent/IP đầy đủ.
- [x] Revoke một session hoặc tất cả session khác.
- [x] Audit action revoke nhạy cảm.

```text
GET    /api/v1/me/sessions
DELETE /api/v1/me/sessions/{sessionId}
DELETE /api/v1/me/sessions?exceptCurrent=true
```

### USR-001 — Profile/onboarding (P0, M)

- [x] Profile encrypted payload.
- [x] Validate IANA timezone và locale.
- [x] Optimistic locking.
- [x] Onboarding goals giới hạn ở các mã wellness; không dùng diagnosis/treatment.

```text
GET   /api/v1/me
PATCH /api/v1/me
POST  /api/v1/me/onboarding:complete
```

### USR-002 — Consent/privacy (P0, M)

- [x] Versioned TERMS/PRIVACY/AI_PROCESSING consent.
- [x] Optional analytics/model-training mặc định false.
- [x] Withdraw consent không xóa lịch sử quyết định.
- [ ] AI use case kiểm tra consent tại execution time, không chỉ enqueue time.

`UserProfileUseCase.isConsentGranted` đã có để M3 kiểm tra consent khi worker thực thi; chưa có AI worker trong M1 nên mục trên vẫn mở.

### Test bắt buộc

- Registration race với cùng email.
- Refresh token replay/reuse.
- Suspended/deletion-pending user không lấy token mới.
- User không đọc/sửa profile/session của user khác.
- Ciphertext thay đổi giữa hai lần encrypt cùng plaintext.
- Secret/token không xuất hiện trong log.

### Exit criteria M1

- Frontend có thể register/login/logout và đọc/sửa profile qua API thật.
- Token rotation/revoke hoạt động.
- Permission seed và authorization test pass.
- Không lưu plaintext email/token/password.

### Bằng chứng và giới hạn M1 backend

- Persistence M1 dùng JPA entity trong `identity`/`user` `infrastructure/persistence/entity`, adapter `EntityManager` trong `infrastructure/persistence`, với native SQL qua JPA cho rate-limit upsert và truy vấn consent mới nhất. Flyway V2–V4 vẫn là schema nguồn; Hibernate chỉ `validate`.
- Flyway V2–V4, register → email verification → login → profile/consent → refresh/reuse chạy qua PostgreSQL Testcontainers. Test có registration race, account pending/suspended, ownership session, ciphertext/tokens, log redaction và key rotation. OpenAPI artifact được xuất từ context bật M1, có các endpoint identity/profile.
- BCrypt cost 12 được chọn sau benchmark local ngày 2026-09-30 (Java 25 trên máy phát triển): trung bình khoảng 264 ms cho một cặp encode + verify; cost 10 khoảng 70 ms, cost 11 khoảng 131 ms. Cần đo lại trên hạ tầng triển khai trước release.
- Local email verification gửi đến Mailpit (`docker compose up -d mailpit`, UI cổng 8025). Production cần SMTP và khóa do secret manager cung cấp; không dùng fallback local.
- Frontend chưa tích hợp các API M1; điều kiện frontend ở exit criteria cần được xác nhận khi tích hợp.
- M1 dùng `audit_logs` sớm trong V2 để audit session revoke. Khi viết V6, chỉ bổ sung outbox/idempotency và phần audit còn thiếu, không tạo lại bảng này.

## 8. M2 — Journal, check-in và safety đầu vào

### Database

- [x] `V5__journal_and_checkin.sql`.
- [x] `V6__platform_outbox_idempotency_audit.sql` phần cần cho journal.
- [x] `V7__safety.sql`.
- [x] Constraint/range/index cho các bảng M2; đã validate trên PostgreSQL 17 và Supabase.

### JRN-001 — Encryption adapter (P0, L)

- [x] `JournalContentCipher` port.
- [x] AES-GCM adapter với nonce duy nhất và AAD.
- [x] Local key provider chỉ dùng development.
- [x] Key version trong mỗi payload.
- [x] Decrypt failure trả lỗi an toàn, không log ciphertext/key.
- [x] Test tampering, wrong owner/AAD và rotation path qua shared cipher và journal integration.

### JRN-002 — Create/read journal (P0, L)

- [x] Validate TipTap JSON allowlist và giới hạn size/depth.
- [x] Server tự sinh plain text đã sanitize.
- [x] Validate mood/stress/energy/sleep theo range và precision.
- [x] Lưu `occurred_at`, timezone snapshot và `local_date`.
- [x] Idempotency-Key cho create.
- [x] Ownership query bắt buộc `entryId + currentUserId`.

```text
POST /api/v1/journal-entries
GET  /api/v1/journal-entries/{entryId}
GET  /api/v1/journal-entries?cursor=&from=&to=&tag=&favorite=
```

### JRN-003 — Update/delete/favorite (P0, M)

- [x] `If-Match` hoặc row version chống lost update.
- [x] Tăng content version khi update nội dung hoặc metadata ảnh hưởng analysis.
- [x] Soft delete + purge sau 30 ngày; chặn purge nếu còn asset chưa xóa.
- [x] Favorite endpoints idempotent.
- [x] Entry đã xóa không xuất hiện trong list journal.

### JRN-004 — Tags/assets (P1, L)

- [x] Tag encrypted + HMAC lookup theo user.
- [x] Giới hạn 20 tag/entry và tên 40 code point.
- [ ] Asset upload dùng Cloudinary signed workflow, delivery type private/authenticated.
- [ ] MIME/size/checksum/malware state.
- [ ] Không chấp nhận remote URL tùy ý làm storage source.

Asset có thể chuyển P2 nếu demo MVP không cần upload ảnh thật.

### CHK-001 — Daily check-in (P0, M)

- [x] Upsert một check-in/user/local date.
- [x] Validate timezone và metric range.
- [x] Activity structured codes.
- [ ] Quy tắc dashboard ưu tiên daily check-in, journal observation chỉ fallback.

### SAF-001 — Rule-based screening (P0, L)

- [ ] Versioned curated rules cho tiếng Việt/Anh.
- [ ] `NORMAL/LOW/MODERATE/HIGH/CRITICAL`.
- [x] Fail-safe nếu classifier unavailable.
- [ ] Safety response/resource duyệt trước theo locale.
- [x] Chỉ lưu event tối thiểu, không lưu matched raw text.
- [x] HIGH/CRITICAL không phát event analysis thông thường.

### SAF-002 — Safety regression corpus (P0, L)

- [x] Synthetic cases: trực tiếp, phủ định, trích dẫn, tiếng lóng, mỉa mai.
- [ ] Theo dõi false positive/false negative trên bộ nhãn được duyệt.
- [x] Không commit dữ liệu người dùng thật.
- [ ] Báo cáo eval có rule/classifier version và ngưỡng chấp nhận.

### Exit criteria M2

- Frontend thay `localStorage` bằng API cho journal/check-in cơ bản.
- Journal vẫn lưu được khi dependency AI không tồn tại.
- Nội dung mã hóa trong DB; repository/admin không trả plaintext ngoài user flow.
- Test horizontal authorization pass cho mọi journal endpoint.
- High-risk case đi đúng safety flow.

### Ghi nhận triển khai M2 ngày 2026-09-30

- Backend đã có journal CRUD/list/favorite/tag, daily check-in, encryption, idempotency, outbox và safety event trong cùng transaction. Flyway V5–V7 đã áp dụng lên Supabase PostgreSQL (schema version 7); ứng dụng khởi động và Hibernate validate thành công với schema này. `mvn verify` qua PostgreSQL Testcontainers.
- Classifier mặc định trả `unavailable`: journal vẫn lưu; ordinary analysis bị chặn và tạo `SafetyRescreenRequested`. Kể cả khi cắm classifier, policy gate chỉ cho ordinary analysis nếu có policy `APPROVED` đang hiệu lực và khớp rule/provider/version/confidence. Rule tiếng Việt/Anh hiện là **draft**, không được coi là policy đã duyệt. Corpus synthetic ghi nhận false positive dự kiến ở câu phủ định/trích dẫn; chưa có đánh giá false negative đáng tin cậy. API safety resources công khai chỉ trả nguồn đã duyệt, xác minh và có nguồn HTTPS; safety modal gọi API nhưng chưa có hotline hoặc nguồn hỗ trợ thật nào được duyệt trong DB.
- Frontend hiện còn auth mock và journal trong `localStorage`; chưa đạt exit criteria tích hợp API. Frontend safety modal đã bỏ số điện thoại chưa xác minh. Cần nối auth M1 trước khi thay luồng journal/check-in bằng API có token, rồi kiểm tra high-risk end-to-end.
- Asset upload Cloudinary signed workflow vẫn mở và có thể chuyển P2 theo quy định ở trên. Dashboard ưu tiên check-in là quy tắc cho M4, chưa có dashboard query thực tế để xác nhận.

## 9. M3 — Outbox, jobs và AI analysis

### Database

- [x] Hoàn tất outbox/job indexes từ V6 (V8 bổ sung lease recovery và job claim).
- [x] `V8__ai_analysis_and_jobs.sql` (đã kiểm thử PostgreSQL Testcontainers và áp dụng lên Supabase; ứng dụng khởi động, Hibernate validate thành công).
- [x] Unique/idempotency constraints cho analysis version.

### JOB-001 — Transactional outbox (P0, L)

- [x] Publish event cùng transaction nghiệp vụ.
- [x] Claim bằng `FOR UPDATE SKIP LOCKED`.
- [x] Lease/lock timeout và worker recovery.
- [x] Exponential backoff + jitter.
- [x] Dead job và sanitized error code.
- [x] Metrics queue depth/oldest age/success/error.

### AI-001 — Provider ports/adapters (P0, L)

- [x] `JournalAnalyzer`, `EmbeddingProvider`, `KnowledgeRetriever` ports.
- [x] Fake deterministic `JournalAnalyzer` cho local/test, bật rõ bằng `MYLOG_AI_FAKE_ENABLED=true`.
- [ ] Adapter provider thật có timeout, retry boundary và circuit breaker.
- [x] Không retry lỗi policy/schema vĩnh viễn.
- [x] Không log raw prompt/response.

### AI-002 — Structured analysis (P0, L)

- [x] JSON schema cho sentiment/emotions/topics/reflection.
- [x] Validate enum/range/size.
- [ ] Output safety validation.
- [x] Encrypt reflection; structured emotion/topic lưu riêng. V8 chưa nhận extracted entities.
- [x] Lưu provider/model/prompt/policy/content version và usage.

### AI-003 — Analysis lifecycle (P0, M)

- [x] `PENDING → ANALYZING → ANALYZED/ANALYSIS_FAILED`.
- [x] Edit journal tạo `ANALYSIS_OUTDATED`.
- [x] Kết quả job cũ thành `STALE`, không activate.
- [x] Retry endpoint idempotent và rate-limited.

```text
POST /api/v1/journal-entries/{entryId}/analysis:retry
GET  /api/v1/journal-entries/{entryId}
GET  /api/v1/journal-entries/{entryId}/analysis
```

### AI-004 — Frontend status integration (P1, M)

- [ ] Polling có exponential backoff.
- [ ] Response phân biệt pending/failed/blocked.
- [ ] Không hiển thị spinner vô hạn.
- [ ] P2: SSE notification khi analysis hoàn tất.

### Exit criteria M3

Backend đã được kiểm thử với PostgreSQL Testcontainers cho fake provider, owner-scope,
consent lúc worker chạy, retry, lease recovery và kết quả cũ; `JournalEntryChanged`/`JournalEntryDeleted`
đánh dấu kết quả cũ `STALE` qua outbox. Fake chỉ dùng local/test. M3 chưa đạt exit criteria
production: classifier/policy/safety content của M2 chưa được duyệt, provider thật và điều khoản
dữ liệu chưa chốt, output safety validator mới là baseline, frontend vẫn dùng mock.
`SafetyRescreenRequested` được retry có backoff khi classifier/policy chưa sẵn sàng,
sau đó rescreen và enqueue analysis khi đủ điều kiện. `MYLOG_JOBS_ENABLED` mặc định false;
bật worker khi môi trường đã được cấu hình và policy/provider phù hợp.

- Demo end-to-end journal → safety → async analysis → reflection.
- Tắt AI provider không làm mất journal/outbox event.
- Worker chạy song song không tạo duplicate active analysis.
- Token/cost/latency metrics có nhưng không chứa user content.

## 10. M4 — Insight, dashboard và report

### Database

- [x] `V9__insights_and_reports.sql` (đã áp dụng Supabase schema v9, Hibernate validate thành công).
- [x] Evidence/index theo user và period.

### INS-001 — Daily aggregate (P0, L)

- [x] Mood/stress/energy/sleep timeline theo timezone snapshot của user.
- [x] Top curated emotions/topics từ active analysis.
- [x] Journal streak với định nghĩa được test (ngày có journal SAVED; check-in đơn lẻ không tính; streak hiện tại có thể kết thúc hôm qua).
- [x] Daily check-in ưu tiên, journal fallback có source marker.
- [x] Dashboard 30 ngày trên 10.000 journal synthetic/user: local PostgreSQL Testcontainers p95 45 ms cho 30 lần gọi service sau warmup (môi trường staging vẫn cần đo lại).

### INS-002 — Evidence-backed insight (P0, L)

- [x] Minimum sample size cấu hình được (`MYLOG_INSIGHTS_MINIMUM_SAMPLES`, mặc định 7).
- [x] Correlation không được mô tả như causation.
- [x] Lưu algorithm version, sample size, strength và evidence.
- [x] Không copy raw journal vào evidence.
- [x] Narrative RULE/STATISTICAL chỉ diễn giải metric đã tính; không gọi LLM.

### RPT-001 — Weekly/monthly report (P1, L)

- [x] Scheduler theo timezone user.
- [x] Unique key theo user/type/period/version; enqueue version 1 idempotent.
- [x] Metrics snapshot bất biến.
- [ ] AI narrative qua output safety.
- [x] Regenerate tạo version mới.

```text
GET /api/v1/dashboard?range=7d|30d|90d
GET /api/v1/insights?from=&to=&cursor=
GET /api/v1/reports?type=WEEKLY&cursor=
GET /api/v1/reports/{reportId}
```

### Performance target ban đầu

- Dashboard 30 ngày p95 dưới 500 ms trên 10.000 entries/user synthetic.
- Journal list p95 dưới 300 ms, page size 20–50.
- Report generation async; HTTP enqueue dưới 300 ms.

Các số trên là engineering target ban đầu, phải đo lại trên môi trường staging.

### Exit criteria M4

Backend M4 đã có dashboard/insight/report API owner-scoped, scheduler và report worker có lease/retry.
PostgreSQL Testcontainers kiểm tra check-in ưu tiên journal fallback, top curated code, sample/evidence,
pagination, scheduler idempotent, regenerate và snapshot cũ bất biến. DST/calendar boundary có unit test.
V9 đã áp dụng Supabase. Chưa đạt toàn bộ M4: p95 trên staging, journal list p95 và HTTP report
enqueue chưa được đo; AI narrative qua output safety còn phụ thuộc provider và policy M3.
Hiện report chỉ dùng narrative RULE từ structured metrics. `MYLOG_REPORTS_ENABLED` mặc định false,
cần bật rõ khi muốn chạy scheduler/worker.

- Dashboard không gọi LLM trong request path.
- Mọi insight hiển thị được evidence/sample size.
- Weekly report chạy idempotent qua scheduler.
- Timezone boundary và DST có test.

## 11. M5 — Self-care

### Database/API

- [x] `V10__selfcare.sql`.
- [x] Goal/habit text encrypted.
- [x] Completion unique theo habit/local date.
- [x] Ownership và optimistic locking cho goal update.

```text
POST   /api/v1/self-care/goals
GET    /api/v1/self-care/goals
PATCH  /api/v1/self-care/goals/{goalId}
POST   /api/v1/self-care/goals/{goalId}/habits
PUT    /api/v1/self-care/habits/{habitId}/completions/{localDate}
DELETE /api/v1/self-care/habits/{habitId}/completions/{localDate}
```

### Business rules

- [ ] Goal nằm trong wellness scope, không treatment plan: đã có category allowlist và bộ lọc từ khóa cơ bản; cần policy/nội dung được duyệt và đánh giá các cách diễn đạt khác trước khi coi là bảo đảm đầy đủ.
- [x] Completion idempotent.
- [x] Streak tính theo timezone snapshot của habit và frequency config DAILY/WEEKLY.
- [x] Liên hệ habit–mood chỉ xuất hiện khi mỗi nhóm có ít nhất 7 check-in có mood, trong cửa sổ 90 ngày.

### Exit criteria M5

Backend M5 có API để tạo goal/habit, đánh dấu và bỏ completion, xem progress trong `GET /goals`.
V10 đã áp dụng lên Supabase và Hibernate `ddl-auto=validate` khởi động thành công (2026-10-01).
Frontend hiện chưa tích hợp các API này; vì vậy exit criterion end-to-end vẫn mở.
Completion dùng `UNIQUE(habit_id, local_date)` và `PUT` upsert; gọi lặp không tăng số lượng.
Goal/habit của user khác trả 404 qua truy vấn theo `user_id` và ID.
M5 dùng timezone snapshot khi tạo habit; đổi timezone profile sau đó không đổi lịch của habit đã tạo.
Chưa có API cập nhật habit, chỉ goal update dùng `If-Match` và version.

## 12. M6 — Knowledge base, RAG và admin

### Database

- [x] `V11__knowledge_and_prompts.sql`.
- [x] `V12__admin_roles_permissions.sql` cho content/safety/system/support/auditor. Đổi số từ V13 dự kiến để Flyway không chạy vượt V12 của M7.
- [x] `V13__knowledge_author_nullification.sql` để account deletion sau này có thể null attribution FK mà giữ approved content bất biến.

### KB-001 — Knowledge workflow (P1, L)

- [x] Draft → review → approve/reject/archive.
- [x] Version và chunk bất biến sau approve bằng DB trigger; archive giữ citation lịch sử.
- [x] Checksum tránh chunk lại nội dung không đổi. Embedding chưa bật khi chưa chốt provider/model.
- [x] Chỉ approver có `knowledge:review` được publish; không tự duyệt bản do mình tạo.
- [x] Audit approve/archive bằng reason code, không ghi content.

### KB-002 — Chunk/embedding/retrieval (P1, L)

- [x] Chunk strategy có version (`paragraph-800-v1`).
- [ ] Embedding model/dimension provenance: schema đã có, chưa có provider adapter đã duyệt để tạo vector.
- [ ] Partial vector index sau khi chốt model.
- [x] Filter APPROVED + locale + effective date trước retrieval.
- [x] Citation tới knowledge version/chunk.
- [ ] Eval retrieval bằng curated queries.

### RAG-001 — Safe recommendation (P1, L)

- [x] Context minimization: endpoint chỉ nhận topic code, không gửi journal ra ngoài.
- [x] Endpoint hiện chỉ trả approved excerpts.
- [x] Structured excerpt response kèm citation.
- [ ] Safety validation sau generation.
- [x] Không tự tạo hotline/nguồn hỗ trợ vì endpoint hiện không sinh nội dung.

### ADM-001 — Admin foundation (P0/P1, L)

- [x] Separate admin controllers/permissions.
- [x] User metadata view không có decrypt journal/email.
- [x] Suspend/restore account có reason code + audit; suspend revoke sessions.
- [x] Job list/retry chỉ hiển thị error code, không payload/summary; retry chỉ `DEAD/PROVIDER_UNAVAILABLE`.
- [x] Aggregate dashboard có minimum cohort size 20 cho từng metric.

```text
GET  /api/v1/admin/users
GET  /api/v1/admin/users/{userId}/metadata
POST /api/v1/admin/users/{userId}:suspend
POST /api/v1/admin/users/{userId}:restore
GET  /api/v1/admin/ai-jobs
POST /api/v1/admin/ai-jobs/{jobId}:retry
```

### Exit criteria M6

Backend M6 đã có workflow knowledge, retrieval approved theo topic/locale/effective date,
metadata admin, suspend/restore, sanitized job list/retry và aggregate dashboard.
V11–V13 đã áp dụng lên Supabase ngày 2026-10-01; Hibernate schema validation khởi động thành công.
Test PostgreSQL xác nhận nội dung draft/review/archive không được retrieve, citation đúng version/chunk,
DB chặn sửa version đã approve và permission matrix có cả deny/allow.
Chưa có embedding provider/model được duyệt, vector index, curated retrieval eval hoặc generation/output safety validation;
recommendation hiện chỉ trả nguyên văn approved excerpts, chưa phải RAG sinh nội dung.

## 13. M7 — Data rights, export và support

### Database

- [x] `V14__exports_deletion_feedback.sql` (đã kiểm tra bằng Flyway/Testcontainers và áp dụng lên Supabase ngày 2026-10-01; migration bất biến).
- [x] Partial unique cho active deletion/export theo policy.
- [x] `V15__restrict_direct_database_api_access.sql`: khóa grant Supabase client roles và bật RLS deny-all cho bảng `public`; đã kiểm chứng Testcontainers và 43 bảng trên Supabase ngày 2026-10-01.

### EXP-001 — Export (P0, L)

- [x] CSV machine-readable và PDF user-readable.
- [x] Export tạo async job.
- [x] File private/encrypted, signed URL ngắn hạn.
- [x] Expiry cleanup artifact + metadata.
- [x] Re-authentication trước download nếu policy yêu cầu.

### DEL-001 — Account deletion (P0, L)

- [x] Re-authenticate trước request.
- [x] Grace period/cancel.
- [x] Revoke session khi bắt đầu deletion.
- [x] Idempotent checkpoint worker.
- [ ] Xóa DB, Cloudinary assets, cache và provider artifacts.
- [x] Audit tối thiểu/pseudonymous theo retention.
- [x] Test xác nhận không còn data user-owned đã triển khai.

### FBK-001 — Feedback (P1, M)

- [x] Message encrypted.
- [x] Không auto-attach journal.
- [x] Workflow status/assignment.
- [x] Retention và audit truy cập.

### Exit criteria M7

Backend M7 đã có API/worker cho CSV/PDF, xóa tài khoản và feedback. V14–V15 đã chạy qua Flyway trên PostgreSQL Testcontainers và áp dụng lên Supabase ngày 2026-10-01. V15 khóa truy cập trực tiếp qua Supabase Data API; kiểm tra thực tế 43 bảng ứng dụng đều bật RLS và `anon`/`authenticated` không có SELECT. Export hiện giới hạn 5 MB và gồm account/profile/consent/session metadata, journal kể cả soft-deleted, tag/link, check-in, analysis, insight, report, self-care kể cả giá trị habit completion và feedback qua application facades. Artifact mã hóa trong PostgreSQL theo ADR-0007, hết hạn sau 24 giờ; metadata request được dọn sau 30 ngày. Yêu cầu xóa có grace period 7 ngày, thu hồi session ngay, hủy bằng xác thực lại và worker retry/checkpoint; test mô phỏng lỗi cleanup rồi chạy lại. Deletion audit pseudonymous giữ tối đa 365 ngày, feedback giữ tối đa 180 ngày. Hiện chưa có adapter Redis cache hay AI provider artifact lưu dữ liệu user cần purge; khi thêm adapter mới phải nối cleanup tương ứng trước khi đánh dấu toàn bộ DEL-001 hoàn tất. Chưa kiểm chứng Cloudinary destroy bằng tài khoản thử nghiệm thực tế. Export/deletion worker bật mặc định khi identity được bật; có thể tắt bằng `MYLOG_EXPORTS_ENABLED=false` hoặc `MYLOG_DELETION_ENABLED=false` khi bảo trì.

- User tải được dữ liệu của chính mình.
- Signed URL hết hạn và không public object.
- Deletion job chạy lại an toàn sau lỗi giữa chừng.
- Không còn session hoạt động sau deletion processing.

## 14. M8 — Hardening và release

### Security (P0)

- [x] Threat model cho auth, journal, admin, AI provider và Cloudinary (`M8_THREAT_MODEL.md`; release blockers được ghi rõ).
- [ ] Dependency/container scan không còn critical unresolved.
- [ ] Authorization regression toàn endpoint.
- [x] Rate limit login, journal, analysis retry, export (journal/tag 60 writes/15 phút/user; export 3 requests/ngày/user; DB-backed HMAC subject).
- [ ] Secret rotation drill.
- [x] Encryption key rotation test (`SensitiveDataCipherTest` đọc payload khóa cũ bằng keyring mới; `IdentityJwtTest` xác minh JWT bằng public key cũ).
- [x] CORS và security headers review (exact configured origins; nosniff, DENY frame, no-referrer; cần xác nhận origin HTTPS thật ở staging).
- [ ] Admin MFA hoặc ghi rõ giới hạn nếu demo local.

### Privacy/safety (P0)

- [ ] Log/trace/metric scan không có sensitive text.
- [ ] Provider retention/opt-out được xác nhận.
- [ ] Safety eval đạt threshold đã chốt.
- [ ] Crisis resources được duyệt và có verified timestamp.
- [x] Consent withdrawal chặn future AI processing (PostgreSQL integration test thu hồi sau enqueue, trước worker).
- [ ] Backup retention phù hợp deletion policy.

### Reliability/performance (P0/P1)

- [ ] Load test journal CRUD/dashboard/job claim.
- [ ] Worker crash/reclaim test.
- [ ] Provider timeout/circuit breaker test.
- [ ] PostgreSQL backup + restore drill.
- [ ] Redis unavailable không làm mất source-of-truth data.
- [ ] Query plan/index review trên dataset representative.

### Deployment (P1)

- [ ] Staging dùng synthetic data.
- [ ] API/worker chạy profile riêng (`MYLOG_APP_PROFILE=api|worker` đã có trong code; staging deployment chưa kiểm chứng).
- [ ] Readiness kiểm tra dependency cần thiết; liveness không phụ thuộc provider ngoài (đã cấu hình DB ở staging/prod, chưa kiểm chứng deployment).
- [ ] Flyway chạy một lần có kiểm soát trước rollout app (`migrate` profile đã chạy trên PostgreSQL tách biệt, exit 0/V15; chưa chạy staging).
- [x] Runbook rollback application không rollback destructive migration (`M8_RELEASE_RUNBOOK.md`; chưa diễn tập).
- [ ] Dashboard/alert cho HTTP, DB, queue, AI và safety dependency.

### Exit criteria M8

M8 chưa đạt exit criteria release. Đã bổ sung quota ghi journal/export, tách HTTP worker và lịch xử lý trên API instance, security headers, readiness DB ở staging/prod, migration-only profile, threat model và runbook. Migration-only profile đã áp dụng V1–V15 và thoát mã 0 trên PostgreSQL/pgvector 17 tách biệt; chưa chạy staging. Các đánh giá security, safety, restore, performance và phê duyệt bên ngoài còn mở; không phát hành production dựa trên checklist này.

- Demo script end-to-end chạy ổn định trên staging.
- Không còn P0 bug mở.
- Restore backup thành công.
- Privacy/safety checklist được sign-off.
- Runbook xử lý AI/provider/job/deletion incident sẵn sàng.

## 15. API delivery order cho frontend

| Contract batch | Endpoint chính | Frontend có thể bỏ mock |
|---|---|---|
| API-1 | auth + `/me` | login/register/profile |
| API-2 | journal CRUD + check-in | JournalContext/localStorage |
| API-3 | analysis status/result | AI reflection drawer |
| API-4 | dashboard + insight | dashboard/insight mock data |
| API-5 | self-care | goal mock data |
| API-6 | report/export/delete | settings/report flows |

Mỗi batch thực hiện:

1. Chốt request/response/error examples.
2. Cập nhật OpenAPI.
3. Sinh/viết TypeScript client types.
4. Contract test backend.
5. Frontend tích hợp trên staging/local.
6. Chỉ xóa mock sau khi error/loading/empty state hoàn chỉnh.

## 16. Chiến lược test xuyên suốt

| Loại test | Chạy khi nào | Mục tiêu |
|---|---|---|
| Unit/domain | mỗi commit | rule/invariant nhanh, deterministic |
| Controller slice | mỗi PR | validation, auth, error contract |
| Repository integration | mỗi PR có Docker | SQL/Flyway/PostgreSQL thật |
| Architecture | mỗi PR | giữ module boundary |
| Provider contract | mỗi PR với fake; định kỳ với sandbox | schema/timeout/error mapping |
| Safety regression | mỗi thay rule/model/prompt | không giảm chất lượng âm thầm |
| End-to-end | staging/nightly | luồng người dùng hoàn chỉnh |
| Load/resilience | trước release | capacity và failure behavior |

Test data chỉ dùng synthetic fixtures. Không copy journal production vào local/staging/test.

## 17. Quy tắc triển khai từng feature

Thứ tự thực hiện một feature:

```text
Problem/acceptance criteria
  → API contract
  → domain rule
  → migration/constraint/index
  → repository adapter
  → application use case
  → controller/security
  → tests
  → observability
  → documentation
```

Không nhất thiết mỗi feature đều có mọi layer. Query đơn giản có thể dùng projection qua query port; business rule không được đặt trong controller hoặc JPA callback.

## 18. Definition of Ready

Task sẵn sàng phát triển khi:

- [ ] User story và acceptance criteria rõ.
- [ ] Privacy/safety impact đã phân loại.
- [ ] API request/response/error dự kiến rõ.
- [ ] Ownership/permission rõ.
- [ ] Dữ liệu mới và retention rõ.
- [ ] Dependency/blocker được xử lý.
- [ ] Task không còn kích thước XL.

## 19. Definition of Done

- [ ] Code tuân thủ module boundary.
- [ ] Migration và database constraint đầy đủ.
- [ ] Authorization/ownership được test cả allow và deny.
- [ ] Unit/integration/contract test pass.
- [ ] Không log dữ liệu nhạy cảm.
- [ ] OpenAPI và tài liệu cập nhật.
- [ ] Metrics/audit phù hợp được thêm.
- [ ] Failure mode của dependency được test.
- [ ] Frontend contract được xác nhận nếu có thay đổi.
- [ ] Không có P0/P1 defect liên quan feature.

## 20. Git và review workflow đề xuất

- Branch ngắn theo task: `feat/JRN-002-create-journal`.
- Một pull request tập trung vào một vertical slice nhỏ.
- Migration không sửa sau khi đã merge vào nhánh dùng chung; tạo migration mới.
- PR có thay đổi auth/privacy/safety cần reviewer thứ hai.
- Không merge khi architecture test hoặc migration test fail.
- Commit generated secret, `.env`, journal fixture thật hoặc provider response thật bị cấm.

## 21. Risk register

| Rủi ro | Dấu hiệu sớm | Giảm thiểu |
|---|---|---|
| Scope quá lớn | nhiều module làm dở cùng lúc | hoàn thành vertical slice theo milestone |
| AI provider chậm/lỗi | queue age tăng | async job, timeout, retry, circuit breaker |
| Safety false negative | regression corpus fail | layered rules/classifier/policy, versioned eval |
| Safety false positive | nhiều entry bị block | review threshold và contextual test |
| Rò rỉ journal qua log | body/prompt xuất hiện trong trace | denylist + log tests + no body logging |
| Query dashboard chậm | p95 tăng theo số entry | aggregate/projection/index, load test sớm |
| Encryption làm mất khả năng search | yêu cầu full-text xuất hiện | MVP metadata/tag search; ADR encrypted search sau |
| Admin quyền quá rộng | endpoint reuse user repository decrypt | admin projection riêng, permission test |
| Migration khó rollback | DDL phá dữ liệu | expand/contract, backup, staging rehearsal |
| Team phụ thuộc một người | module chỉ một người hiểu | ADR, review chéo, runbook |

## 22. MVP cut line

### P0 — Bắt buộc

- Identity/session/consent.
- Journal CRUD/check-in với encryption và ownership.
- Safety screening/fail-safe.
- Async sentiment/emotion/topic analysis.
- Dashboard cơ bản và evidence-backed weekly summary.
- Admin tối thiểu cho account status, safety resources và job errors.
- Export và account deletion.
- Audit cho hành động admin nhạy cảm.

### P1 — Nên có

- Self-care goals/habits.
- Knowledge review + RAG citation.
- Asset upload.
- Monthly report.
- Feedback workflow.

### P2 — Sau MVP

- OAuth/social login nếu local auth đã đủ demo.
- SSE thay polling.
- Advanced correlation.
- Wearable/voice/reminder.
- Multi-region, microservices, Kafka.
- Encrypted full-text journal search.

## 23. Sprint đầu tiên đề xuất

### Mục tiêu sprint

Hoàn tất foundation và tạo được user an toàn trong PostgreSQL.

### Task theo thứ tự

1. `FND-001`: ADR auth và encryption.
2. `FND-002`: Problem Details/error codes.
3. `FND-003`: request ID + safe logging.
4. `FND-004`: Clock/UUID/current-user ports.
5. `V2__identity_and_rbac.sql` + repository tests.
6. `IDN-001`: registration transaction.
7. Password hashing + duplicate email race test.
8. OpenAPI cho register/login error contract.

### Sprint demo

```text
POST /api/v1/auth/register
  → validate
  → normalize/HMAC/encrypt email
  → hash password
  → create user/profile/role
  → return safe user response
```

Database phải chứng minh không có plaintext email/password và duplicate concurrent registration chỉ tạo một user.

## 24. Theo dõi tiến độ

Mỗi milestone duy trì một bảng ngắn trong pull request/project board:

| Task | Owner | Status | Dependency | Test evidence | Docs/API |
|---|---|---|---|---|---|
| `IDN-001` | TBD | TODO | FND-001, V2 | link CI | link OpenAPI |

Status chuẩn: `TODO`, `READY`, `IN_PROGRESS`, `IN_REVIEW`, `BLOCKED`, `DONE`. Không đánh dấu `DONE` nếu migration/API/test/documentation liên quan còn thiếu.
