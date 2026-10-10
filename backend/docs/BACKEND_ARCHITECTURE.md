# Kiến trúc backend mylog

> Architecture blueprint v1 — backend MVP và hướng mở rộng  
> Tên sản phẩm chính thức trong code và tài liệu: **mylog**

## 1. Mục tiêu

mylog là nền tảng nhật ký cá nhân có dữ liệu đặc biệt nhạy cảm. Backend phải bảo đảm:

- CRUD journal/check-in nhanh và vẫn hoạt động khi AI lỗi.
- Safety screening chạy trước mọi phản hồi sinh bởi LLM.
- AI, embedding, report và export chạy nền, retry được và quan sát được.
- Dữ liệu luôn giới hạn theo chủ sở hữu; admin mặc định không đọc journal thô.
- Insight truy ngược được về số liệu hỗ trợ, không chỉ là đoạn văn AI sinh.
- Code dễ tìm, dễ test và có đường tách service rõ nếu quy mô tăng.

Sản phẩm chỉ hỗ trợ mental wellness và self-reflection, không chẩn đoán, điều trị hoặc thay thế chuyên gia.

Danh mục tác nhân, luồng chính/ngoại lệ và trạng thái triển khai của từng nghiệp vụ nằm ở [USE_CASE_CATALOG.md](USE_CASE_CATALOG.md).

## 2. Lựa chọn kiến trúc

### 2.1 Modular monolith

MVP dùng một ứng dụng Spring Boot, chia thành các module nghiệp vụ có ranh giới rõ. Lựa chọn này giúp team nhỏ phát triển/deploy đơn giản, giữ transaction đáng tin cậy và tránh chi phí microservices quá sớm. Các tác vụ nặng vẫn được tách khỏi request thread bằng worker.

Khi có nhu cầu thật, `analysis`, `reporting`, `notification` và `knowledge` là các ứng viên tách service. Chỉ tách khi có khác biệt rõ về tải, bảo mật, ownership hoặc chu kỳ deploy.

### 2.2 Package by feature

Không gom toàn dự án vào các folder cấp cao `controller/service/repository/entity`. Mỗi module có các lớp nhỏ:

```text
api             HTTP contract, request/response DTO, controller
application     use case, transaction boundary, input/output port
domain          model, value object, policy, domain event
infrastructure  JPA, Redis, external provider, adapter
```

Chiều phụ thuộc:

```text
api ───────► application ───────► domain
                    ▲
                    │ implements ports
             infrastructure
```

`domain` không phụ thuộc Spring, JPA, HTTP, Redis hay SDK AI. Module khác chỉ gọi application facade/public use case, không gọi repository hoặc entity nội bộ.

### 2.3 Đồng bộ và bất đồng bộ

- Đồng bộ: auth, CRUD journal, check-in, goal completion, safety screening đầu vào, đọc dashboard.
- Bất đồng bộ: emotion/topic analysis, embedding, report, export lớn, cleanup.
- API trả `202 Accepted` khi job đã được nhận nhưng chưa hoàn thành.
- MVP có thể polling trạng thái; SSE là nâng cấp sau.

### 2.4 Nguồn dữ liệu

- PostgreSQL do Supabase quản lý là source of truth cho nghiệp vụ, job và audit ở môi trường triển khai; local/test vẫn dùng PostgreSQL container.
- `pgvector` lưu embedding của knowledge base đã duyệt.
- Redis chỉ dùng cache ngắn hạn, rate limit và distributed lock.
- Transactional outbox bảo đảm event không mất sau commit.

## 3. Bối cảnh hệ thống

```text
Next.js user app ───────┐
                       ├── HTTPS ──► Spring Boot API
Next.js admin app ──────┘                 │
                                         ├── Supabase PostgreSQL + pgvector
                                         ├── Redis
                                         ├── Cloudinary private images
                                         └── AI provider

Spring Boot worker ── claim outbox/jobs ──► AI / RAG / report / export
```

Cùng một artifact có thể chạy theo profile:

- `api`: nhận HTTP, không chạy job nặng.
- `worker`: xử lý job, chỉ expose health nội bộ.
- `all`: thuận tiện cho local development.

## 4. Ranh giới module

| Module | Trách nhiệm | Không được làm |
|---|---|---|
| `identity` | đăng ký, đăng nhập, refresh token, session, reset password | chứa wellness profile |
| `user` | profile, timezone, language, consent, privacy | xác thực token |
| `journal` | entry, tag, asset, favorite, analysis status | gọi trực tiếp SDK AI |
| `checkin` | mood, stress, energy, sleep, activity theo ngày | sinh insight bằng LLM |
| `safety` | risk screening, policy, response, event tối thiểu | tư vấn điều trị |
| `analysis` | sentiment, emotion, topic, entity, reflection | tự quyết safety cuối cùng |
| `insight` | trend/correlation và evidence có cấu trúc | sửa journal gốc |
| `reporting` | weekly/monthly report và snapshot | truy cập entity module khác |
| `selfcare` | goal, habit, completion | khuyến nghị y khoa |
| `knowledge` | tài liệu, review, chunk, embedding, retrieval | publish bản chưa duyệt |
| `prompt` | journal prompt và nội dung hiển thị | quản lý provider secret |
| `export` | CSV/PDF, file tạm, expiry | trả dữ liệu user khác |
| `admin` | use case vận hành, aggregate dashboard | đọc journal raw mặc định |
| `audit` | append-only audit hành động nhạy cảm | chứa journal thô |
| `feedback` | feedback/bug report và workflow | bắt buộc gửi nội dung nhạy cảm |
| `platform` | config, web, security primitives, crypto dùng chung, jobs, observability | chứa business rule |

