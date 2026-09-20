# MYLOG – BACKEND IMPLEMENTATION PLAN

| Thuộc tính | Giá trị |
| --- | --- |
| Version | 1.0 |
| Status | Ready for implementation |
| Owner | Backend developer |
| Baseline | Spring Boot modular monolith |
| Database | Supabase PostgreSQL |
| Cache | Redis |
| Message broker | RabbitMQ |
| Target | MVP/P0 |
| Estimated duration | 6 tuần, 1 backend developer |
| Last updated | 2026-09-20 |

---

## 1. Mục tiêu

Tài liệu này chuyển các yêu cầu trong PRD, SRS, Backend Architecture và Database Design thành kế hoạch triển khai backend có thể thực thi. Plan xác định:

- Thứ tự phát triển và dependency giữa các module.
- Deliverable của từng milestone.
- API, migration và event cần hoàn thành.
- Test bắt buộc và acceptance criteria.
- Điều kiện release, rollback và vận hành.
- Phần P0 phải có và phần có thể trì hoãn khi thiếu thời gian.

Tài liệu liên quan:

- [Product Requirements](./PRODUCT_REQUIREMENTS.md)
- [Software Requirements Specification](./SOFTWARE_REQUIREMENTS_SPECIFICATION.md)
- [Backend Architecture](./BACKEND_ARCHITECTURE.md)
- [Database Design](./DATABASE_DESIGN.md)

---

## 2. Trạng thái hiện tại

### 2.1. Đã hoàn thành

- Spring Boot 4, Java 21 và Maven Wrapper.
- Profile `local`, `api`, `worker`, `supabase`, `test`.
- Kết nối Supabase Session pooler qua SSL.
- PostgreSQL, Redis và RabbitMQ cho local development bằng Docker Compose.
- Flyway, JPA/Hibernate và PostgreSQL driver.
- RabbitMQ exchange, queue, binding và dead-letter baseline.
- Spring Security baseline, CORS và stateless session.
- Global exception response baseline.
- OpenAPI/Swagger UI.
- Actuator, Prometheus và OpenTelemetry dependencies.
- Dockerfile.
- Migration `V001` cho identity, journal và operational core.
- Migration `V002` khóa Supabase Data API roles và bật RLS.
- Supabase project `my-log-platform` tại Singapore.

### 2.2. Chưa hoàn thành

- Entity, repository và application service thực tế.
- JWT authentication và refresh token rotation.
- Journal CRUD và autosave.
- Transactional outbox publisher.
- RabbitMQ consumers và idempotency.
- AI provider, analysis, reflection và safety pipeline.
- Correction, statistics, insight, dashboard và feedback.
- Integration/E2E test suite.
- CI/CD và production observability hoàn chỉnh.

### 2.3. Quy tắc migration hiện tại

`V001` và `V002` được xem là immutable. Mọi thay đổi schema tiếp theo phải bắt đầu từ `V003`.

---

## 3. Phạm vi release

### 3.1. P0 bắt buộc

- Register, login, refresh token, logout và authorization.
- User profile và preferences.
- Journal create/read/update/delete, history và autosave.
- Mood, stress, energy và multiple journals per day.
- Async AI analysis: sentiment, emotion, topic và explanation.
- Risk classification và safety response.
- User correction cho emotion/topic.
- Reflection questions và regenerate.
- Daily mood, emotion/topic frequency, basic trend/correlation.
- Insight có evidence, confidence và lifecycle.
- Dashboard 7 ngày.
- Suggested action và helpful/not-helpful feedback.
- OpenAPI contract, health check, metrics và structured logging.

### 3.2. P1 sau MVP

- Weekly/monthly reports và PDF export.
- Media/image attachment.
- Emergency contact.
- Free/Plus quota chính thức.
- Semantic search/pgvector.
- Notification nâng cao.

### 3.3. Không thuộc backend MVP

- OAuth, payment gateway và mobile API riêng.
- Therapy chatbot hoặc medical diagnosis.
- Tự động liên hệ emergency contact.
- RAG và semantic recommendation nâng cao.

