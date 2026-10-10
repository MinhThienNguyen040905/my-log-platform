# Giải thích từng bảng trong database mylog

> Phạm vi: **43 bảng nghiệp vụ được tạo bởi Flyway V2–V14, với trạng thái cuối sau V15**. Đối chiếu repository ngày 2026-10-07. V1 chỉ bật extension `vector` và `pgcrypto`; V15 thu hồi quyền truy cập trực tiếp và bật RLS cho các bảng `public`, không tạo bảng nghiệp vụ mới. `flyway_schema_history` do Flyway quản lý nên không tính vào 43 bảng dưới đây.

Đây là tài liệu **giải thích schema đang có**, không phải đề xuất thêm bảng hoặc hướng dẫn truy vấn trực tiếp từ frontend. Khi tài liệu khác với migration, migration là nguồn thực thi. Xem [DATABASE_OVERVIEW.md](DATABASE_OVERVIEW.md) để đọc nguyên tắc thiết kế, [DBML](database/mylog.dbml) để dựng sơ đồ và [use case](USE_CASE_CATALOG.md) để biết bảng phục vụ chức năng nào.

## 1. Cách đọc schema trước khi xem từng bảng

- **PK** là khóa chính; **FK** là khóa ngoại. Phần lớn bản ghi có `id UUID` do application sinh. Bảng nối thuần dùng khóa ghép. `user_id` xác định chủ sở hữu, nhưng một FK tới `users` **không thay thế** điều kiện `user_id = currentUserId` trong query backend.
- Hậu tố `encrypted_*` là ciphertext, đi kèm `*_iv`, `*_wrapped_key`, `*_key_version`. Backend dùng envelope encryption; dữ liệu số có cấu trúc như mood/stress thường lưu dạng số để tổng hợp, vẫn phải owner-scope. Hash lookup email/tag dùng HMAC có khóa, không khôi phục được nguyên văn.
- `created_at`/`updated_at` là thời điểm UTC; `local_date` và `timezone` lưu ngày theo người dùng. `row_version` dùng chống ghi đè đồng thời; `content_version` của journal xác định **phiên bản nội dung** mà AI được phép phân tích. Hai version này không cùng mục đích.
- `status` mô tả vòng đời từng bảng; không suy ra trạng thái của bảng khác chỉ từ một giá trị. JSONB dùng cho payload/snapshot có cấu trúc đã giới hạn, không phải nơi cất văn bản nhật ký thô. Các trường `*_id` dạng tham chiếu nghiệp vụ không phải lúc nào cũng có FK vì một số evidence/outbox dùng tham chiếu đa loại.
- `ON DELETE CASCADE` xóa bản ghi con khi xóa vật lý bản ghi cha; `ON DELETE SET NULL` giữ metadata nhưng bỏ liên kết người dùng. Xóa tài khoản còn cần worker dọn Cloudinary/cache/provider và chính sách retention, không thể chỉ dựa vào cascade.

| Nhóm | Bảng | Migration tạo bảng |
|---|---:|---|
| Tài khoản, RBAC, hồ sơ, audit | 12 | V2–V3; V4 đổi PK lịch sử refresh |
| Nhật ký, tag, asset, check-in | 6 | V5 |
| Outbox và idempotency | 2 | V6; V8 thêm lease cho outbox |
| Safety | 3 | V7 |
| AI job, kết quả và usage | 5 | V8 |
| Insight và report | 4 | V9 |
| Self-care | 3 | V10 |
| Knowledge và prompt | 5 | V11; V13 điều chỉnh trigger |
| Export, xóa tài khoản, feedback | 3 | V14 |
| **Tổng** | **43** | V15 áp chính sách quyền/RLS |

## 2. Tài khoản, quyền, hồ sơ và audit — 12 bảng

Nguồn schema: [V2](../src/main/resources/db/migration/V2__identity_and_rbac.sql), [V3](../src/main/resources/db/migration/V3__user_profile_and_consent.sql), [V4](../src/main/resources/db/migration/V4__auth_refresh_history_id.sql); V12/V14 bổ sung role và permission.

### 2.1 `users` — danh tính và trạng thái tài khoản