## 5. Cấu trúc repository

```text
my-log-platform/
├── README.md
├── backend/
│   ├── README.md
│   ├── compose.yaml
│   ├── .env.example
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── Dockerfile
│   ├── docs/
│   │   ├── BACKEND_ARCHITECTURE.md
│   │   ├── BACKEND_DEVELOPMENT_PLAN.md
│   │   ├── API_CONVENTIONS.md
│   │   ├── DATABASE_OVERVIEW.md
│   │   ├── AI_OPERATIONS_RUNBOOK.md
│   │   ├── database/
│   │   │   └── mylog.dbml
│   │   └── adr/
│   │       ├── 0001-modular-monolith.md
│   │       ├── 0002-transactional-outbox.md
│   │       └── 0003-journal-encryption.md
│   └── src/
│       ├── main/
│       │   ├── java/com/mylog/
│       │   │   ├── MylogApplication.java
│       │   │   ├── identity/
│       │   │   ├── user/
│       │   │   ├── journal/
│       │   │   ├── checkin/
│       │   │   ├── safety/
│       │   │   ├── analysis/
│       │   │   ├── insight/
│       │   │   ├── reporting/
│       │   │   ├── selfcare/
│       │   │   ├── knowledge/
│       │   │   ├── prompt/
│       │   │   ├── export/
│       │   │   ├── admin/
│       │   │   ├── audit/
│       │   │   ├── feedback/
│       │   │   └── platform/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-test.yml
│       │       ├── db/migration/
│       │       ├── prompts/
│       │       └── logback-spring.xml
│       └── test/
│           ├── java/com/mylog/
│           │   ├── architecture/
│           │   ├── journal/
│           │   ├── safety/
│           │   └── support/
│           └── resources/
└── front-end/my-app-web/
```

MVP chưa cần multi-module Maven. Package boundary + ArchUnit ít ceremony hơn. Chỉ tách Maven module khi build time, ownership hoặc deploy độc lập thực sự yêu cầu.

## 6. Cấu trúc một module

Ví dụ rút gọn từ module `journal` hiện tại (chỉ liệt kê các file tiêu biểu):

```text
journal/
├── api/
│   ├── JournalController.java
│   ├── request/
│   │   ├── CreateJournalEntryRequest.java
│   │   └── UpdateJournalEntryRequest.java
│   └── response/
│       ├── JournalEntryResponse.java
│       └── JournalEntrySummaryResponse.java
├── application/
│   ├── JournalService.java
│   ├── JournalStore.java
│   ├── JournalContentCipher.java
│   ├── command/
│   │   ├── CreateJournalEntryCommand.java
│   │   └── UpdateJournalEntryCommand.java
│   └── query/
│       ├── JournalEntryView.java
│       └── JournalSummaryView.java
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   │   ├── JournalEntry.java
    │   │   ├── JournalTag.java
    │   │   ├── JournalEntryTag.java
    │   │   └── JournalEntryTagId.java
    │   ├── JpaJournalStore.java
    │   └── JpaJournalTagStore.java
    └── crypto/AesGcmJournalContentCipher.java
```

Quy tắc:

- Controller chỉ xử lý HTTP, lấy `currentUserId`, gọi use case và map response.
- Use case có thể tách command/query service khi cần; `JournalService` hiện điều phối cả hai. Không cần CQRS framework.
- Transaction boundary đặt ở application service.
- Domain model bảo vệ invariant, không mở setter hàng loạt.
- JPA entity nằm trong infrastructure và không được trả ra API.
- Request/response DTO tách khỏi command/domain để API tiến hóa độc lập.
- Mỗi request/response DTO của HTTP là một record ở file riêng trong `api/request/` hoặc `api/response/` như cây thư mục trên; không khai báo DTO lồng trong controller. Controller chuyển dữ liệu từ use case sang response DTO, không dùng trực tiếp application result/projection làm HTTP response. Endpoint không có body thì không cần response DTO.
- Nếu use case cần kiểu dữ liệu trả về riêng, đặt nó trong `application/query/` hoặc `application/result/` theo vai trò, tách khỏi contract HTTP. Ví dụ hiện tại: `ProfileView`, `ConsentView` là dữ liệu application; `ProfileResponse`, `ConsentResponse` là contract API.
- Mapper quan trọng viết tay và có test.

Ví dụ application service minh họa luồng nghiệp vụ; đoạn mã dưới đây không phải class hiện có trong repository:

```java
@Service
@Transactional
class JournalCommandService {
    private final JournalEntryRepository entries;
    private final RiskScreeningUseCase riskScreening;
    private final JournalEventPublisher events;

    JournalEntryResult create(UserId ownerId, CreateJournalEntryCommand command) {
        var entry = JournalEntry.create(ownerId, command);
        var risk = riskScreening.screen(ownerId, entry.contentForScreening());
        entry.applyRiskDecision(risk.level());
        entries.save(entry);

        if (risk.allowsStandardAnalysis()) {
            events.publish(new JournalEntrySubmitted(entry.id(), ownerId));
        }
        return JournalEntryResult.from(entry, risk.safeUserResponse());
    }
}
```

Persistence adapter mã hóa nội dung. Event/outbox chỉ mang ID/version, tuyệt đối không chứa raw journal.

### 6.1 So sánh với Controller → Service → Repository truyền thống

Luồng cơ bản vẫn giống Spring Boot thông thường, nhưng tên gọi và vị trí phụ thuộc rõ hơn:

```text
HTTP Request
   ↓
api/JournalController                       Controller
   ↓
application/JournalService                  Service / Use case
   ↓
business rules trong use case/domain         Business rules
   ↓
application/JournalStore                     Repository interface (port)
   ↓
infrastructure/persistence/
  JpaJournalStore                             Repository implementation (adapter)
   ↓
infrastructure/persistence/entity/
  JournalEntry                               JPA model
   ↓
PostgreSQL
```

Response đi ngược lại qua mapper:

```text
JPA model → Domain/Projection → Response DTO → JSON
```

Khác biệt quan trọng so với mô hình ba layer đơn giản:

- JPA/Hibernate là cách truy cập PostgreSQL thống nhất trong production code. Đặt entity và class khóa ghép `@IdClass` trong `infrastructure/persistence/entity/`; adapter/repository ở `infrastructure/persistence/` ánh xạ entity sang record của application port. Có thể dùng native SQL qua JPA cho thao tác đặc thù PostgreSQL hoặc cần tính nguyên tử như `ON CONFLICT`, `DISTINCT ON`, `FOR UPDATE SKIP LOCKED`; kiểm thử các câu SQL đó trên PostgreSQL thật.
- Class JPA dùng tên ngắn theo đối tượng (`User`, `AuthSession`, `UserProfile`) trong package `infrastructure/persistence/entity`, không thêm hậu tố `Entity`. Package con là package Java riêng nên class và field được repository truy cập trực tiếp cần có visibility phù hợp; không cho tầng API/application hoặc module khác phụ thuộc vào entity. Biến trong repository đặt theo dữ liệu đang xử lý (`user`, `session`, `profile`), không đặt chung là `entity`.
- Hai entity kỹ thuật hiện có của `platform` (`OutboxEvent`, `IdempotencyKey`) vẫn nằm cùng package với adapter tương ứng; đây không phải mẫu tổ chức cho feature mới.
- Flyway migration là nguồn schema thực thi; Hibernate `ddl-auto=validate` chỉ kiểm tra ánh xạ, không tạo/sửa bảng. Không sửa migration đã áp dụng lên Supabase; thay đổi schema dùng migration mới. Không trả entity trực tiếp qua API.
- Mặc định bản ghi độc lập dùng `id UUID` do application tạo. Bảng nối chỉ biểu diễn một cặp quan hệ có thể dùng khóa ghép; quan hệ một–một có thể dùng FK làm PK; bảng trạng thái một dòng theo subject có thể dùng subject key. Khi thêm surrogate `id`, vẫn giữ `UNIQUE` trên khóa nghiệp vụ cần lookup/chống trùng. Ví dụ `auth_refresh_history` dùng `id UUID` từ V4 và `UNIQUE(token_hash)` để phát hiện token dùng lại; không dùng `byte[]` làm JPA `@Id` cho bản ghi này.
- `Service` được gọi là application service/use case và chỉ điều phối một nghiệp vụ cụ thể.
- Business rule quan trọng nằm trong domain object/policy, không dồn hết vào một service lớn.
- Repository trong application là interface; code JPA triển khai interface đó ở infrastructure.
- Controller không biết JPA entity và repository implementation.
- Module khác không gọi xuyên vào repository; nó gọi public application facade hoặc giao tiếp bằng event.
- Query chỉ đọc có thể dùng projection tối ưu qua query port, không bắt buộc dựng đầy đủ domain aggregate.

Với một tính năng CRUD rất đơn giản, luồng có thể chỉ là:

```text
Controller → Application Service → Repository Port → JPA Adapter → Database
```

Với journal có AI, luồng ghi và luồng phân tích được tách ra:

```text
JournalController
   → JournalService
      → SafetyScreeningUseCase
      → JournalStore port → JpaJournalStore → persistence/entity/JournalEntry → PostgreSQL
      → OutboxPublisher → outbox_events

OutboxWorker → JournalAnalysisOutboxHandler → ai_jobs
AiJobWorker → AnalysisJobHandler
   → AI provider adapter
   → output safety validation
   → AnalysisStore port → JpaAnalysisStore → persistence/entity/AiAnalysis → PostgreSQL
```

Nhờ vậy thao tác lưu journal không thất bại chỉ vì AI chậm hoặc tạm ngừng hoạt động.

## 7. Luồng quan trọng

### 7.1 Tạo journal và phân tích

```text
1. POST /api/v1/journal-entries + Idempotency-Key.
2. Xác thực user, validate và rate limit.
3. Tạo entry; persistence adapter mã hóa title/content.
4. Safety chạy rule + classifier đồng bộ.
5a. HIGH/CRITICAL:
    - lưu risk level và safety event tối thiểu;
    - không enqueue reflection/recommendation thường;
    - trả safety response đã kiểm duyệt.
5b. NORMAL/LOW khi policy cho phép:
    - commit journal + outbox trong cùng transaction;
    - trả 201 với analysisStatus=PENDING.
5c. MODERATE/CONSTRAIN: lưu bài nhưng chặn ordinary reflection cho tới khi có luồng
    phản hồi có ràng buộc riêng được duyệt.
6. Worker claim event bằng SELECT ... FOR UPDATE SKIP LOCKED.
7. Giải mã đúng entry cần thiết, tối thiểu hóa context, gọi AI.
8. Validate JSON schema và chạy output safety check.
9. Lưu AIAnalysis có version; đổi status thành ANALYZED.
10. Frontend polling entry hoặc dùng SSE trong phiên bản sau.
```

Nếu AI provider lỗi, journal vẫn được lưu. Job retry exponential backoff; hết lượt thử chuyển `DEAD`, journal thành `ANALYSIS_FAILED`. Retry không tạo entry mới.

### 7.2 Sửa journal và chống race condition