---

## 4. Nguyên tắc triển khai

1. Mỗi module dùng cấu trúc `api`, `application`, `domain`, `infrastructure`.
2. Controller không truy cập repository trực tiếp.
3. Domain không phụ thuộc Spring, RabbitMQ, Redis hay AI SDK.
4. Mọi query theo user phải chứa ownership condition ngay tại repository.
5. Journal commit không phụ thuộc AI provider hoặc RabbitMQ availability.
6. Event được ghi cùng transaction với dữ liệu nghiệp vụ bằng outbox.
7. Consumer phải idempotent; message delivery được xem là at-least-once.
8. Không đưa journal content, password, JWT hoặc AI prompt chứa dữ liệu riêng tư vào log.
9. Không sửa migration đã chạy; chỉ thêm migration mới.
10. Mỗi API hoàn thành phải có OpenAPI annotation và automated test.

---

## 5. Dependency và critical path

```mermaid
flowchart LR
    F[Foundation] --> I[Identity]
    I --> J[Journal]
    J --> M[Outbox and Messaging]
    M --> A[AI Analysis and Safety]
    A --> C[Correction and Reflection]
    C --> S[Statistics]
    S --> N[Insight and Dashboard]
    N --> H[Hardening and Release]
```

Critical path:

```text
Identity → Journal → Outbox → AI/Safety → Statistics → Insight/Dashboard → Release
```

Frontend có thể tích hợp sớm sau khi Identity và Journal API ổn định; không cần chờ AI pipeline hoàn tất.

---

## 6. Kế hoạch theo milestone

### M0 – Foundation hardening và Supabase schema

**Thời lượng:** 2–3 ngày  
**Mục tiêu:** Biến scaffold hiện tại thành nền tảng đủ an toàn để phát triển feature.

#### Công việc

- [ ] Chạy `V001` và `V002` trên Supabase bằng Flyway.
- [ ] Kiểm tra tables, indexes, constraints và RLS state.
- [ ] Thêm Testcontainers base cho PostgreSQL, Redis và RabbitMQ.
- [ ] Thêm integration-test profile riêng, không dùng Supabase thật trong CI.
- [ ] Chuẩn hóa `ApiErrorResponse`, error code và validation response.
- [ ] Thêm request/correlation ID filter.
- [ ] Thêm log masking cho Authorization, cookie và secret headers.
- [ ] Thêm ArchUnit rules cho module boundaries.
- [ ] Thêm CI workflow: compile → unit test → integration test → package.
- [ ] Chốt JWT signing key format và secret management.

#### Migration

- `V003__complete_identity_schema.sql`
  - `user_consents` nếu thuộc MVP.
  - Bổ sung constraint/index còn thiếu cho identity.

#### Acceptance criteria

- Supabase schema ở đúng Flyway version.
- CI chạy được trên database sạch.
- Không có secret trong source control hoặc test output.
- `mvnw verify` thành công.
- Architecture test chặn dependency sai chiều.

---

### M1 – Identity, authentication và authorization

**Thời lượng:** 4–5 ngày  
**Requirement:** `FR-AUTH-*`, `FR-USER-*`, `NFR-SEC-*`.

#### Domain và persistence

- [ ] `User`, `UserPreference`, `RefreshToken` entities.
- [ ] Normalize email bằng `trim + lowercase` trước unique check.
- [ ] Password hashing bằng BCrypt baseline; benchmark trước khi đổi Argon2id.
- [ ] Refresh token chỉ lưu hash.
- [ ] Token family để phát hiện refresh token reuse.
- [ ] Account status guard: active, locked, deletion pending, deleted.

#### API

```text
POST  /api/v1/auth/register
POST  /api/v1/auth/login
POST  /api/v1/auth/refresh
POST  /api/v1/auth/logout
GET   /api/v1/users/me
PATCH /api/v1/users/me
```

#### Security