- **Khóa/quan hệ:** `id` là PK; nhiều bảng cá nhân trỏ tới nó. `email_lookup_hash` có unique index **chỉ với tài khoản `deleted_at IS NULL`**, cho phép kiểm tra email đang dùng mà không lưu email plaintext để tìm kiếm.
- **Cột quan trọng:** nhóm `encrypted_email/email_iv/email_wrapped_key/email_key_version` chứa email mã hóa; `password_hash` chứa BCrypt hash; `auth_provider` hiện chỉ cho `LOCAL`. `status` có `PENDING`, `ACTIVE`, `SUSPENDED`, `DELETION_PENDING`, `DELETED`; `failed_login_count`, `locked_until` phục vụ chống thử mật khẩu; `row_version` chống ghi đè.
- **Cách dùng:** đăng ký/đăng nhập/đình chỉ/xóa tài khoản. Không dùng `email_lookup_hash` như email hiển thị; không cho admin thường đọc/decrypt email hoặc nhật ký chỉ nhờ quyền xem metadata.

### 2.2 `roles` — vai trò

- **Khóa/quan hệ:** PK `id`, `code` unique; nối tới user bằng `user_roles` và permission bằng `role_permissions`.
- **Cột quan trọng:** `code`, `name`, `description`, `system_role`. V2 seed `USER`; V12 seed các role nội dung, safety, system, support và auditor.
- **Cách dùng:** nhóm quyền để cấp cho tài khoản; role không tự chứng minh người gọi sở hữu tài nguyên cá nhân.

### 2.3 `permissions` — quyền thao tác cụ thể

- **Khóa/quan hệ:** PK `id`, `code` unique; được gán cho role qua `role_permissions`.
- **Cột quan trọng:** `code` như `knowledge:review`, `jobs:retry`, `feedback:read`; `description` giải thích phạm vi.
- **Cách dùng:** `@PreAuthorize` và policy backend kiểm tra hành động admin; quyền mới cần seed/migration và test allow/deny.

### 2.4 `role_permissions` — role có quyền nào

- **Khóa/quan hệ:** PK ghép `(role_id, permission_id)`; cả hai FK cascade tới bảng danh mục.
- **Cột quan trọng:** `created_at` là lúc gán quyền; một cặp chỉ có một dòng.
- **Cách dùng:** xây ma trận RBAC. V12/V14 thêm liên kết quyền admin và feedback; tránh hardcode rằng mọi admin có mọi permission.

### 2.5 `user_roles` — gán role cho user

- **Khóa/quan hệ:** PK ghép `(user_id, role_id)`; `user_id` cascade khi xóa user, `assigned_by` có thể null sau khi người gán bị xóa.
- **Cột quan trọng:** `assigned_at`, `expires_at` thể hiện thời hạn gán; index theo `role_id` hỗ trợ tra cứu.
- **Cách dùng:** lấy authorities khi xác thực. Có schema/RBAC nhưng API tự phục vụ việc gán role vẫn là roadmap; không sửa bảng trực tiếp từ frontend.

### 2.6 `auth_sessions` — phiên và refresh-token family

- **Khóa/quan hệ:** PK `id`, FK `user_id`; `current_token_hash` unique. `token_family_id` nhóm các lần refresh của cùng family.
- **Cột quan trọng:** `expires_at`, `revoked_at/revoke_reason`, `last_used_at`; `device_name`, `user_agent_hash`, `ip_prefix` chỉ là metadata hạn chế, không phải raw token.
- **Cách dùng:** đăng nhập, refresh rotation, đăng xuất một/all sessions, phát hiện token reuse và thu hồi. Access JWT không được lưu nguyên văn tại đây.

### 2.7 `auth_refresh_history` — token refresh đã dùng

- **Khóa/quan hệ:** sau V4, PK là `id UUID`, `token_hash` vẫn unique; `session_id` FK cascade tới `auth_sessions`.
- **Cột quan trọng:** `used_at` ghi thời điểm token cũ được tiêu thụ.
- **Cách dùng:** nhận ra token refresh bị dùng lại sau rotation; không chứa token plaintext. V2 ban đầu dùng `token_hash` làm PK, nên phải đọc V4 khi mô tả schema cuối.

### 2.8 `auth_action_tokens` — token cho hành động tài khoản

- **Khóa/quan hệ:** PK `id`, FK `user_id`, `token_hash` unique.
- **Cột quan trọng:** `purpose` chỉ `VERIFY_EMAIL`/`RESET_PASSWORD`, cùng `expires_at`, `consumed_at`.
- **Cách dùng:** backend đã dùng luồng xác minh email; schema có chỗ cho reset password nhưng **không có nghĩa API reset password đã hoàn thành**. Token thô không lưu trong DB.

### 2.9 `auth_rate_limits` — bộ đếm chống lạm dụng

- **Khóa/quan hệ:** PK `subject_hash`; không FK tới `users` vì subject có thể là định danh đã pseudonym hóa từ IP/email trước khi có tài khoản.
- **Cột quan trọng:** `attempts`, `window_started_at`, `blocked_until`.
- **Cách dùng:** chặn tạm các hành động xác thực vượt ngưỡng; không lưu email/IP thô làm khóa.