- Tăng `contentVersion` khi title/content/metrics ảnh hưởng phân tích thay đổi.
- Kết quả cũ giữ để truy vết nhưng không còn active.
- Trạng thái chuyển `ANALYSIS_OUTDATED`, rồi enqueue phân tích mới.
- Worker chỉ activate nếu `analysis.contentVersion == journal.contentVersion`; kết quả đến trễ thành stale.
- Dùng optimistic locking (`@Version`) tránh hai tab ghi đè.

### 7.3 Dashboard/insight

Dashboard không gọi LLM khi mở trang:

1. Query journal/check-in/analysis có cấu trúc.
2. Aggregate theo timezone user.
3. Lưu insight gồm loại, khoảng thời gian, strength và evidence.
4. LLM chỉ diễn giải quan sát đã tính, không tự tạo số liệu.
5. Response kèm sample size và lưu ý “tương quan không phải nhân quả”.

Chỉ tính/hiển thị correlation khi đạt minimum sample size được cấu hình.

### 7.4 Weekly/monthly report

- Scheduler tạo job theo timezone và unique key `(user_id, period_type, period_start)`.
- Reporting lấy snapshot qua query ports, không chạm repository nội bộ module khác.
- Metrics/evidence tách khỏi narrative do LLM sinh.
- Regenerate tạo version mới, không overwrite âm thầm.

### Đề xuất sản phẩm sau M5: self-compassion và bản chuẩn bị tham vấn

Ba luồng **chưa triển khai** được mô tả trong [đề xuất tính năng](SELF_COMPASSION_FEATURE_PROPOSALS.md): thư do người dùng viết để tự đọc lại, kho điểm sáng nhỏ có xác nhận trước khi lưu, và bản tóm tắt chọn lọc do người dùng duyệt trước khi xuất. Đây không phải đầu ra mặc định của `JournalAnalyzer` hoặc report hiện có.

- Một điểm check-in thấp có thể gợi ý *lựa chọn* xem thư; không tự mở/gửi thư và không thay thế `SAFETY_FLOW` bằng nội dung cũ.
- Trích xuất điểm sáng, nếu được bổ sung, chỉ dùng bài/phiên bản đã lưu, sau consent và safety; người dùng sửa hoặc từ chối trước khi tạo mục riêng. Không dùng `sentiment` làm bằng chứng một sự kiện đã xảy ra.
- Bản chuẩn bị tham vấn chỉ dùng dữ liệu tổng hợp và các mục người dùng chọn, kèm nguồn/số mẫu; không mặc định kèm nguyên văn journal hoặc suy luận chẩn đoán. Người dùng chủ động xem trước và xuất, không chia sẻ tự động. Luồng này khác export toàn bộ dữ liệu cá nhân.
- Dữ liệu nhạy cảm mới phải mã hóa, owner-scope, thuộc export/deletion; persistence/API chỉ được chốt sau khi có contract, retention và migration riêng. Các module truy cập nhau qua application port/facade.

### 7.5 Xóa tài khoản

1. Re-authenticate và tạo `deletion_request` có grace period.
2. Vô hiệu hóa session/refresh token.
3. Worker xóa object, cache, journal, analysis, dữ liệu cá nhân và export.
4. Audit vận hành chỉ giữ dữ liệu tối thiểu đã giảm định danh theo policy.
5. Job có checkpoint và idempotent để tiếp tục sau lỗi.

## 8. Mô hình dữ liệu

Thiết kế table, constraint, index, encryption và retention chi tiết nằm tại [DATABASE_OVERVIEW.md](DATABASE_OVERVIEW.md). Sơ đồ có thể import vào dbdiagram nằm tại [database/mylog.dbml](database/mylog.dbml).

Mọi bảng nghiệp vụ dùng `UUID`, `created_at timestamptz`, `updated_at timestamptz`. Timestamp lưu UTC; dữ liệu theo ngày lưu thêm `local_date` và timezone.

| Nhóm | Bảng chính | Ghi chú |
|---|---|---|
| Identity | `users`, `roles`, `permissions`, `user_roles`, `sessions`, `refresh_tokens` | token chỉ lưu hash |
| Profile | `user_profiles`, `user_consents`, `privacy_settings` | consent có version |
| Journal | `journal_entries`, `journal_tags`, `journal_entry_tags`, `journal_assets` | title/content mã hóa |
| Check-in | `daily_checkins`, `activities` | unique user + local date nếu một bản/ngày |
| AI | `ai_analyses`, `analysis_emotions`, `analysis_topics`, `ai_jobs`, `ai_usage` | model/prompt/config version |
| Safety | `safety_events`, `safety_policy_versions`, `safety_resources` | không lưu raw text trong event |
| Insight | `insights`, `insight_evidence` | evidence trỏ metric/date |
| Report | `reports`, `report_evidence` | versioned theo kỳ |
| Self-care | `selfcare_goals`, `habits`, `habit_completions` | completion idempotent |
| RAG | `knowledge_items`, `knowledge_versions`, `knowledge_chunks` | chỉ APPROVED được retrieve |
| Content | `journal_prompts`, `prompt_categories` | trạng thái + locale |
| Ops | `outbox_events`, `job_executions`, `audit_logs`, `system_configs` | append-only theo nghiệp vụ |
| Support | `feedback`, `export_requests`, `deletion_requests` | export có expiry |

### 8.1 Journal entry

