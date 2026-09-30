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
- [x] M1 backend: identity/profile/consent API, V2/V3 migrations và PostgreSQL integration tests.

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

- Persistence M1 dùng JPA entity và `EntityManager` trong `identity`/`user` `infrastructure/persistence`, với native SQL qua JPA cho rate-limit upsert và truy vấn consent mới nhất. Flyway V2/V3 vẫn là schema nguồn; Hibernate chỉ `validate`.
- Flyway V2/V3, register → email verification → login → profile/consent → refresh/reuse chạy qua PostgreSQL Testcontainers. Test có registration race, account pending/suspended, ownership session, ciphertext/tokens, log redaction và key rotation. OpenAPI artifact được xuất từ context bật M1, có các endpoint identity/profile.
- BCrypt cost 12 được chọn sau benchmark local ngày 2026-09-30 (Java 25 trên máy phát triển): trung bình khoảng 264 ms cho một cặp encode + verify; cost 10 khoảng 70 ms, cost 11 khoảng 131 ms. Cần đo lại trên hạ tầng triển khai trước release.
- Local email verification gửi đến Mailpit (`docker compose up -d mailpit`, UI cổng 8025). Production cần SMTP và khóa do secret manager cung cấp; không dùng fallback local.
- Frontend chưa tích hợp các API M1; điều kiện frontend ở exit criteria cần được xác nhận khi tích hợp.
- M1 dùng `audit_logs` sớm trong V2 để audit session revoke. Khi viết V5, chỉ bổ sung outbox/idempotency và phần audit còn thiếu, không tạo lại bảng này.

## 8. M2 — Journal, check-in và safety đầu vào

### Database

- [ ] `V4__journal_and_checkin.sql`.
- [ ] `V5__platform_outbox_idempotency_audit.sql` phần cần cho journal.
- [ ] `V6__safety.sql`.
- [ ] Constraint/range/index đúng database blueprint.

### JRN-001 — Encryption adapter (P0, L)

- [ ] `JournalContentCipher` port.
- [ ] AES-GCM adapter với nonce duy nhất và AAD.
- [ ] Local key provider chỉ dùng development.
- [ ] Key version trong mỗi payload.
- [ ] Decrypt failure trả lỗi an toàn, không log ciphertext/key.
- [ ] Test tampering, wrong owner/AAD và rotation path.

### JRN-002 — Create/read journal (P0, L)

- [ ] Validate TipTap JSON allowlist và giới hạn size/depth.
- [ ] Server tự sinh plain text đã sanitize.
- [ ] Normalize mood/stress/energy/sleep.
- [ ] Lưu `occurred_at`, timezone snapshot và `local_date`.
- [ ] Idempotency-Key cho create.
- [ ] Ownership query bắt buộc `entryId + currentUserId`.

```text
POST /api/v1/journal-entries
GET  /api/v1/journal-entries/{entryId}
GET  /api/v1/journal-entries?cursor=&from=&to=&tag=&favorite=
```

### JRN-003 — Update/delete/favorite (P0, M)

- [ ] `If-Match` hoặc row version chống lost update.
- [ ] Tăng content version khi field ảnh hưởng analysis thay đổi.
- [ ] Soft delete + purge workflow.
- [ ] Favorite endpoints idempotent.
- [ ] Entry đã xóa không xuất hiện trong list/dashboard.

### JRN-004 — Tags/assets (P1, L)

- [ ] Tag encrypted + HMAC lookup.
- [ ] Giới hạn số tag/entry và độ dài.
- [ ] Asset upload dùng Cloudinary signed workflow, delivery type private/authenticated.
- [ ] MIME/size/checksum/malware state.
- [ ] Không chấp nhận remote URL tùy ý làm storage source.

Asset có thể chuyển P2 nếu demo MVP không cần upload ảnh thật.

### CHK-001 — Daily check-in (P0, M)

- [ ] Upsert một check-in/user/local date.
- [ ] Validate timezone và metric range.
- [ ] Activity structured codes.
- [ ] Quy tắc dashboard ưu tiên daily check-in, journal observation chỉ fallback.

### SAF-001 — Rule-based screening (P0, L)

- [ ] Versioned curated rules cho tiếng Việt/Anh.
- [ ] `NORMAL/LOW/MODERATE/HIGH/CRITICAL`.
- [ ] Fail-safe nếu classifier unavailable.
- [ ] Safety response/resource duyệt trước theo locale.
- [ ] Chỉ lưu event tối thiểu, không lưu matched raw text.
- [ ] HIGH/CRITICAL không phát event analysis thông thường.

### SAF-002 — Safety regression corpus (P0, L)

- [ ] Synthetic cases: trực tiếp, phủ định, trích dẫn, tiếng lóng, mỉa mai.
- [ ] Theo dõi false positive/false negative.
- [ ] Không commit dữ liệu người dùng thật.
- [ ] Báo cáo eval có rule/classifier version.

### Exit criteria M2