### 2.10 `audit_logs` — dấu vết hành động nhạy cảm

- **Khóa/quan hệ:** PK `id`; `actor_user_id` FK `ON DELETE SET NULL`, `target_type/target_id` là tham chiếu nghiệp vụ không phải FK đa hình.
- **Cột quan trọng:** `actor_type`, `action`, `reason_code`, `trace_id`, `occurred_at`, `safe_metadata JSONB`.
- **Cách dùng:** audit thu hồi phiên, duyệt nội dung, đình chỉ tài khoản, feedback... Chỉ metadata allowlist; **không** ghi journal, email, token, prompt hay raw provider response.

### 2.11 `user_profiles` — thông tin hiển thị và tùy chọn

- **Khóa/quan hệ:** `user_id` vừa là PK vừa FK tới `users`: quan hệ một–một, xóa cascade.
- **Cột quan trọng:** `encrypted_profile` và bộ IV/wrapped key/version; `timezone`, `locale` để tính ngày/ngôn ngữ; `onboarding_completed_at`, `preferred_journal_time`, `row_version`.
- **Cách dùng:** onboarding, tên/pen name, múi giờ, locale. Trường profile mã hóa không thể query full-text trong PostgreSQL; sửa đồng thời cần `If-Match`.

### 2.12 `user_consents` — lịch sử quyết định đồng ý

- **Khóa/quan hệ:** PK `id`, FK `user_id`; index `(user_id, consent_type, decided_at DESC)` để lấy quyết định mới nhất.
- **Cột quan trọng:** `consent_type` gồm `TERMS`, `PRIVACY`, `AI_PROCESSING`, `ANALYTICS`, `MODEL_TRAINING`; `document_version`, `granted`, `decided_at`, `source`.
- **Cách dùng:** lưu cả cấp và rút consent thay vì ghi đè một boolean. Worker phải kiểm tra `AI_PROCESSING` lúc **thực thi**; consent này không cấp quyền train model.

## 3. Nhật ký, ảnh và check-in — 6 bảng

Nguồn schema: [V5](../src/main/resources/db/migration/V5__journal_and_checkin.sql); V8 thêm FK `latest_analysis_id`.

### 3.1 `journal_entries` — bài nhật ký chính

- **Khóa/quan hệ:** PK `id`, FK `user_id`; `latest_analysis_id` là FK tùy chọn tới `ai_analyses` được thêm ở V8. Một user có nhiều bài; một bài có nhiều tag/asset/analysis theo lịch sử.
- **Cột quan trọng:** `encrypted_payload` cùng IV/wrapped key/key version giữ title/nội dung/location; `occurred_at`, `local_date`, `timezone` xác định thời điểm. `mood_code`, mood/stress/energy 1–10 và sleep 0–1440 phút là metrics có cấu trúc. `favorite`, `entry_status`, `risk_level`, `analysis_status`, `content_version`, `row_version`, `deleted_at` điều khiển vòng đời.
- **Cách dùng:** lưu độc lập với AI. `content_version` tăng khi dữ liệu ảnh hưởng phân tích đổi; kết quả cũ không active cho bản mới. Query user flow lọc `id + user_id + deleted_at IS NULL`. Soft-delete chưa phải xóa vật lý; purge/asset cleanup xử lý sau.

### 3.2 `journal_tags` — nhãn cá nhân

- **Khóa/quan hệ:** PK `id`, FK `user_id`; `UNIQUE(user_id, name_lookup_hash)` chống tên tag trùng theo cùng chủ sở hữu.
- **Cột quan trọng:** tên tag mã hóa với IV/wrapped key/version, `name_lookup_hash` phục vụ so sánh bằng HMAC; `color` có CHECK mã màu hex.
- **Cách dùng:** lọc/nhóm bài mà không lưu plaintext tên tag. Adapter phải kiểm tra cùng owner khi gắn tag vào bài.

### 3.3 `journal_entry_tags` — liên kết bài và tag

- **Khóa/quan hệ:** PK ghép `(journal_entry_id, tag_id)`, cả hai FK cascade; index `tag_id` giúp tìm bài theo tag.
- **Cột quan trọng:** `created_at` là lúc gắn; không lưu bản sao tên tag.
- **Cách dùng:** quan hệ nhiều–nhiều. Hai FK riêng lẻ **không tự đảm bảo** entry và tag thuộc cùng user; application bắt buộc kiểm tra ownership trước khi insert.

### 3.4 `journal_assets` — metadata ảnh/tệp Cloudinary