```text
journal_entries
- id UUID PK
- user_id UUID NOT NULL
- encrypted_title BYTEA
- encrypted_content BYTEA NOT NULL
- encryption_key_version VARCHAR NOT NULL
- local_date DATE NOT NULL
- occurred_at TIMESTAMPTZ NOT NULL
- timezone VARCHAR NOT NULL
- mood_code VARCHAR
- mood_score NUMERIC(3,1) CHECK 1..10
- stress_score NUMERIC(3,1) CHECK 1..10
- energy_score NUMERIC(3,1) CHECK 1..10
- sleep_minutes SMALLINT CHECK 0..1440
- favorite BOOLEAN NOT NULL DEFAULT FALSE
- risk_level VARCHAR NOT NULL
- analysis_status VARCHAR NOT NULL
- content_version INTEGER NOT NULL
- version BIGINT NOT NULL              -- optimistic lock
- created_at / updated_at / deleted_at
```

Index tối thiểu:

```sql
(user_id, occurred_at DESC)
(user_id, local_date DESC)
(user_id, favorite) WHERE deleted_at IS NULL
(analysis_status, updated_at)
  WHERE analysis_status IN ('PENDING', 'ANALYSIS_FAILED')
```

Không index plaintext title/content. MVP search bằng metadata/tag; encrypted full-text search cần threat model và ADR riêng.

### 8.2 Lifecycle

Trạng thái khớp frontend hiện tại:

```text
DRAFT → SAVED → ANALYZING → ANALYZED
                  │             │
                  └─► ANALYSIS_FAILED

ANALYZED ── content changed ──► ANALYSIS_OUTDATED ──► ANALYZING
```

Job: `PENDING`, `RUNNING`, `SUCCEEDED`, `RETRY_WAIT`, `DEAD`, `CANCELLED`.

Knowledge version: `DRAFT`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `ARCHIVED`. Chỉ bản `APPROVED`, còn hiệu lực được dùng trong RAG.

### 8.3 Journal metrics và DailyCheckIn

Frontend hiện gắn `moodScore`, `stressScore`, `energyScore`, `sleepHours` vào mỗi journal. Backend giữ các giá trị này như **observation tại thời điểm viết**. `DailyCheckIn` là bản tổng kết có chủ đích, tối đa một bản/user/ngày.

Để dashboard không đếm trùng:

1. Nếu ngày đó có `DailyCheckIn`, dùng check-in làm daily data point.
2. Nếu chưa có, dùng journal observation mới nhất trong ngày làm fallback và đánh dấu `source=JOURNAL_FALLBACK`.
3. Không cộng/trung bình cả check-in và mọi journal entry một cách ngầm định.
4. Insight/report lưu source cùng evidence để kết quả tái hiện được.

### 8.4 Mapping với frontend hiện tại

| Frontend | API/backend | Quy ước |
|---|---|---|
| `MoodType` như `calm-joy` | `moodCode` | API giữ stable kebab-case code; backend map sang enum/value object |
| `date` + `time` | `occurredAt`, `localDate`, `timezone` | client gửi ISO timestamp có offset; server tự tính lại local date |
| `sleepHours` decimal | `sleepMinutes` | API có thể nhận decimal, application chuẩn hóa thành phút |
| `status` | `analysisStatus` | tránh nhầm lifecycle của entry với lifecycle AI |
| `aiAnalysis.emotions[].percentage` | normalized score | lưu `0..1`, response có thể map sang phần trăm |
| `tags: string[]` | tag resource theo owner | trim, Unicode-normalize, giới hạn số lượng/độ dài |
| `photoUrl` | `journalAssetId` + signed URL | không nhận URL tùy ý làm nguồn lưu trữ chính |

Nội dung từ TipTap phải có contract rõ (`contentJson` ưu tiên, optional `plainText` do server tự sinh). Server sanitize theo allowlist, giới hạn kích thước/depth, loại script/event handler và không cho nhúng remote URL tùy ý. Plain text dùng cho safety/AI; JSON đã sanitize dùng để render.

## 9. API v1

Base path `/api/v1`. JSON `camelCase`; enum `UPPER_SNAKE_CASE`; ID là UUID string; thời gian ISO-8601.

### 9.1 User API

```text
POST   /auth/register
POST   /auth/email-verifications
POST   /auth/email-verifications:confirm
POST   /auth/email-verifications:confirm-code
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
GET    /me
PATCH  /me
POST   /me/onboarding:complete
GET    /me/consents
PUT    /me/consents/{type}
GET    /me/sessions
DELETE /me/sessions/{sessionId}
DELETE /me/sessions?exceptCurrent=true

POST   /journal-entries
GET    /journal-entries?from=&to=&cursor=&tag=&favorite=
GET    /journal-entries/{entryId}
PATCH  /journal-entries/{entryId}
DELETE /journal-entries/{entryId}
PUT    /journal-entries/{entryId}/favorite
DELETE /journal-entries/{entryId}/favorite
POST   /journal-entries/{entryId}/analysis:retry
GET    /journal-entries/{entryId}/analysis

PUT    /check-ins/{localDate}
GET    /check-ins?from=&to=
GET    /dashboard?range=30d
GET    /insights?from=&to=&cursor=
GET    /reports?type=WEEKLY&cursor=
GET    /reports/{reportId}

POST   /self-care/goals
GET    /self-care/goals
PATCH  /self-care/goals/{goalId}
POST   /self-care/goals/{goalId}/habits
PUT    /self-care/habits/{habitId}/completions/{localDate}
DELETE /self-care/habits/{habitId}/completions/{localDate}

GET    /journal-prompts?locale=vi
POST   /exports
GET    /exports/{exportId}
POST   /exports/{exportId}:authorize-download
GET    /exports/{exportId}/file?expires=...&signature=...
POST   /account-deletion-requests
POST   /account-deletion-requests/{requestId}:status
POST   /account-deletion-requests/{requestId}:cancel
POST   /feedback
GET    /feedback/{feedbackId}
```

### 9.2 Admin API

