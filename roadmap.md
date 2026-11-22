# Roadmap của mylog

Các giai đoạn dự án theo thứ tự triển khai. File này là bản tóm tắt cấp sản phẩm:

- Chi tiết task, exit criteria và checklist backend: [`backend/docs/BACKEND_DEVELOPMENT_PLAN.md`](backend/docs/BACKEND_DEVELOPMENT_PLAN.md) (milestone M0–M8).
- Hành vi và trạng thái từng tính năng: [`spec/mucluc.md`](spec/mucluc.md), nguồn sự thật duy nhất về feature.
- Lý do sản phẩm: [`mission.md`](mission.md). Lựa chọn kỹ thuật: [`tech-stack.md`](tech-stack.md).

Trạng thái chụp ngày **2026-11-20**, tổng hợp từ checklist trong kế hoạch backend và việc đối chiếu code khi điền spec. Khi một giai đoạn đổi trạng thái, cập nhật file này cùng spec liên quan.

## Thứ tự ưu tiên

1. Privacy và safety đi trước cá nhân hóa bằng AI.
2. Nhật ký hoạt động độc lập với AI.
3. Mỗi giai đoạn tạo ra một lát cắt dọc chạy và demo được.
4. Migration, API, code, test, spec và tài liệu hoàn thành cùng nhau.
5. Không thêm hạ tầng phức tạp (microservice, Kafka...) khi chưa có nhu cầu đo được.

## Sơ đồ phụ thuộc

```text
GĐ0 Nền tảng
  ↓
GĐ1 Tài khoản và hồ sơ
  ↓
GĐ2 Nhật ký, check-in, safety đầu vào ──────► GĐ5 Tự chăm sóc và companion
  ↓
GĐ3 Phân tích AI bất đồng bộ
  ├──────────────► GĐ4 Insight, dashboard, báo cáo
  └──────────────► GĐ6 Kho kiến thức, RAG, quản trị
                        ↓
                 GĐ7 Quyền đối với dữ liệu
                        ↓
                 GĐ8 Hoàn thiện client (web, mobile)
                        ↓
                 GĐ9 Gia cố và phát hành
                        ↓
                 GĐ10 Sau MVP
```

## Tổng quan

| GĐ | Tên | Milestone | Ưu tiên | Trạng thái |
|---|---|---|---|---|
| 0 | Nền tảng | M0 | P0 | ✅ Xong |
| 1 | Tài khoản và hồ sơ | M1 | P0 | ✅ Xong (mobile thiếu Google, đổi mật khẩu) |
| 2 | Nhật ký, check-in, safety đầu vào | M2 | P0 | 🟡 Phần lõi xong; còn nội dung safety và eval |
| 3 | Phân tích AI bất đồng bộ | M3 | P0 | 🟡 Có code, mặc định tắt; chờ duyệt provider |
| 4 | Insight, dashboard, báo cáo | M4 | P0/P1 | 🟡 Có code; narrative AI và kiểu suy nghĩ còn là thử nghiệm |
| 5 | Tự chăm sóc và companion | M5 | P1 | 🟡 Có code; companion mặc định tắt |
| 6 | Kho kiến thức, RAG, quản trị | M6 | P0/P1 | 🟡 Có code; nội dung kiến thức chưa duyệt |
| 7 | Quyền đối với dữ liệu | M7 | P0 | 🟡 Export, xóa, phản hồi có code; còn xóa dữ liệu ở dịch vụ ngoài |
| 8 | Hoàn thiện client | — | P0 | 🟡 Web gần đủ; mobile chưa chạy thiết bị thật |
| 9 | Gia cố và phát hành | M8 | P0 | 🔴 Chưa đạt release gate |
| 10 | Sau MVP | — | P2 | ⚪ Chưa bắt đầu |

✅ xong · 🟡 đang làm hoặc có code nhưng còn việc mở · 🔴 chưa đạt · ⚪ chưa bắt đầu

## Chi tiết từng giai đoạn

### GĐ0 — Nền tảng (M0) ✅

**Mục tiêu:** bộ khung an toàn để mọi feature dựa vào.

- Spring Boot 4.1, Java 21, Docker Compose (PostgreSQL/pgvector, Redis, Mailpit), profile `local`/`test`/`prod`.
- Security mặc định chặn, error contract `problem+json`, request ID, log redaction, UUIDv7, OpenAPI.
- Flyway V1, Testcontainers, ArchUnit.
- ADR nền: xác thực (0001), mã hóa (0002).

