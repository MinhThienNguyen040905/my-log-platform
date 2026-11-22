# 05 — Phân tích AI và phản chiếu

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Sau khi lưu bài, hệ thống phân tích bất đồng bộ để nhận ra cảm xúc, chủ đề và viết một đoạn phản chiếu ngắn.

- **Module backend:** `com.mylog.analysis` (nhận diện kiểu suy nghĩ cũng nằm ở đây, xem [06-thinking-patterns](06-thinking-patterns.md))
- **API gốc:** `/api/v1/journal-entries/{entryId}/analysis`
- **Web:** [`features/journal/api/analysis.ts`](../front-end/my-app-web/features/journal/api/analysis.ts), `features/journal/components/EntryAnalysisPanel.tsx`
- **Mobile:** [`src/features/journal`](../front-end/my-app-mobile/src/features/journal) (gọi `analysis` và `analysis:retry`)
- **Migration:** `V8` (trong [`db/migration`](../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`AI_ANALYSIS_GUIDE.md`](../backend/docs/AI_ANALYSIS_GUIDE.md), [`adr/0004-ai-provider-and-data-handling.md`](../backend/docs/adr/0004-ai-provider-and-data-handling.md), [`adr/0009-hybrid-ai-inference.md`](../backend/docs/adr/0009-hybrid-ai-inference.md)

## 1. Mục tiêu và phạm vi

Giúp người dùng tự suy ngẫm về bài vừa viết, mà không làm việc ghi nhật ký phụ thuộc vào AI.

- **Trong phạm vi:** hàng đợi job phân tích, gọi provider ngoài request, kiểm tra đầu ra, trạng thái phân tích theo phiên bản nội dung, xem kết quả, thử lại.
- **Ngoài phạm vi:** sàng lọc an toàn đầu vào ([08-safety](08-safety/README.md)), insight tổng hợp nhiều ngày ([07-insights-reports](07-insights-reports/README.md)).

## 2. Use case

### UC-13 — Xem phản chiếu sau khi lưu bài

Catalog: UC-13 trong [`USE_CASE_CATALOG.md`](../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên).

#### 1. Tác nhân

Người dùng đã lưu một bài nhật ký.

#### 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện để phân tích chạy:** tất cả các điều kiện sau phải đúng tại thời điểm worker xử lý:
  - job bật (`mylog.jobs.enabled=true`) và có provider được cấu hình;
  - tài khoản `ACTIVE`, consent `AI_PROCESSING` đang bật;
  - sự kiện safety mới nhất của bài là `ALLOW` **và** khớp một safety policy `APPROVED` còn hiệu lực (`JpaSafetyAnalysisPermission`);
  - `contentVersion` của job vẫn là phiên bản hiện tại của bài.
- **Hậu điều kiện:** bài có `analysisStatus = ANALYZED` và trỏ tới một bản phân tích `SUCCEEDED` của đúng `contentVersion`.

#### 3. Luồng chính

1. Người dùng lưu bài (UC-08). Nếu sàng lọc cho phép, outbox phát `JournalEntrySubmitted`.
2. Outbox handler tạo một job `JOURNAL_ANALYSIS` trong `ai_jobs` (tối đa 5 lần thử) nếu bài vẫn ở `PENDING` hoặc `ANALYSIS_OUTDATED` (`JournalAnalysisOutboxHandler`).
3. Worker (mỗi 2 giây, tối đa 10 job/lượt) claim job bằng `FOR UPDATE SKIP LOCKED`, lease 5 phút (`AiJobWorker.java:43-80`).
4. Handler (`AnalysisJobHandler.java`):
   1. kiểm tra tài khoản, consent, safety; chuyển bài sang `ANALYZING`;
   2. gọi provider **ngoài** transaction với tiêu đề + văn bản thuần;
   3. kiểm tra đầu ra (`AnalysisOutputValidator`);
   4. kiểm tra lại lease, consent, safety và phiên bản; lưu kết quả (phản chiếu được mã hóa); chuyển bài sang `ANALYZED`.
5. Client gọi `GET /journal-entries/{id}/analysis` (polling) và hiển thị phản chiếu, cảm xúc, chủ đề bằng nhãn dễ hiểu.

#### 4. Luồng phụ và ngoại lệ

| Tình huống | Kết quả |
|---|---|
| Thiếu consent hoặc tài khoản không `ACTIVE` (kể cả rút consent trong lúc gọi provider) | Không lưu kết quả; bài chuyển `NOT_REQUESTED` |
| Safety không cho phép | Bài chuyển `BLOCKED_BY_SAFETY`; client mở luồng hỗ trợ an toàn, không có nút thử lại |
| Bài đã được sửa (phiên bản mới) | Job cũ bị bỏ (`STALE`); kết quả cũ chuyển `STALE` và không hiển thị cho phiên bản mới |
| Bài bị xóa | Mọi kết quả `SUCCEEDED` của bài chuyển `STALE` |
| Provider lỗi tạm thời | Thử lại với backoff `2^attempt` giây (tối đa 1 giờ), tối đa 5 lần |
| Đầu ra sai định dạng hoặc chứa từ cấm | Lỗi vĩnh viễn, không thử lại; bài chuyển `ANALYSIS_FAILED` |
| Hết 5 lần thử | Job `DEAD`; bài chuyển `ANALYSIS_FAILED` |
| Worker chết giữa chừng | Lease hết hạn sau 5 phút, worker khác claim lại; nhật ký không mất |
| Không có provider (`provider=none`) | Mỗi lần thử đều lỗi tạm thời; sau 5 lần bài thành `ANALYSIS_FAILED` |

**Thử lại thủ công** (`POST /journal-entries/{id}/analysis:retry`, `AnalysisRetryService.java`):

- Bài đang `ANALYZED`, `PENDING` hoặc `ANALYZING`: trả trạng thái hiện tại, không làm gì.
- Thiếu consent hoặc safety không cho phép: `409`.
- Thử lại trong vòng 60 giây sau lần xử lý trước: `429`.
- Chưa có job nào cho bài: `409` "Job phân tích chưa sẵn sàng."
- Thành công: job được xếp lại, trả `{status: "PENDING"}`.

#### 5. Business rule

- **Đầu ra hợp lệ** (`AnalysisOutputValidator.java`):
  - `sentiment`: `POSITIVE`, `NEUTRAL`, `NEGATIVE`, `MIXED`, kèm điểm;
  - `emotions` (≤ 5, không trùng): `JOY`, `SADNESS`, `ANGER`, `FEAR`, `CALM`, `HOPE`, `ANXIETY`;
  - `topics` (≤ 5, không trùng): `WORK`, `RELATIONSHIPS`, `HEALTH`, `SLEEP`, `SELF_CARE`, `OTHER`;
  - `reflection`: không rỗng, ≤ 2.000 ký tự, không chứa "hotline", "đường dây nóng", "chẩn đoán", "diagnos", "tự tử", "suicide".
- **Provider:** `fake` (chỉ dev) hoặc `openai`. Bật `openai` bắt buộc cả hai cờ `mylog.ai.provider-data-approved` và `mylog.ai.output-safety-approved` = `true`, nếu không ứng dụng không khởi động (`JournalAnalyzerConfiguration.java`).
- Payload job và outbox chỉ chứa id và `contentVersion`, không có nội dung.
- Mỗi lần gọi provider ghi chi phí ước tính và số token vào `ai_usage_records`.

#### 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/journal-entries/{id}/analysis` | — | `200 {entryId, contentVersion, status, analysisId, sentiment, sentimentScore, emotions[], topics[], reflection}`; các trường kết quả chỉ có khi `status = ANALYZED` | 401, 404 |
| POST | `/api/v1/journal-entries/{id}/analysis:retry` | — | `200 {status}` | 401, 404, 409, 429 |
| POST | `/api/v1/dev/analyses:seed` | — | Tạo phân tích giả cho dữ liệu demo. Chỉ profile `local` | — |

#### 7. Giao diện web và mobile

- **Web:** `EntryAnalysisPanel` hiển thị trạng thái chờ bằng lời dễ hiểu, phản chiếu và các chip cảm xúc/chủ đề có nhãn; **không** hiện điểm số hay ID; bài bị chặn bởi safety không có nút thử lại và dẫn tới nguồn hỗ trợ.
- **Mobile:** hiển thị trạng thái và kết quả trong chi tiết bài, có thử lại.

#### 8. Tiêu chí chấp nhận

- [x] Thiếu consent thì không giải mã và không gọi provider; rút consent trong lúc gọi thì không lưu kết quả; tài khoản chờ xóa bỏ qua provider (`AnalysisJobHandlerTest`).
- [x] Policy được duyệt + consent → phản chiếu mã hóa, chỉ chủ sở hữu đọc; nội dung đổi thì job cũ không kích hoạt; sửa bài làm kết quả cũ hết hiệu lực; poll song song chỉ giữ một job và một kết quả; lease hết hạn được claim lại; retry sau khi bật consent bị giới hạn tốc độ (`AiPipelineIntegrationTest`).
- [x] Provider thật đòi đủ hai phê duyệt (`JournalAnalyzerConfigurationTest`).
- [x] Kiểm tra đầu ra (`AnalysisOutputValidatorTest`, `OpenAiJournalAnalyzerTest`).
- [x] Web: chờ, bị chặn, có kết quả, thất bại có thử lại (`entry-analysis.spec.ts`).
- [ ] Kiểm tra an toàn đầy đủ cho phản chiếu (hiện chỉ có danh sách từ cấm).
- [ ] Client polling có backoff và giới hạn, không spinner vô hạn.

## 3. Business rule và phân quyền chung

- Endpoint cần bearer token; kết quả lọc theo chủ sở hữu.
- Consent và safety được kiểm tra **hai lần**: trước khi gọi provider và trước khi lưu kết quả.
- Không gọi provider trong transaction database.

## 4. API contract

Xem mục 6 của UC-13 ở trên.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `ai_jobs` | Hàng đợi job: `status` (`PENDING`, `RETRY_WAIT`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `DEAD`), `attempt`/`max_attempts`, lease, `last_error_code` (`POLICY_BLOCKED`, `CONSENT_REQUIRED`), payload chỉ có id/version | V8 |
| `ai_analyses` | Kết quả theo `content_version`: sentiment, phản chiếu mã hóa, provider/model/prompt version, `status` (`RUNNING`, `SUCCEEDED`, `FAILED`, `STALE`, `REJECTED_BY_SAFETY`) | V8 |
| `analysis_emotions`, `analysis_topics` | Mã cảm xúc/chủ đề và điểm của mỗi kết quả | V8 |
| `ai_usage_records` | Token, độ trễ, chi phí ước tính mỗi lần gọi | V8 |

`analysis_status` trên `journal_entries` được mô tả ở [03-journal](03-journal/README.md#5-dữ-liệu).

## 6. Safety, quyền riêng tư và consent

- Chỉ gửi tiêu đề và văn bản thuần, không gửi ảnh, nhãn, địa điểm.
- Bài `HIGH`/`CRITICAL` hoặc chưa sàng lọc được không bao giờ được gửi tới provider.
- Hiện **chưa có safety policy nào được duyệt**, nên ở môi trường thật cổng safety luôn chặn và phân tích không chạy.
- Ở local, `MYLOG_SAFETY_DEV_ALLOW` chỉ thay classifier cho dữ liệu synthetic và **từ chối khởi động cùng `MYLOG_JOBS_ENABLED=true`**; dữ liệu demo có phân tích được tạo bằng `POST /dev/analyses:seed` (`LOCAL_FAKE`).

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Các test ở mục 8. Mặc định tắt (`MYLOG_JOBS_ENABLED=false`, `MYLOG_AI_PROVIDER=none`) |
| Web | đã kiểm chứng | `entry-analysis.spec.ts` (một phần dùng phản hồi giả lập) |
| Mobile | xong | Chưa kiểm tra polling có backoff |

## 8. Câu hỏi mở

1. Phản chiếu do AI viết chỉ được kiểm tra bằng danh sách từ cấm, **chưa** qua bộ sàng lọc an toàn đầu ra như narrative của insight (`NarrativeRewriter`). Có bắt buộc sàng lọc đầu ra trước khi bật provider thật?
2. Khi `provider=none` nhưng job bật, mọi bài sẽ thất bại sau 5 lần thử và hiện "thất bại" cho người dùng. Có nên coi là "chưa bật" (`NOT_REQUESTED`) thay vì lỗi?
3. Ai duyệt provider (retention, không dùng để train, vùng dữ liệu, xác minh xóa) và ghi lại ở đâu?