Các endpoint dưới đây gồm API đã triển khai và roadmap; M6 đã có user metadata/status, dashboard, knowledge version workflow và AI job vận hành; M7 thêm feedback admin. Gán role, prompt workflow, safety metrics và audit read API vẫn để mốc sau.

```text
GET    /admin/users
GET    /admin/users/{userId}/metadata
POST   /admin/users/{userId}:suspend
POST   /admin/users/{userId}:restore
PUT    /admin/users/{userId}/roles
GET    /admin/dashboard

POST   /admin/knowledge-items
GET    /admin/knowledge-items/{itemId}/versions/{version}
POST   /admin/knowledge-items/{itemId}/versions
PATCH  /admin/knowledge-items/{itemId}/versions/{version}
POST   /admin/knowledge-items/{itemId}/versions/{version}:submit
POST   /admin/knowledge-items/{itemId}/versions/{version}:approve
POST   /admin/knowledge-items/{itemId}/versions/{version}:reject
POST   /admin/knowledge-items/{itemId}/versions/{version}:archive
POST   /admin/journal-prompts
PATCH  /admin/journal-prompts/{promptId}
POST   /admin/journal-prompts/{promptId}:publish

GET    /admin/ai-jobs
POST   /admin/ai-jobs/{jobId}:retry
GET    /admin/safety/metrics
PUT    /admin/safety/resources/{resourceId}
GET    /admin/audit-logs
GET    /admin/feedback
GET    /admin/feedback/{feedbackId}
PATCH  /admin/feedback/{feedbackId}
```

M7 lưu artifact export mã hóa trong PostgreSQL theo ADR-0007 (tối đa 5 MB, TTL 24 giờ). Link tải tương đối có chữ ký 60 giây, vẫn cần header Bearer JWT hợp lệ và owner check; frontend tải bằng authenticated fetch rồi lưu blob, không mở link trần trong trình duyệt. API cấp link yêu cầu xác thực lại mật khẩu. Xóa tài khoản có grace period 7 ngày, hủy bằng email/mật khẩu và request ID vì session đã bị thu hồi. Worker dọn Cloudinary trước khi xóa hàng `users` theo cascade; khi Cloudinary không khả dụng mà còn asset, job giữ trạng thái retry. Feedback không tự đính kèm journal, message mã hóa và quyền admin `feedback:read`/`feedback:manage` được audit. PATCH admin feedback yêu cầu `If-Match` với `version` hiện tại, xung đột trả 409.

Không có admin endpoint trả plaintext journal. Metadata chỉ gồm ID giảm định danh, status, timestamp, size và job state cần cho vận hành.
M6 có user endpoint `GET /recommendations?topicCode=...` trả approved excerpts và citation, không sinh phản hồi AI.

### 9.3 Pagination và lỗi

Page response:

```json
{
  "items": [],
  "nextCursor": "opaque-token",
  "hasMore": false
}
```

Lỗi dùng `application/problem+json`:

```json
{
  "type": "https://mylog.app/problems/validation-error",
  "title": "Dữ liệu không hợp lệ",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "traceId": "01J...",
  "errors": [{ "field": "moodScore", "message": "must be between 1 and 10" }]
}
```

Không trả stack trace, SQL, provider response hay journal content. Request tạo resource/job nhận `Idempotency-Key`. Update dùng `If-Match`/version, conflict trả `409`.

## 10. Security và privacy

### 10.1 Auth

- Access JWT sống ngắn; refresh token rotation + reuse detection.
- Refresh token lưu hash, gắn session/device và revoke được.
- Password dùng Argon2id hoặc BCrypt với cost đã benchmark.
- RBAC cho quyền hệ thống; ownership check cho mọi resource cá nhân.
- Role gợi ý: `USER`, `ADMIN_SUPPORT`, `ADMIN_CONTENT`, `ADMIN_SAFETY`, `ADMIN_SYSTEM`, `AUDITOR`.
- Tránh một role `ADMIN` toàn quyền; admin nhạy cảm cần MFA ở production.

### 10.2 Bảo vệ nội dung

- TLS khi truyền và encryption-at-rest của hạ tầng.
- Title/content/caption/location nhạy cảm mã hóa cấp ứng dụng bằng envelope encryption AES-GCM.
- Khóa dữ liệu không nằm trong DB; production dùng KMS/secret manager và `keyVersion` để rotate.
- AAD gồm tối thiểu `userId + entryId + fieldName` để chống tráo ciphertext.
- Không cache raw content lâu hơn request/job.
- Ảnh journal lưu trên Cloudinary với signed delivery URL ngắn hạn; kiểm tra MIME/size/malware và không dùng unsigned upload preset.

### 10.3 Logging/audit

Được log: trace ID, route template, status, latency, job ID, provider/model, token count và error code đã sanitize.

Cấm log: title/content, prompt đầy đủ có dữ liệu user, token/password/API key, raw AI request/response, email/location/signed URL.

Audit lưu `actorId`, `action`, `targetType`, `targetId`, `reason`, `timestamp`, `traceId` và metadata diff an toàn. Audit không có CRUD sửa/xóa thông thường.

### 10.4 Consent/retention

- Consent version hóa riêng cho AI processing, optional analytics và future model training.
- Không dùng journal để train nếu chưa opt-in rõ ràng.
- Safety event, temporary AI payload, export, account deletion và backup có retention riêng.
- Admin analytics chỉ dùng aggregate và minimum cohort size.

## 11. Safety pipeline