### GĐ1 — Tài khoản và hồ sơ (M1) ✅

**Mục tiêu:** người dùng tạo tài khoản an toàn và quản lý quyền riêng tư.
**Spec:** [01-identity-auth](spec/01-identity-auth/README.md), [02-profile-consent](spec/02-profile-consent/README.md).

- Đăng ký 2 bước có xác minh email (link hoặc mã), đăng nhập, Google OAuth, refresh token xoay vòng, quên/đổi mật khẩu, quản lý phiên.
- Hồ sơ, onboarding, consent có lịch sử.
- **Còn mở:** thống nhất chính sách độ dài mật khẩu; mobile chưa có Google và đổi mật khẩu.

### GĐ2 — Nhật ký, check-in, safety đầu vào (M2) 🟡

**Mục tiêu:** ghi nhật ký mã hóa, chỉ chủ sở hữu đọc được, có sàng lọc an toàn ngay khi lưu.
**Spec:** [03-journal](spec/03-journal/README.md), [04-checkin](spec/04-checkin.md), [08-safety](spec/08-safety/README.md).

- **Đã có:** CRUD nhật ký mã hóa, idempotency, `If-Match`, nhãn, ảnh (Cloudinary/local có chữ ký), check-in hằng ngày, sàng lọc an toàn và fail-safe trong cùng transaction.
- **Còn mở:**
  - Bộ luật safety tiếng Việt/Anh có phiên bản, nội dung phản hồi và nguồn hỗ trợ được duyệt theo ngôn ngữ.
  - Đánh giá false positive/negative trên bộ nhãn được duyệt, báo cáo eval có ngưỡng chấp nhận.
  - Quét ảnh (`PENDING_SCAN`). (Quy tắc dashboard ưu tiên check-in, nhật ký chỉ là dự phòng **đã có trong code**, dù checklist M2 trong kế hoạch backend chưa tick.)

### GĐ3 — Phân tích AI bất đồng bộ (M3) 🟡

**Mục tiêu:** phân tích cảm xúc, chủ đề, phản chiếu sau khi lưu bài, chạy trong worker, không chặn việc ghi.
**Spec:** [05-ai-analysis](spec/05-ai-analysis.md).

- **Đã có:** transactional outbox, worker claim có lease, vòng đời trạng thái phân tích, retry, kiểm tra consent `AI_PROCESSING` lúc job chạy, adapter provider qua port.
- **Còn mở:**
  - Duyệt provider: retention, không dùng để train, vùng dữ liệu, xác minh xóa; circuit breaker.
  - Kiểm tra an toàn cho **đầu ra** AI.
  - Client: polling có backoff, phân biệt pending/failed/blocked, không spinner vô hạn.

### GĐ4 — Insight, dashboard, báo cáo (M4) 🟡

**Mục tiêu:** người dùng nhìn lại xu hướng dựa trên số liệu thật, có bằng chứng.
**Spec:** [07-insights-reports](spec/07-insights-reports/README.md), [06-thinking-patterns](spec/06-thinking-patterns.md).

- **Đã có:** tổng hợp theo ngày, insight có evidence, báo cáo tuần/tháng, dashboard.
- **Thử nghiệm (2026-11):** nhận diện kiểu suy nghĩ (V22) với consent `THINKING_PATTERNS` riêng. Việc trích tín hiệu hiện chỉ chạy qua endpoint dev ở profile `local`; cần cơ chế kích hoạt và extractor được duyệt cho môi trường thật.
- **Còn mở:** narrative AI phải qua kiểm tra an toàn đầu ra; tiến bộ nhỏ (UC-19) và bản tóm tắt cho buổi tham vấn (UC-20) mới là đề xuất.

### GĐ5 — Tự chăm sóc và companion (M5) 🟡

**Mục tiêu:** mục tiêu, thói quen, chuỗi ngày; đồng hành khi viết.
**Spec:** [09-selfcare](spec/09-selfcare/README.md), [10-companion](spec/10-companion.md).

- **Đã có:** mục tiêu, thói quen, hoàn thành, streak; công cụ giúp bình tĩnh; companion gợi ý khi viết và gợi ý thói quen (V21), mặc định tắt.
- **Còn mở:** policy và nội dung được duyệt để giữ mục tiêu trong phạm vi wellness (không thành kế hoạch điều trị); duyệt provider cho companion; lời nhắn cho bản thân tương lai (UC-18) mới là đề xuất.

### GĐ6 — Kho kiến thức, RAG, quản trị (M6) 🟡