- **Khóa/quan hệ:** PK `id`, FK tới `journal_entries` và `users`; `provider_asset_id`/`public_id` unique.
- **Cột quan trọng:** `provider_version`, `delivery_type` `AUTHENTICATED/PRIVATE`, `asset_type`, MIME, size, SHA-256, width/height, status quét, caption mã hóa và `deleted_at`.
- **Cách dùng:** DB lưu metadata, **không lưu bytes ảnh**. Thiết kế Cloudinary được chốt nhưng workflow upload ký, kiểm MIME/size/checksum/malware chưa hoàn tất; có bảng không chứng minh upload production đã chạy. Trước hard-delete entry phải dọn object liên quan.

### 3.5 `daily_checkins` — một điểm check-in cho mỗi ngày

- **Khóa/quan hệ:** PK `id`, FK `user_id`, `UNIQUE(user_id, local_date)` bảo đảm một dòng/user/ngày.
- **Cột quan trọng:** timezone, mood/stress/energy/sleep, note mã hóa tùy chọn, `source` (`USER`, `JOURNAL_FALLBACK`, `IMPORT`), `row_version`. CHECK bảo đảm hoặc đủ cả bộ cột mã hóa note, hoặc tất cả null.
- **Cách dùng:** nguồn chính của dashboard ngày. Nếu không có check-in user, journal observation gần nhất có thể làm fallback **kèm source marker**, không tạo hai điểm cho cùng ngày.

### 3.6 `checkin_activities` — hoạt động đi kèm check-in

- **Khóa/quan hệ:** PK `id`, FK `checkin_id` cascade, `UNIQUE(checkin_id, activity_code)`.
- **Cột quan trọng:** `activity_code`, `duration_minutes` 0–1440, `intensity` `LOW/MODERATE/HIGH`.
- **Cách dùng:** biểu diễn hoạt động có cấu trúc thay vì nhét vào văn bản note; hỗ trợ xem lại cùng check-in.

## 4. Độ tin cậy của thao tác và safety — 5 bảng

Nguồn schema: [V6](../src/main/resources/db/migration/V6__platform_outbox_idempotency_audit.sql), [V7](../src/main/resources/db/migration/V7__safety.sql), [V8](../src/main/resources/db/migration/V8__ai_analysis_and_jobs.sql) cho lease của outbox.

### 4.1 `outbox_events` — yêu cầu việc nền đã commit

- **Khóa/quan hệ:** PK `id`; `aggregate_type + aggregate_id` tham chiếu tài nguyên nghiệp vụ, không FK đa hình. Index `available_at/created_at` cho claim và `lease_expires_at` cho recovery.
- **Cột quan trọng:** `event_type`, `event_version`, `payload JSONB`, status `PENDING/PROCESSING/PUBLISHED/FAILED/DEAD`, attempt, available/locked/lease, `last_error_code`.
- **Cách dùng:** insert cùng transaction lưu/sửa/xóa journal để worker không quên việc sau commit. Payload chỉ mang ID/version, không mang text nhật ký; retry và lease cho phép chạy lại idempotent.

### 4.2 `idempotency_keys` — chống tạo trùng do request lặp

- **Khóa/quan hệ:** PK `id`, FK `user_id`; `UNIQUE(user_id, operation, idempotency_key)`.
- **Cột quan trọng:** `request_hash`, response status/reference, state `PROCESSING/COMPLETED/FAILED`, expiry và timestamps.
- **Cách dùng:** ví dụ client retry `POST /journal-entries` với cùng key không tạo thêm bài. Cùng key nhưng nội dung khác phải bị phát hiện; bản ghi hết hạn cần dọn theo policy.

### 4.3 `safety_policy_versions` — phiên bản quyết định safety

- **Khóa/quan hệ:** PK `id`, `version` unique; `approved_by` FK `ON DELETE SET NULL`.
- **Cột quan trọng:** status `DRAFT/APPROVED/RETIRED`, `config JSONB`, thời điểm duyệt và cửa sổ hiệu lực.
- **Cách dùng:** policy gate chỉ mở ordinary analysis khi policy được duyệt, còn hiệu lực và khớp rule/classifier/version/confidence. Bảng tồn tại không có nghĩa đã có policy production được duyệt.

### 4.4 `safety_resources` — nguồn hỗ trợ được biên tập

- **Khóa/quan hệ:** PK `id`, index `(locale, country_code, status)`; không FK user vì đây là nội dung dùng chung.
- **Cột quan trọng:** loại, tên, contact, mô tả, source URL, `verified_at`, `status`, `version`.
- **Cách dùng:** API công khai chỉ trả nguồn `APPROVED`, được xác minh và có HTTPS source phù hợp locale/country. Chưa có nguồn thật được duyệt trong DB; model không tự sáng tác hotline.