Gợi ý khi đang viết là use case riêng với phân tích bài đã lưu. API `POST /api/v1/journal-writing-suggestions` (JWT, plain text tối đa 4.000 ký tự, 20 lần/15 phút/user) kiểm tra consent `AI_PROCESSING` và dùng `SafetyScreeningUseCase` trước khi trả câu hỏi mẫu do backend sở hữu. Nó không lưu draft, không tạo outbox/job và mặc định tắt bằng `MYLOG_WRITING_SUGGESTIONS_ENABLED=false`. Frontend login/journal còn mock nên chưa gọi API này; FE không tự quyết định safety hoặc nội dung gợi ý. Xem [AI_ANALYSIS_GUIDE.md](AI_ANALYSIS_GUIDE.md) cho trạng thái triển khai.

```text
Input validation
   ↓
Deterministic rules / curated phrases
   ↓
Risk classifier
   ↓
Versioned policy engine
   ├── NORMAL/LOW ──► standard analysis
   ├── MODERATE ─────► constrained response + configured policy
   └── HIGH/CRITICAL ► approved safety response, no normal reflection
                                ↓
                    locale-aware support resources
```

Yêu cầu:

- Fail safe: classifier lỗi/timeout thì không gọi generative reflection cho entry chưa screen; journal vẫn lưu.
- Lưu rule/classifier/policy version cùng decision.
- Safety response lấy từ nội dung duyệt trước; LLM không tự bịa hotline.
- Output LLM cũng được validate để chặn chẩn đoán, phán xét, hướng dẫn gây hại và khẳng định quá mức.
- Regression suite có tiếng Việt, tiếng lóng, phủ định, trích dẫn, mỉa mai và false positive.
- UI nói rõ đây không phải dịch vụ khẩn cấp. Liên hệ bên thứ ba cần consent, legal review và vận hành thật.

## 12. AI và RAG

Xem [hướng dẫn AI/phân tích](AI_ANALYSIS_GUIDE.md) để đọc luồng code hiện tại, vòng đời outbox/job, safety gate và các phần chưa sẵn sàng cho production.

### 12.1 Ports

```java
public interface JournalAnalyzer {
    Result analyze(String title, String plainText);
}

public interface EmbeddingProvider {
    float[] embed(String text);
}

public interface KnowledgeRetriever {
    List<Passage> retrieve(String topicCode, String locale, int limit);
}
```

Provider adapter nằm trong infrastructure. Đổi model/provider không làm đổi domain/controller.

M3 có `FakeJournalAnalyzer` chỉ khi bật rõ trong local/test; mặc định provider unavailable và
worker retry rồi đưa job vào DEAD mà không mất journal. ADR-0009 bổ sung HTTP adapter cho
classifier nội bộ và Chat Completions adapter cho `JournalAnalyzer`, đều tắt theo mặc định.
`MYLOG_JOBS_ENABLED` mặc định false; V8 đã áp dụng lên Supabase nhưng worker chỉ nên bật khi
môi trường, safety policy, output eval và điều kiện dữ liệu/provider theo ADR-0004 được duyệt.
`MODERATE/CONSTRAIN` hiện bị chặn khỏi reflection thông thường cho tới khi có luồng riêng.
`EmbeddingProvider` chưa có adapter được duyệt.
M6 đã có `KnowledgeRetriever` chỉ đọc chunk của version `APPROVED`, đúng locale và còn hiệu lực;
recommendation API hiện trả nguyên văn excerpt kèm citation, chưa dùng LLM.
Không đưa journal text vào retrieval request.

### 12.2 Structured output/provenance

AI result phải validate schema và lưu:

- provider/model/model version;
- prompt template và safety policy version;
- input content version, locale;
- latency, token usage, estimated cost;
- schema validation state;
- citation tới knowledge version/chunk khi dùng RAG.

Không lưu chain-of-thought. Chỉ lưu structured output và giải thích ngắn cho người dùng.

### 12.3 RAG workflow

```text
Admin draft → review → approve → chunk → embed → publish
                                        ↓
User context → retrieve approved chunks → LLM → output safety validation
```

- Chunk thuộc version bất biến.
- Embedding có model/version/dimension; đổi model tạo index/version mới.
- Filter bắt buộc `APPROVED`, locale và effective date.
- Archive ngăn dùng cho response mới nhưng citation lịch sử vẫn truy vết được.

## 13. Jobs và reliability

MVP dùng database-backed job/outbox. Redis không làm queue duy nhất nếu mất job là không chấp nhận được.

```text
id, type, aggregateId, userId, status, attempt,
availableAt, lockedAt, lockedBy, lastErrorCode,
idempotencyKey, payloadVersion, createdAt, finishedAt
```

- Claim bằng lock có timeout; worker chết thì job được reclaim.
- Handler idempotent, có unique business key.
- Retry lỗi tạm thời; validation/policy error đi `DEAD`.
- Exponential backoff có jitter.
- Dead job cho phép admin retry sau khi xử lý nguyên nhân.
- Outbox chỉ chứa ID/version, không chứa raw journal.
- Metrics: queue depth, oldest age, success/error/timeout, provider cost.

Khi tải lớn, thay adapter bằng RabbitMQ/Kafka/SQS mà không đổi use case.

## 14. Cache

Chỉ cache published prompts, approved safety resources, public system config và dashboard aggregate ngắn hạn theo `userId + range + dataVersion`.

Không cache raw journal. Cache key không chứa email/text; dùng TTL ngắn, private network và invalidate khi dữ liệu nguồn đổi.

## 15. Testing

```text
Unit             domain policy, value object, application use case
Slice            controller/security, JPA query, JSON schema
Integration      PostgreSQL/pgvector/Redis bằng Testcontainers
Contract         public API và AI provider adapter
Architecture     package dependency bằng ArchUnit
Safety eval      curated Vietnamese corpus + regression cases
End-to-end       register → journal → analysis → dashboard → export/delete
```