- Frontend thay `localStorage` bằng API cho journal/check-in cơ bản.
- Journal vẫn lưu được khi dependency AI không tồn tại.
- Nội dung mã hóa trong DB; repository/admin không trả plaintext ngoài user flow.
- Test horizontal authorization pass cho mọi journal endpoint.
- High-risk case đi đúng safety flow.

## 9. M3 — Outbox, jobs và AI analysis

### Database

- [ ] Hoàn tất outbox/job indexes từ V5.
- [ ] `V7__ai_analysis_and_jobs.sql`.
- [ ] Unique/idempotency constraints cho analysis version.

### JOB-001 — Transactional outbox (P0, L)

- [ ] Publish event cùng transaction nghiệp vụ.
- [ ] Claim bằng `FOR UPDATE SKIP LOCKED`.
- [ ] Lease/lock timeout và worker recovery.
- [ ] Exponential backoff + jitter.
- [ ] Dead job và sanitized error.
- [ ] Metrics queue depth/oldest age/success/error.

### AI-001 — Provider ports/adapters (P0, L)

- [ ] `JournalAnalyzer`, `EmbeddingProvider`, `KnowledgeRetriever` ports.
- [ ] Fake deterministic provider cho local/test.
- [ ] Adapter provider thật có timeout, retry boundary và circuit breaker.
- [ ] Không retry lỗi policy/schema vĩnh viễn.
- [ ] Không log raw prompt/response.

### AI-002 — Structured analysis (P0, L)

- [ ] JSON schema cho sentiment/emotions/topics/reflection.
- [ ] Validate enum/range/size.
- [ ] Output safety validation.
- [ ] Encrypt narrative/entities; structured emotion/topic lưu riêng.
- [ ] Lưu provider/model/prompt/policy/content version và usage.

### AI-003 — Analysis lifecycle (P0, M)

- [ ] `PENDING → ANALYZING → ANALYZED/ANALYSIS_FAILED`.
- [ ] Edit journal tạo `ANALYSIS_OUTDATED`.
- [ ] Kết quả job cũ thành `STALE`, không activate.
- [ ] Retry endpoint idempotent và rate-limited.

```text
POST /api/v1/journal-entries/{entryId}/analysis:retry
GET  /api/v1/journal-entries/{entryId}
```

### AI-004 — Frontend status integration (P1, M)

- [ ] Polling có exponential backoff.
- [ ] Response phân biệt pending/failed/blocked.
- [ ] Không hiển thị spinner vô hạn.
- [ ] P2: SSE notification khi analysis hoàn tất.

### Exit criteria M3

- Demo end-to-end journal → safety → async analysis → reflection.
- Tắt AI provider không làm mất journal/outbox event.
- Worker chạy song song không tạo duplicate active analysis.
- Token/cost/latency metrics có nhưng không chứa user content.

## 10. M4 — Insight, dashboard và report

### Database

- [ ] `V8__insights_and_reports.sql`.
- [ ] Evidence/index theo user và period.

### INS-001 — Daily aggregate (P0, L)

- [ ] Mood/stress/energy/sleep timeline theo timezone.
- [ ] Top curated emotions/topics.
- [ ] Journal streak với định nghĩa được test.
- [ ] Daily check-in ưu tiên, journal fallback có source marker.
- [ ] Query plan đạt mục tiêu trên synthetic dataset.

### INS-002 — Evidence-backed insight (P0, L)

- [ ] Minimum sample size cấu hình được.
- [ ] Correlation không được mô tả như causation.
- [ ] Lưu algorithm version, sample size, strength và evidence.
- [ ] Không copy raw journal vào evidence.
- [ ] Narrative chỉ diễn giải metric đã tính.

### RPT-001 — Weekly/monthly report (P1, L)

- [ ] Scheduler theo timezone user.
- [ ] Unique key theo user/type/period.
- [ ] Metrics snapshot bất biến.
- [ ] AI narrative qua output safety.
- [ ] Regenerate tạo version mới.

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

- Dashboard không gọi LLM trong request path.
- Mọi insight hiển thị được evidence/sample size.
- Weekly report chạy idempotent qua scheduler.
- Timezone boundary và DST có test.

## 11. M5 — Self-care

### Database/API

- [ ] `V9__selfcare.sql`.
- [ ] Goal/habit text encrypted.
- [ ] Completion unique theo habit/local date.
- [ ] Ownership và optimistic locking.

```text
POST   /api/v1/self-care/goals
GET    /api/v1/self-care/goals
PATCH  /api/v1/self-care/goals/{goalId}
POST   /api/v1/self-care/goals/{goalId}/habits
PUT    /api/v1/self-care/habits/{habitId}/completions/{localDate}
DELETE /api/v1/self-care/habits/{habitId}/completions/{localDate}
```

### Business rules

- [ ] Goal nằm trong wellness scope, không treatment plan.
- [ ] Completion idempotent.
- [ ] Streak tính theo timezone và frequency config.
- [ ] Correlation habit–mood chỉ xuất hiện khi đủ mẫu.