### 4.5 `safety_events` — dấu vết screening tối thiểu

- **Khóa/quan hệ:** PK `id`; `user_id` và `journal_entry_id` FK `ON DELETE SET NULL` để có thể bỏ liên kết sau xóa, `subject_ref` là tham chiếu giảm định danh.
- **Cột quan trọng:** `risk_level`, decision `ALLOW/CONSTRAIN/SAFETY_FLOW/FAIL_SAFE`, rule/classifier/policy version, confidence, resource-set version, thời điểm.
- **Cách dùng:** truy vết vì sao một bài được/chưa được phân tích mà **không lưu đoạn text kích hoạt**. Không dùng bảng này để tái tạo journal gốc hoặc làm dataset train tự động.

## 5. AI job, kết quả phân tích và chi phí — 5 bảng

Nguồn schema: [V8](../src/main/resources/db/migration/V8__ai_analysis_and_jobs.sql). Provider thực tế mặc định chưa bật/duyệt; bảng và adapter có thể tồn tại khi chưa có kết quả AI production.

### 5.1 `ai_jobs` — hàng công việc phân tích

- **Khóa/quan hệ:** PK `id`; `user_id` FK tùy chọn, `aggregate_type/aggregate_id` là tham chiếu nghiệp vụ; `idempotency_key` unique chặn tạo job trùng.
- **Cột quan trọng:** job type, priority, status `PENDING/RETRY_WAIT/PROCESSING/SUCCEEDED/FAILED/DEAD`, attempt/max attempts, available/locked/lease, payload version, error code/summary đã sanitize.
- **Cách dùng:** outbox worker tạo job; AI worker claim, retry với backoff và phục hồi lease. Payload chỉ có ID/version; không đặt prompt hay raw journal vào job row.

### 5.2 `ai_analyses` — kết quả cho một phiên bản bài

- **Khóa/quan hệ:** PK `id`, FK `journal_entry_id` và `user_id`; unique `(journal_entry_id, content_version, analysis_version)`, thêm partial unique một `SUCCEEDED` cho mỗi entry/content version.
- **Cột quan trọng:** status, sentiment label/score, `encrypted_output` cùng bộ mã hóa cho reflection/narrative, schema version, provider/model/prompt/safety policy version, timestamps.
- **Cách dùng:** giữ provenance và lịch sử kết quả; `journal_entries.latest_analysis_id` chỉ trỏ bản active khi `content_version` còn khớp. Kết quả đến trễ có thể thành `STALE`; dữ liệu đã lưu không tự động được hiển thị nếu safety/consent không hợp lệ.

### 5.3 `analysis_emotions` — cảm xúc có cấu trúc

- **Khóa/quan hệ:** PK ghép `(analysis_id, emotion_code)`, FK cascade tới `ai_analyses`; unique `(analysis_id, rank)`.
- **Cột quan trọng:** mã cảm xúc, score 0–1, rank 1–5.
- **Cách dùng:** phục vụ hiển thị top emotions và aggregate; không chứa câu reflection. Chỉ tính từ analysis hiện hành/được phép hiển thị.

### 5.4 `analysis_topics` — chủ đề có cấu trúc

- **Khóa/quan hệ:** PK `id`, FK `analysis_id`; unique `(analysis_id, topic_code)` và `(analysis_id, rank)`.
- **Cột quan trọng:** `topic_code`, score 0–1, rank 1–5.
- **Cách dùng:** chủ đề phân tích cho dashboard/recommendation; khác `journal_tags` do người dùng tự gắn và khác metadata nguồn tri thức.

### 5.5 `ai_usage_records` — metadata sử dụng provider

- **Khóa/quan hệ:** PK `id`; FK `job_id`, `analysis_id` đều `ON DELETE SET NULL` để có thể giữ metadata giảm định danh theo retention.
- **Cột quan trọng:** provider/model/operation, input/output tokens, estimated USD cost, latency, request status và thời điểm; các số có CHECK không âm.
- **Cách dùng:** theo dõi chi phí/độ trễ mà không lưu raw prompt/response. **Bảng này có trong Flyway V8 nhưng hiện bị thiếu khỏi DBML**; không suy từ DBML rằng nó chưa tồn tại.

## 6. Insight và báo cáo — 4 bảng

Nguồn schema: [V9](../src/main/resources/db/migration/V9__insights_and_reports.sql).

### 6.1 `insights` — quan sát trên nhiều ngày