**Mục tiêu:** nội dung hỗ trợ được con người duyệt, gợi ý có trích nguồn; công cụ quản trị tối thiểu.
**Spec:** [11-knowledge](spec/11-knowledge.md), [14-admin](spec/14-admin/README.md).

- **Đã có:** quy trình biên tập/duyệt kiến thức, embedding vào pgvector, API gợi ý, RBAC quản trị, audit.
- **Còn mở:** eval retrieval bằng bộ câu hỏi chọn sẵn; đánh giá grounding và an toàn đầu ra; duyệt provider embedding/generation; các knowledge card trong `recommend-model/` vẫn đang chờ duyệt.

### GĐ7 — Quyền đối với dữ liệu (M7) 🟡

**Mục tiêu:** người dùng lấy về hoặc xóa toàn bộ dữ liệu của mình.
**Spec:** [12-export-deletion](spec/12-export-deletion/README.md), [13-feedback](spec/13-feedback/README.md).

- **Đã có:** export CSV/PDF mã hóa có hạn 24 giờ, "bắt đầu lại", yêu cầu xóa tài khoản có thể hủy và worker xóa dữ liệu, phản hồi mã hóa.
- **Còn mở:** xóa cả ảnh trên Cloudinary, cache và dữ liệu phía provider AI; thiếu test HTTP cho export, feedback, admin.

### GĐ8 — Hoàn thiện client 🟡

**Mục tiêu:** web và mobile dùng đủ các luồng P0/P1 với backend thật.

- **Web:** đã nối hầu hết luồng; cần kiểm chứng trên môi trường triển khai.
- **Mobile:** đã nối auth, onboarding, nhật ký văn bản, dashboard, insight, self-care, phiên, export. Còn mở:
  - chạy trên Android/iOS thật (SecureStore, bàn phím, safe area, screen reader);
  - rich text và ảnh trong nhật ký; Google; đổi mật khẩu; mở link email (Universal/App Links);
  - đồng bộ và xung đột giữa web và mobile, mất mạng khi lưu; tải/chia sẻ file export native.

### GĐ9 — Gia cố và phát hành (M8) 🔴

**Mục tiêu:** đủ điều kiện phát hành staging với dữ liệu synthetic. Runbook: [`M8_RELEASE_RUNBOOK.md`](backend/docs/M8_RELEASE_RUNBOOK.md), threat model: [`M8_THREAT_MODEL.md`](backend/docs/M8_THREAT_MODEL.md).

- **Đã có:** quota cho nhật ký/export, chế độ chạy API và worker, readiness, security header, profile `migrate`.
- **Còn mở (release gate):**
  - Bảo mật: scan dependency/container, kiểm thử phân quyền toàn bộ endpoint, diễn tập xoay secret, MFA cho quản trị.
  - Privacy/safety: quét log không có dữ liệu nhạy cảm, xác nhận retention/opt-out của provider, safety eval đạt ngưỡng, nguồn hỗ trợ khủng hoảng được duyệt.
  - Độ tin cậy: load test, worker crash/reclaim, timeout provider, backup + restore drill, Redis mất không làm mất dữ liệu.
  - Triển khai: staging, Flyway có kiểm soát, dashboard và cảnh báo.

### GĐ10 — Sau MVP ⚪

Chỉ bắt đầu sau khi thử nghiệm với người dùng; trước khi làm phải chốt consent, retention, mã hóa, export/xóa, safety và contract.

- Thư cho chính mình đọc lại vào ngày khó khăn; bản chuẩn bị buổi tham vấn một trang (xem [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)).
- SSE thay polling, tương quan nâng cao.
- Thiết bị đeo, giọng nói, nhắc nhở.
- Tìm kiếm toàn văn trên nhật ký mã hóa.
- Đa vùng, microservices, Kafka khi có nhu cầu đo được.

## Phạm vi MVP

| Mức | Gồm |
|---|---|
| **P0 — bắt buộc** | Tài khoản, phiên, consent; nhật ký và check-in mã hóa; sàng lọc an toàn và fail-safe; phân tích cảm xúc/chủ đề bất đồng bộ; dashboard cơ bản và tóm tắt tuần có bằng chứng; quản trị tối thiểu (trạng thái tài khoản, nguồn hỗ trợ, job lỗi); export và xóa tài khoản; audit hành động quản trị |
| **P1 — nên có** | Tự chăm sóc; duyệt kiến thức và RAG có trích nguồn; tải ảnh; báo cáo tháng; quy trình phản hồi |
| **P2 — sau MVP** | Xem GĐ10 |