- [ ] JWT access token ngắn hạn.
- [ ] Refresh token rotation khi refresh.
- [ ] Revoke token family khi phát hiện reuse.
- [ ] Security filter tạo authenticated principal từ JWT.
- [ ] Rate-limit register, login và refresh bằng Redis.
- [ ] CORS theo allowlist, không dùng wildcard với credentials.
- [ ] Chuẩn bị transport refresh token bằng Secure HttpOnly cookie nếu frontend cùng site; nếu khác site phải chốt rõ SameSite/CORS.

#### Tests

- [ ] Register thành công và duplicate email.
- [ ] Password policy validation.
- [ ] Login đúng/sai password.
- [ ] Access token hết hạn.
- [ ] Refresh rotation và reuse detection.
- [ ] Logout revoke token.
- [ ] User không đọc/sửa profile của user khác.

#### Acceptance criteria

- Không lưu raw password hoặc raw refresh token.
- Mọi protected endpoint trả `401` khi thiếu/sai token.
- Ownership violation trả `404` hoặc `403` theo policy thống nhất.
- Swagger mô tả Bearer authentication và mọi response chính.

---

### M2 – Journal core

**Thời lượng:** 4–5 ngày  
**Requirement:** `FR-JOURNAL-*` và journal state model.

#### Domain

- [ ] `JournalEntry` entity và aggregate rules.
- [ ] Score validation: mood bắt buộc 1–10; stress/energy nullable 1–10.
- [ ] `journalVersion` tăng khi content ảnh hưởng analysis thay đổi.
- [ ] Optimistic lock bằng `version`.
- [ ] Soft delete và ownership-safe query.
- [ ] Entry date được tính từ `occurredAt + timezoneAtEntry`.

#### API

```text
POST   /api/v1/journals
GET    /api/v1/journals?cursor=&limit=&from=&to=
GET    /api/v1/journals/{journalId}
PATCH  /api/v1/journals/{journalId}
DELETE /api/v1/journals/{journalId}
```

#### Contract rules

- [ ] Cursor pagination theo `(created_at, id)`.
- [ ] `POST` hỗ trợ `Idempotency-Key`.
- [ ] `PATCH` dùng version/ETag hoặc request version để phát hiện concurrent edit.
- [ ] Autosave dùng cùng update API, debounce thuộc trách nhiệm frontend.
- [ ] Response không trả internal operational fields.

#### Tests

- [ ] CRUD happy path.
- [ ] Multiple journals cùng ngày.
- [ ] Validation score/content.
- [ ] Pagination ổn định khi insert journal mới.
- [ ] Optimistic conflict.
- [ ] User A không đọc/update/delete journal của user B.
- [ ] Soft-deleted journal không xuất hiện trong history/dashboard.

#### Acceptance criteria

- Journal save hoạt động khi Redis, RabbitMQ và AI provider unavailable.
- API p95 local dưới target của SRS với dataset kiểm thử hợp lý.
- Frontend có thể hoàn thành editor/history dựa trên contract này.

---

### M3 – Transactional outbox và messaging

**Thời lượng:** 3–4 ngày  
**Dependency:** M2.

#### Công việc