- **Khóa/quan hệ:** PK `id`, FK `user_id`; unique `(user_id, insight_type, period_start, period_end, algorithm_version)`.
- **Cột quan trọng:** khoảng ngày/timezone, status `ACTIVE/SUPERSEDED/HIDDEN`, direction/strength/sample size, algorithm version, `metrics_snapshot JSONB`, narrative mã hóa tùy chọn, `generated_by`.
- **Cách dùng:** hiện có insight quan hệ mood–sleep khi đủ mẫu; narrative diễn giải metric, không chứng minh nguyên nhân. Partial index chỉ ưu tiên bản `ACTIVE`; không lấy insight cũ làm quan sát hiện hành.

### 6.2 `insight_evidence` — số liệu hỗ trợ insight

- **Khóa/quan hệ:** PK `id`, FK `insight_id` cascade; `source_type/source_id` trỏ check-in/journal/metric theo kiểu đa nguồn, `source_id` không có FK đa hình.
- **Cột quan trọng:** evidence date, metric name/value, weight, metadata đã giới hạn.
- **Cách dùng:** cho phép truy ngược kết luận về số liệu/ngày mà không copy raw journal. Application phải bảo đảm source cùng owner và còn phù hợp.

### 6.3 `reports` — bản tổng kết tuần hoặc tháng

- **Khóa/quan hệ:** PK `id`, FK `user_id`; unique `(user_id, report_type, period_start, version)` giữ các lần regenerate riêng.
- **Cột quan trọng:** kỳ/timezone, status `PENDING/PROCESSING/READY/FAILED/DEAD`, sample size, snapshot, narrative mã hóa, `generated_by`, provenance AI nếu có, attempt/lease/error cho worker.
- **Cách dùng:** scheduler tạo report theo timezone, worker lưu snapshot bất biến. Hiện narrative là `RULE`, không phải LLM; `MYLOG_REPORTS_ENABLED` mặc định false.

### 6.4 `report_evidence` — bằng chứng đi cùng báo cáo

- **Khóa/quan hệ:** PK `id`, FK `report_id` cascade; `insight_id` tùy chọn `ON DELETE SET NULL`; source ID đa loại không có FK trực tiếp.
- **Cột quan trọng:** source type/date, metric name/value, metadata.
- **Cách dùng:** giải thích các số trong báo cáo, giữ bằng chứng khi insight tham chiếu bị gỡ; không chép nhật ký thô vào evidence.

## 7. Mục tiêu và thói quen tự chăm sóc — 3 bảng

Nguồn schema: [V10](../src/main/resources/db/migration/V10__selfcare.sql).

### 7.1 `selfcare_goals` — mục tiêu wellness

- **Khóa/quan hệ:** PK `id`, FK `user_id`, thêm `UNIQUE(id, user_id)` để bảng `habits` có thể dùng FK ghép xác nhận cùng owner.
- **Cột quan trọng:** category `SLEEP/MINDFULNESS/EXERCISE/SOCIAL/CUSTOM`, title/description mã hóa, status, ngày bắt đầu/đích, completion time, row version.
- **Cách dùng:** mục tiêu tự chăm sóc, không phải treatment plan; kiểm tra target date không trước start date và update theo optimistic locking.

### 7.2 `habits` — hành vi lặp lại thuộc mục tiêu

- **Khóa/quan hệ:** PK `id`, `user_id` FK; FK ghép `(goal_id, user_id)` tới `selfcare_goals` ngăn habit gắn goal của user khác.
- **Cột quan trọng:** title mã hóa, target value/unit, `frequency_type` `DAILY/WEEKLY`, `frequency_config JSONB`, timezone snapshot, status/version.
- **Cách dùng:** lịch thực hiện và progress; múi giờ chụp khi tạo habit, không tự đổi theo profile sau đó. API sửa habit chưa có dù bảng có `row_version`.

### 7.3 `habit_completions` — giá trị đã ghi vào một ngày

- **Khóa/quan hệ:** PK `id`; FK ghép `(habit_id, user_id)` tới `habits`, unique `(habit_id, local_date)`.
- **Cột quan trọng:** ngày, value > 0, source `USER/IMPORT`, timestamps.
- **Cách dùng:** `PUT` ghi/upsert idempotent, `DELETE` hoàn tác. Unique ngăn hai completion cho một habit/ngày; insight habit–mood vẫn cần minimum sample và không suy ra quan hệ nhân quả.

## 8. Kho kiến thức và câu hỏi viết — 5 bảng