### Exit criteria M5

- Frontend có thể tạo goal, tick completion và xem progress.
- Không double count completion.
- Goal/habit của user khác luôn trả not found/forbidden theo contract.

## 12. M6 — Knowledge base, RAG và admin

### Database

- [ ] `V10__knowledge_and_prompts.sql`.
- [ ] `V12__seed_extended_admin_roles_permissions.sql` cho content/safety/system/support/auditor.

### KB-001 — Knowledge workflow (P1, L)

- [ ] Draft → review → approve/reject/archive.
- [ ] Version bất biến sau approve.
- [ ] Checksum tránh chunk/embed lại nội dung không đổi.
- [ ] Chỉ approver có permission phù hợp được publish.
- [ ] Audit mọi approve/archive.

### KB-002 — Chunk/embedding/retrieval (P1, L)

- [ ] Chunk strategy có version.
- [ ] Embedding model/dimension provenance.
- [ ] Partial vector index sau khi chốt model.
- [ ] Filter APPROVED + locale + effective date trước retrieval.
- [ ] Citation tới knowledge version/chunk.
- [ ] Eval retrieval bằng curated queries.

### RAG-001 — Safe recommendation (P1, L)

- [ ] Context minimization.
- [ ] Chỉ dùng retrieved approved excerpts.
- [ ] Structured response kèm citation.
- [ ] Safety validation sau generation.
- [ ] Không tự tạo hotline/nguồn hỗ trợ.

### ADM-001 — Admin foundation (P0/P1, L)

- [ ] Separate admin controllers/permissions.
- [ ] User metadata view không có decrypt journal.
- [ ] Suspend/restore account có reason + audit.
- [ ] Job list/retry chỉ hiển thị sanitized error.
- [ ] Aggregate dashboard có minimum cohort size.

```text
GET  /api/v1/admin/users
GET  /api/v1/admin/users/{userId}/metadata
POST /api/v1/admin/users/{userId}:suspend
POST /api/v1/admin/users/{userId}:restore
GET  /api/v1/admin/ai-jobs
POST /api/v1/admin/ai-jobs/{jobId}:retry
```

### Exit criteria M6

- Nội dung chưa approve không bao giờ được retrieve.
- Admin thông thường không có code path giải mã journal.
- Citation truy ngược đúng document version.
- Permission matrix có test deny và allow.

## 13. M7 — Data rights, export và support

### Database

- [ ] `V11__exports_deletion_feedback.sql`.
- [ ] Partial unique cho active deletion/export theo policy.

### EXP-001 — Export (P0, L)

- [ ] CSV machine-readable và PDF user-readable.
- [ ] Export tạo async job.
- [ ] File private/encrypted, signed URL ngắn hạn.
- [ ] Expiry cleanup object + metadata.
- [ ] Re-authentication trước download nếu policy yêu cầu.

### DEL-001 — Account deletion (P0, L)

- [ ] Re-authenticate trước request.
- [ ] Grace period/cancel.
- [ ] Revoke session khi bắt đầu deletion.
- [ ] Idempotent checkpoint worker.
- [ ] Xóa DB, Cloudinary assets, cache và provider artifacts.
- [ ] Audit tối thiểu/pseudonymous theo retention.
- [ ] Test xác nhận không còn data user-owned.

### FBK-001 — Feedback (P1, M)

- [ ] Message encrypted.
- [ ] Không auto-attach journal.
- [ ] Workflow status/assignment.
- [ ] Retention và audit truy cập.

### Exit criteria M7

- User tải được dữ liệu của chính mình.
- Signed URL hết hạn và không public object.
- Deletion job chạy lại an toàn sau lỗi giữa chừng.
- Không còn session hoạt động sau deletion processing.

## 14. M8 — Hardening và release

### Security (P0)

- [ ] Threat model cho auth, journal, admin, AI provider và Cloudinary.
- [ ] Dependency/container scan không còn critical unresolved.
- [ ] Authorization regression toàn endpoint.
- [ ] Rate limit login, journal, analysis retry, export.
- [ ] Secret rotation drill.
- [ ] Encryption key rotation test.
- [ ] CORS và security headers review.
- [ ] Admin MFA hoặc ghi rõ giới hạn nếu demo local.

### Privacy/safety (P0)

- [ ] Log/trace/metric scan không có sensitive text.
- [ ] Provider retention/opt-out được xác nhận.
- [ ] Safety eval đạt threshold đã chốt.
- [ ] Crisis resources được duyệt và có verified timestamp.
- [ ] Consent withdrawal chặn future AI processing.
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
- [ ] API/worker chạy profile riêng.
- [ ] Readiness kiểm tra dependency cần thiết; liveness không phụ thuộc provider ngoài.
- [ ] Flyway chạy một lần có kiểm soát trước rollout app.
- [ ] Rollback application không rollback destructive migration.
- [ ] Dashboard/alert cho HTTP, DB, queue, AI và safety dependency.

### Exit criteria M8

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