- [ ] Tạo outbox event trong cùng transaction khi journal được save/update.
- [ ] Outbox publisher claim batch bằng `FOR UPDATE SKIP LOCKED`.
- [ ] Publisher confirm và retry có exponential backoff/jitter.
- [ ] Event envelope chuẩn: `messageId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, `payload`.
- [ ] Payload chỉ chứa identifier/version, không chứa journal content.
- [ ] Consumer idempotency bằng `processed_messages`.
- [ ] Manual acknowledgement.
- [ ] Dead-letter queue và operational replay procedure.
- [ ] Worker health/metrics cho queue lag, retry và DLQ.

#### Events baseline

```text
journal.created
journal.updated
journal.deleted
journal.analysis.requested
journal.analysis.completed
journal.analysis.failed
journal.corrected
statistics.updated
```

#### Tests

- [ ] Commit journal và outbox là atomic.
- [ ] RabbitMQ down không làm mất event.
- [ ] Publisher restart không làm mất pending row.
- [ ] Duplicate message không tạo duplicate result.
- [ ] Poison message đi DLQ sau max attempts.
- [ ] Multiple worker không claim cùng job.

#### Acceptance criteria

- Delivery semantics được chứng minh là at-least-once + idempotent consumer.
- Có metric cho pending outbox, publish failure và DLQ count.
- Có runbook replay message tối thiểu.

---

### M4 – AI analysis, safety, correction và reflection

**Thời lượng:** 7–8 ngày  
**Dependency:** M3.

#### Schema migrations

- `V004__create_analysis_schema.sql`
  - `journal_analyses`
  - `journal_emotions`
  - `topics`
  - `journal_topics`
  - `journal_corrections`
- `V005__create_reflection_and_safety_schema.sql`
  - `reflection_questions`
  - `reflection_responses` nếu thuộc MVP interaction.
  - `safety_events`
  - `ai_usage_records`

#### Provider abstraction

- [ ] `AiAnalysisPort` trong application/domain boundary.
- [ ] Mock adapter deterministic cho local/test.
- [ ] Một production adapter duy nhất cho MVP.
- [ ] Connect timeout, response timeout và circuit breaker.
- [ ] Structured JSON output schema và strict validation.
- [ ] Prompt version, model name và provider metadata.
- [ ] Token/cost tracking không chứa journal text.

#### Analysis pipeline

- [ ] Claim `analysis_jobs` an toàn.
- [ ] Load journal theo `journalId + userId + journalVersion`.
- [ ] Chạy deterministic safety rules trước AI-generated action.
- [ ] Gọi provider, parse và validate output.
- [ ] Persist analysis, emotion, topic và safety result trong transaction.
- [ ] Chỉ analysis đúng current journal version được active.
- [ ] Retry transient errors; không retry validation error vô hạn.
- [ ] Update journal state và publish completion event.

#### API

```text
GET   /api/v1/journals/{journalId}/analysis
POST  /api/v1/journals/{journalId}/analysis/retry
PATCH /api/v1/journals/{journalId}/corrections
GET   /api/v1/journals/{journalId}/reflections
POST  /api/v1/journals/{journalId}/reflections/regenerate
```

#### Safety invariants

- [ ] HIGH/CRITICAL không nhận normal coaching hoặc suggested action.
- [ ] Không tạo diagnosis, medication advice hoặc therapy claim.
- [ ] Safety event không lưu toàn bộ journal content.
- [ ] Response trả safety metadata để frontend hiển thị popup phù hợp.

#### Tests

- [ ] Provider success, timeout, 429, 5xx và malformed JSON.
- [ ] Retry/backoff và circuit breaker.
- [ ] Stale analysis không ghi đè version mới.
- [ ] HIGH/CRITICAL chặn normal response.
- [ ] Correction giữ nguyên AI original và tạo audit record.
- [ ] Effective emotion/topic ưu tiên user correction.

#### Acceptance criteria

- Journal API không chờ AI hoàn tất.
- Analysis có trạng thái rõ ràng: pending, processing, completed, failed, outdated.
- Mọi AI output đi qua schema validation và safety guard.
- Không có journal content trong message/log/metric label.

---

### M5 – Statistics, insight, dashboard và feedback

**Thời lượng:** 5–6 ngày  
**Dependency:** M4.

#### Schema migrations

- `V006__create_statistics_and_insight_schema.sql`
  - `daily_user_statistics`
  - `daily_emotion_statistics`
  - `insights`
  - `insight_evidence`
  - `suggested_actions`
  - `feedback`
- `V007__add_statistics_and_insight_indexes.sql`

#### Statistics

- [ ] Daily mood average theo timezone.
- [ ] Emotion distribution dùng effective value.
- [ ] Topic frequency dùng effective value.
- [ ] Day-of-week pattern.
- [ ] Trend direction và basic period comparison.
- [ ] Topic–mood association dưới dạng evidence, không tuyên bố causation.
- [ ] Calculation version cho mọi derived result.

#### Insight

- [ ] Minimum evidence threshold.
- [ ] Confidence category: low/medium/high theo rule đã chốt.
- [ ] Evidence rows truy ngược được về aggregate/query basis.
- [ ] Lifecycle: active, fading, expired.
- [ ] Suggested action nhỏ, cụ thể và tối đa theo product rule.
- [ ] Feedback upsert idempotent theo user/target.

#### API

```text
GET /api/v1/dashboard?from=&to=&timezone=
GET /api/v1/statistics/mood?from=&to=
GET /api/v1/statistics/emotions?from=&to=
GET /api/v1/statistics/topics?from=&to=
GET /api/v1/insights
GET /api/v1/insights/{insightId}
PUT /api/v1/feedback/{targetType}/{targetId}
```

#### Redis

- [ ] Cache dashboard/statistics theo user + range + timezone + calculation version.
- [ ] Invalidate cache khi journal/analysis/correction thay đổi.
- [ ] Redis failure fallback về PostgreSQL cho read API.
- [ ] Không cache raw journal content nếu không cần thiết.

#### Tests

- [ ] Timezone và day boundary.
- [ ] NULL stress/energy không biến thành 0.
- [ ] Correction ảnh hưởng statistics đúng cách.
- [ ] Threshold thiếu data không sinh insight giả.
- [ ] Cache hit/miss/invalidation.
- [ ] Redis down vẫn trả kết quả core.
- [ ] Feedback PUT idempotent.

#### Acceptance criteria

- Dashboard 7 ngày trả đủ mood trend, emotion distribution, mood calendar và topic frequency.
- Mọi insight có evidence và confidence.
- API không đưa ra causal claim từ correlation.

---

### M6 – Hardening, observability và release

**Thời lượng:** 4–5 ngày  
**Dependency:** M1–M5.

#### Security và privacy

- [ ] Verify RLS/Data API state trên Supabase.
- [ ] Dependency vulnerability scan.
- [ ] Authorization matrix test cho toàn bộ resource endpoints.
- [ ] Rate limit auth, retry analysis và expensive analytics.
- [ ] Log redaction test.
- [ ] Account/journal deletion workflow.
- [ ] Data retention jobs nếu bắt buộc trong MVP.

#### Reliability

- [ ] Graceful shutdown cho API, worker và message listener.
- [ ] Readiness phản ánh PostgreSQL; policy Redis/RabbitMQ theo runtime role.
- [ ] Timeout cho DB, Redis, RabbitMQ và AI provider.
- [ ] Retry chỉ áp dụng transient failure.
- [ ] Outbox backlog alert và DLQ alert.

#### Observability

- [ ] Structured JSON logs ở production.
- [ ] Correlation: request ID → trace ID → message ID → job ID.
- [ ] Metrics cho HTTP, DB pool, Redis, RabbitMQ, outbox, AI latency/cost và job status.
- [ ] Tracing qua API → DB/outbox → worker → AI provider.
- [ ] Dashboard vận hành tối thiểu và alert thresholds.

#### Release

- [ ] OpenAPI document được export/validate trong CI.
- [ ] Docker image chạy non-root và có health check.
- [ ] Separate API/worker deployment bằng profile.
- [ ] Production secrets nằm trong secret manager của platform deploy.
- [ ] Migration job chạy trước application rollout.
- [ ] Backup/restore smoke test cho Supabase.
- [ ] Rollback runbook cho app và forward-fix migration.

#### Acceptance criteria

- E2E backend flow pass trên staging.
- Không có high/critical vulnerability chưa xử lý.
- API và worker restart không mất job/event.
- Release checklist được ký xác nhận.

---

## 7. Timeline đề xuất

| Tuần | Milestone | Kết quả chính |
| --- | --- | --- |
| 1 | M0 + M1 | Foundation, Supabase migrations, authentication |
| 2 | M2 + đầu M3 | Journal CRUD, history, outbox transaction |
| 3 | M3 + đầu M4 | Messaging, worker, mock AI, analysis state |
| 4 | M4 | Production AI adapter, safety, correction, reflection |
| 5 | M5 | Statistics, insight, dashboard, feedback |
| 6 | M6 | Security, E2E, observability, deployment |

### Đường cắt nếu chỉ có 4 tuần

Giữ lại:

- Identity.
- Journal CRUD/history/autosave.
- Async analysis với một provider.
- Safety classification.
- Correction cơ bản.
- Dashboard 7 ngày và một loại insight có evidence.
- Integration/E2E tests cho critical path.

Trì hoãn sang P1:

- Reports/PDF/media.
- Advanced correlations và period comparisons.
- Nhiều AI provider.
- Reflection response history phức tạp.
- Plus quota và advanced retention automation.

Không được cắt:

- Authorization isolation.
- Password/token security.
- Transactional outbox/idempotent consumer.
- Stale analysis protection.
- Safety guard.
- Migration và integration tests.

---

## 8. Migration roadmap

| Version | Nội dung |
| --- | --- |
| V001 | Core identity, journal, jobs, outbox, idempotency |
| V002 | Supabase Data API lockdown và RLS baseline |
| V003 | Complete identity/consent schema |
| V004 | Analysis, emotion, topic và correction schema |
| V005 | Reflection, safety và AI usage schema |
| V006 | Statistics, insight, action và feedback schema |
| V007 | Statistics/insight indexes và query optimization |
| V008+ | Chỉ thêm theo feature thực tế; report/media/privacy thuộc P1 |

Mỗi migration phải được test với:

- Database sạch.
- Database ở version liền trước.
- PostgreSQL Testcontainer cùng major version gần Supabase.
- `spring.jpa.hibernate.ddl-auto=validate` sau migrate.

---

## 9. Test strategy và quality gates

| Layer | Tool/phạm vi | Quality gate |
| --- | --- | --- |
| Unit | JUnit, AssertJ, Mockito khi cần | Domain rules và branch quan trọng |
| Repository | PostgreSQL Testcontainers | Query, constraint, index behavior |
| Module integration | Spring Boot Test + Testcontainers | Use case qua real adapters |
| Messaging | RabbitMQ Testcontainer | Publish, retry, idempotency, DLQ |
| Cache | Redis Testcontainer | TTL, invalidation, degraded mode |
| API | MockMvc/REST client | Contract, auth, validation, errors |
| Provider contract | Mock HTTP server | Timeout, malformed output, rate limit |
| Architecture | ArchUnit | Module dependency rules |
| E2E | API + worker + all infrastructure | Critical user journey |

### Critical E2E journey

```text
register
→ login
→ create journal
→ outbox publish
→ analysis worker
→ safety check
→ analysis persisted
→ statistics updated
→ insight generated
→ dashboard query
→ correction
→ dashboard recalculated
```

### Pull request gate

- Compile thành công.
- Unit và integration tests pass.
- Flyway migration test pass.
- `git diff --check` pass.
- Không có secret scan finding.
- OpenAPI không có breaking change ngoài chủ đích.
- Code coverage không giảm ở critical domain rules.

---

## 10. API delivery workflow với frontend

Cho mỗi endpoint:

1. Chốt request/response/error schema.
2. Thêm OpenAPI annotation và example.
3. Export `/v3/api-docs` hoặc cung cấp Swagger URL.
4. Frontend review contract trước khi implementation khóa.
5. Backend viết API contract test.
6. Thông báo breaking change; không silently đổi field semantics.

Quy ước trạng thái async cần thống nhất sớm:

```text
PENDING
PROCESSING
COMPLETED
FAILED
OUTDATED
```

Frontend không được suy luận AI status từ việc field analysis có `null` hay không.

---

## 11. Environment strategy

| Environment | PostgreSQL | Redis/RabbitMQ | AI |
| --- | --- | --- | --- |
| Unit test | Không hoặc fake | Không | Fake |
| Integration test | Testcontainers | Testcontainers | Mock server |
| Local offline | Docker PostgreSQL | Docker | Mock provider mặc định |
| Local Supabase | Supabase Session pooler | Docker | Mock/real theo flag |
| Staging | Supabase | Managed/container | Provider sandbox/limited key |
| Production | Supabase | Managed | Production provider |

Không dùng production Supabase cho automated tests. Không commit `.env`, DB password, JWT key hoặc AI API key.

---

## 12. Risk register

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Scope MVP quá lớn cho một backend developer | Trễ tiến độ | Dùng đường cắt 4 tuần; khóa P0/P1 |
| AI output không ổn định | Data sai, pipeline lỗi | Strict JSON schema, retry giới hạn, mock contract tests |
| Journal update khi analysis đang chạy | Stale result | Journal version check trước persist |
| RabbitMQ/message duplicate | Duplicate analysis/statistics | Processed message key và unique constraints |
| Supabase connection exhaustion | API outage | Hikari pool nhỏ, metric pool, Session pooler |
| Data API vô tình mở | Rò rỉ dữ liệu | Data API disabled, revoke roles, RLS, security verification |
| Sensitive data xuất hiện trong logs | Privacy incident | Logging prohibition, masking và log tests |
| Timezone calculation sai | Dashboard sai | Persist timezone-at-entry và boundary tests |
| Redis unavailable | Dashboard/rate limit lỗi | Core reads fallback DB; policy rõ theo endpoint |
| AI provider cost tăng | Vượt ngân sách | Token limit, usage records, quota và alert |

---

## 13. Backend Definition of Done

Backend MVP chỉ được xem là hoàn thành khi:

- Authentication và refresh rotation hoạt động.
- Authorization cô lập dữ liệu giữa users.
- Journal CRUD/history/autosave hoạt động.
- Journal save không phụ thuộc AI provider.
- Outbox không mất event khi RabbitMQ unavailable.
- Consumer idempotent, có retry và DLQ.
- Stale analysis không ghi đè version mới.
- AI output được validate và qua safety guard.
- Correction được audit và statistics dùng effective value.
- Insight có evidence, confidence và lifecycle.
- Redis failure không làm mất core journal functionality.
- Không log journal content, password hoặc token.
- Flyway chạy thành công trên database sạch và staging.
- Integration tests dùng PostgreSQL/Redis/RabbitMQ thật qua containers.
- OpenAPI được frontend chấp nhận.
- Metrics, health, logs và traces đủ cho deployment.
- Có migration, deployment và rollback runbook.

---

## 14. Backlog khởi động ngay

Thứ tự 10 task đầu tiên:

1. Chạy Flyway `V001/V002` trên Supabase và xác minh schema.
2. Tạo Testcontainers integration-test foundation.
3. Tạo `V003__complete_identity_schema.sql`.
4. Implement `User` và `RefreshToken` persistence.
5. Implement register + password hashing.
6. Implement login + JWT access token.
7. Implement refresh rotation/reuse detection.
8. Implement logout và `/users/me`.
9. Hoàn thiện security/ownership integration tests.
10. Khóa OpenAPI contract Identity với frontend trước khi sang Journal.

Task tiếp theo sau Identity là Journal CRUD, không bắt đầu AI integration trước khi Journal versioning và outbox transaction đã ổn định.

---

## 15. Theo dõi tiến độ

Mỗi milestone dùng trạng thái:

```text
NOT_STARTED → IN_PROGRESS → BLOCKED → IN_REVIEW → DONE
```

Mỗi task tối thiểu phải có:

- Requirement ID liên quan.
- Owner.
- Estimate.
- Dependency.
- Acceptance criteria.
- Test evidence.
- Migration/API/event impact nếu có.

Review tiến độ cuối mỗi tuần dựa trên deliverable chạy được, không dựa trên phần trăm code đã viết.