Nguồn schema: [V11](../src/main/resources/db/migration/V11__knowledge_and_prompts.sql), [V13](../src/main/resources/db/migration/V13__knowledge_author_nullification.sql). Đây là **nội dung biên tập dùng chung**, khác dữ liệu journal cá nhân.

### 8.1 `knowledge_items` — định danh một tài liệu/chủ đề

- **Khóa/quan hệ:** PK `id`, `slug` unique; `created_by` FK `ON DELETE SET NULL`.
- **Cột quan trọng:** topic code, locale, source name/HTTPS URL, owner team, status `ACTIVE/ARCHIVED`, timestamps.
- **Cách dùng:** phần vỏ quản lý tài liệu; nội dung theo phiên bản ở `knowledge_versions`. Trigger giữ metadata bất biến khi đã có version được duyệt/archived.

### 8.2 `knowledge_versions` — nội dung và vòng đời duyệt

- **Khóa/quan hệ:** PK `id`, FK `item_id`, unique `(item_id, version)` và partial unique tối đa một version `APPROVED` mỗi item.
- **Cột quan trọng:** title/content plaintext của **tài liệu đã biên tập**, SHA-256, status `DRAFT/IN_REVIEW/APPROVED/REJECTED/ARCHIVED`, người tạo/duyệt, thời gian hiệu lực, chunk strategy.
- **Cách dùng:** chỉ version được duyệt và còn hiệu lực được retrieval; trigger khóa nội dung sau duyệt. V13 cho phép null attribution FK khi user bị xóa mà không thay nội dung đã duyệt.

### 8.3 `knowledge_chunks` — đoạn nhỏ của một version

- **Khóa/quan hệ:** PK `id`, FK `knowledge_version_id`, unique `(knowledge_version_id, chunk_index)`.
- **Cột quan trọng:** content đoạn, token count, metadata, vị trí đoạn.
- **Cách dùng:** cung cấp excerpt/citation; trigger chặn sửa/xóa chunk của approved/archived version. Không phải bản sao journal của người dùng.

### 8.4 `knowledge_embeddings` — vector cho tìm kiếm ngữ nghĩa

- **Khóa/quan hệ:** PK `id`, FK `chunk_id`; unique `(chunk_id, provider, model, model_version)`.
- **Cột quan trọng:** provider/model/version, `dimensions` 1–2000, `embedding VECTOR`; CHECK `vector_dims(embedding) = dimensions`.
- **Cách dùng:** schema đã sẵn cho semantic retrieval, nhưng embedding provider, chỉ mục/vector evaluation và luồng sinh câu trả lời chưa được duyệt/triển khai đầy đủ. Không coi mọi chunk đã có vector.

### 8.5 `journal_prompts` — câu hỏi mẫu do biên tập viên quản lý

- **Khóa/quan hệ:** PK `id`, `code` unique; người tạo/sửa là FK tùy chọn `ON DELETE SET NULL`.
- **Cột quan trọng:** locale, category, `prompt_text`, status `DRAFT/PUBLISHED/ARCHIVED`, display order và khoảng hiệu lực.
- **Cách dùng:** schema cho workflow câu hỏi đã duyệt; admin/user prompt API còn roadmap. Endpoint `journal-writing-suggestions` hiện chọn các câu **hardcode trong service**, không đọc bảng này.

## 9. Xuất dữ liệu, xóa tài khoản và phản hồi — 3 bảng

Nguồn schema: [V14](../src/main/resources/db/migration/V14__exports_deletion_feedback.sql).

### 9.1 `export_requests` — job và artifact CSV/PDF

- **Khóa/quan hệ:** PK `id`, FK `user_id`; partial unique chỉ một request `PENDING/PROCESSING/READY` trên mỗi user.
- **Cột quan trọng:** format/status, `encrypted_file` cùng IV/wrapped key/version, SHA-256, size, attempt/lease/expiry/error. CHECK bảo đảm hàng `READY` có đủ artifact và expiry.
- **Cách dùng:** worker tạo file mã hóa tạm trong PostgreSQL (không phải object Cloudinary); backend giới hạn 5 MB, TTL 24 giờ, xác thực lại trước cấp link tải có ký, vẫn kiểm JWT và owner.

### 9.2 `deletion_requests` — yêu cầu xóa có grace period

- **Khóa/quan hệ:** PK `id`; `user_id` FK `ON DELETE SET NULL` vì request/audit giảm định danh có thể còn sau khi xóa user, `subject_hash` giúp nhận diện giảm định danh.
- **Cột quan trọng:** status `GRACE_PERIOD/PROCESSING/COMPLETED/CANCELLED/FAILED`, requested/scheduled/completed/cancelled time, checkpoint, attempt/lease/error; partial unique ngăn hai yêu cầu active cùng user.
- **Cách dùng:** sau xác thực lại, thu hồi phiên và chờ grace period 7 ngày; worker xóa theo checkpoint, có thể retry hoặc hủy đúng điều kiện. Không báo đã xóa toàn bộ chỉ vì DB cascade xong; object/provider/cache và backup retention phải được xét riêng.