- Không mock domain; application test mock port.
- Query quan trọng test trên PostgreSQL thật, không H2.
- Inject Clock, UUID generator và provider để deterministic.
- Mỗi bug privacy/authorization/safety thêm regression test.
- Fixture chỉ dùng synthetic data.

ArchUnit kiểm tra:

- domain không phụ thuộc Spring/JPA/web;
- api không gọi repository;
- module không import infrastructure của module khác;
- controller không trả JPA entity;
- admin không truy cập plaintext journal adapter.

## 16. Configuration

`application.yml` chỉ chứa default không bí mật. Biến môi trường:

```text
MYLOG_DB_URL
MYLOG_DB_USERNAME
MYLOG_DB_PASSWORD
MYLOG_REDIS_URL
MYLOG_JWT_ISSUER
MYLOG_JWT_PRIVATE_KEY
MYLOG_ENCRYPTION_MASTER_KEY_REF
MYLOG_AI_PROVIDER
MYLOG_AI_API_KEY
MYLOG_CLOUDINARY_ENABLED
MYLOG_CLOUDINARY_CLOUD_NAME
MYLOG_CLOUDINARY_API_KEY
MYLOG_CLOUDINARY_API_SECRET
MYLOG_CLOUDINARY_FOLDER
MYLOG_ALLOWED_ORIGINS
MYLOG_APP_PROFILE=api|worker|all
```

Không commit `.env`, private key hay provider key. Startup fail rõ nếu thiếu secret bắt buộc; không fallback sang secret hard-code.

Môi trường:

- `local`: Docker Compose, mail sink, optional stub AI.
- `test`: Testcontainers, fixed clock, fake provider.
- `staging`: synthetic data, topology giống production.
- `prod`: Supabase PostgreSQL, Cloudinary cho ảnh, managed secrets/KMS, backup, alert, least privilege.

## 17. Observability/vận hành

- Actuator chỉ public liveness/readiness; endpoint chi tiết bảo vệ bằng auth/network.
- JSON logs có `traceId`, `requestId`, `jobId`.
- Metrics: HTTP, DB pool, queue age, AI latency/error/token/cost, aggregate safety decision.
- Trace xuyên API → job → provider nhưng không gắn raw content vào span.
- Alert: safety dependency unavailable, oldest job quá ngưỡng, analysis failure spike, auth anomaly, deletion overdue.
- Backup PostgreSQL mã hóa và kiểm thử restore định kỳ.

## 18. Quy ước code

- Java 21, Spring Boot 4.1.x, Maven Wrapper.
- Constructor injection; không field injection.
- `record` cho immutable DTO/command nhỏ.
- Không dùng `Optional` cho entity/request field; dùng có chủ đích ở return type.
- Exception có error code ổn định; map tập trung.
- Business method không nhận servlet/security context/JPA entity.
- `userId` truyền tường minh; repository dữ liệu cá nhân bắt buộc filter owner.
- Cursor pagination cho journal/audit/job.
- Flyway migration immutable sau merge/deploy; sửa bằng migration mới.
- Breaking API tạo `/v2`; additive change giữ `/v1`.
- Comment giải thích “vì sao”; tên code giải thích “làm gì”.

Dependency mục tiêu: Spring Web, Validation, Security, OAuth2 Resource Server, Data JPA, PostgreSQL, Flyway, Redis, Micrometer/OpenTelemetry, Actuator, springdoc-openapi, Testcontainers, JUnit 5, Mockito, AssertJ, ArchUnit và circuit breaker/timeout cho provider.

## 19. Lộ trình

### Phase 0 — Foundation

- Bootstrap Spring Boot, profiles, Compose, PostgreSQL, Flyway.
- Error contract, log redaction, security baseline, CI.
- ArchUnit và test support.

### Phase 1 — Core journal

- Identity/session và profile.
- Journal CRUD, check-in, tag, ownership, encryption.
- Nối frontend thay `localStorage`.

### Phase 2 — Safety và AI

- Safety policy/rule/classifier adapter.
- Outbox worker, analysis schema, retry và stale version.
- Reflection qua output safety validation.

### Phase 3 — Insight/report

- Aggregate, streak, trend và evidence.
- Weekly report versioned; correlation có minimum sample.

### Phase 4 — Self-care, RAG, admin

- Goal/completion.
- Knowledge workflow, pgvector retrieval, citation.
- Admin least privilege, audit, operational dashboard.

### Phase 5 — Data rights/hardening

- CSV/PDF export, account deletion.
- Load test, restore drill, safety eval, privacy review.

## 20. Definition of Done

Một backend feature chỉ hoàn thành khi:

- ownership/authorization và use case rõ;
- có migration và chiến lược vận hành;
- validation, stable error code và OpenAPI được cập nhật;
- không log dữ liệu nhạy cảm;
- unit/integration/authorization test đạt;
- job idempotent, retry được, có metrics;
- admin action nhạy cảm có audit;
- tài liệu liên quan được cập nhật;
- đã kiểm tra khi DB, Redis hoặc AI provider unavailable.

## 21. ADR cần chốt trước production code

1. Tự quản lý identity hay external IdP.
2. KMS/master key theo môi trường.
3. Safety classifier, threshold và người duyệt policy.
4. AI provider/model, provider retention và vùng xử lý dữ liệu.
5. Object storage và thời hạn ảnh/export.
6. Quốc gia/locale hỗ trợ cho crisis resources.
7. Retention cho journal đã xóa, safety event, audit và backup.

Các quyết định này ảnh hưởng trực tiếp đến privacy/safety và phải nằm trong ADR, không được ẩn trong code hoặc `.env`.