### 9.3 `feedback` — góp ý/bug report

- **Khóa/quan hệ:** PK `id`, FK `user_id` cascade; `assigned_to` FK `ON DELETE SET NULL`.
- **Cột quan trọng:** category `BUG/IDEA/OTHER`, status `OPEN/IN_PROGRESS/RESOLVED/CLOSED`, message mã hóa, người phụ trách, expiry và `row_version`.
- **Cách dùng:** user gửi feedback **không tự kèm journal**; người có `feedback:read/manage` xem/xử lý, update có `If-Match` và audit truy cập.

## 10. Ví dụ đường đi của một bài nhật ký qua các bảng

```text
users (người sở hữu)
  ├─ user_consents (quyết định AI_PROCESSING mới nhất)
  ├─ journal_entries (nội dung mã hóa + content_version)
  │    ├─ journal_entry_tags → journal_tags
  │    ├─ journal_assets (metadata ảnh; bytes nằm ở Cloudinary)
  │    ├─ safety_events (decision/provenance, không có đoạn text kích hoạt)
  │    └─ ai_analyses → analysis_emotions / analysis_topics
  └─ daily_checkins (điểm theo ngày)

journal_entries + safety_events + outbox_events: commit cùng transaction nếu event phù hợp
outbox_events → ai_jobs → ai_analyses: worker xử lý sau commit, theo ID/version
daily_checkins + journal_entries + active ai_analyses → insights/reports + evidence
```

1. `POST /journal-entries` tạo `journal_entries`; safety ghi `safety_events`. `ALLOW` tạo event phân tích ở `outbox_events`, `FAIL_SAFE` tạo event screen lại, `CONSTRAIN/SAFETY_FLOW` không enqueue reflection thông thường. Bài vẫn được lưu khi AI/classifier không sẵn sàng.
2. `OutboxWorker` tạo `ai_jobs` idempotent; `AiJobWorker` kiểm tra lại account, consent, safety và `content_version`, gọi provider ngoài transaction; chỉ kết quả hợp lệ mới ghi `ai_analyses` cùng emotion/topic. `latest_analysis_id` chỉ active cho version hiện tại.
3. Dashboard lấy check-in ưu tiên, journal observation làm fallback có nhãn; report/insight lưu snapshot và evidence thay vì chỉ một đoạn văn. Không mô tả correlation thành causation.

## 11. Quyền truy cập, khác biệt tài liệu và phạm vi chưa có bảng

- **Quyền DB:** [V15](../src/main/resources/db/migration/V15__restrict_direct_database_api_access.sql) thu hồi grant của `PUBLIC`, `anon`, `authenticated` trên `public` và bật RLS không policy cho các bảng ứng dụng. Backend dùng DB owner; mọi API cá nhân vẫn phải owner-scope. Supabase Data API không phải lối truy cập dữ liệu ứng dụng.
- **DBML chậm hơn Flyway:** DBML hiện có 42 bảng, thiếu `ai_usage_records`; Flyway tạo 43 bảng. `DATABASE_OVERVIEW.md` và tài liệu này mô tả usage table theo V8. Không dùng số bảng trong sơ đồ minh họa để suy ra schema triển khai.
- **Bảng trong blueprint khác:** `system_configs` được nhắc trong DATABASE_OVERVIEW nhưng **chưa có `CREATE TABLE system_configs`** trong V1–V15. Không coi là bảng đang chạy.
- **Ba đề xuất self-compassion:** Thư gửi tương lai, Micro-Wins Vault và Therapist Handout [mới là đề xuất](SELF_COMPASSION_FEATURE_PROPOSALS.md); chưa có bảng riêng hoặc migration. Không tự dùng `journal_entries` hay `reports` như kho dữ liệu mới nếu chưa chốt consent, mã hóa, retention, owner check, export/deletion và migration mới.
- **Migration email verification:** [V16](../src/main/resources/db/migration/V16__email_verification_code.sql) thêm `code_hash`, `code_expires_at`, `code_failed_attempts` cho `auth_action_tokens`; không thêm bảng. V1–V15 đã áp dụng và bất biến. Thay đổi schema tiếp theo cần V17 trở lên, cập nhật DBML/entity/test cùng lúc.
