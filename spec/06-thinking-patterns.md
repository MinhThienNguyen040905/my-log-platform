# 06 — Nhận diện kiểu suy nghĩ

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**. Tính năng đang ở mức **prototype**.

Nhận ra các kiểu suy nghĩ và thói quen lặp lại trong nhật ký 30 ngày gần nhất, dẫn chứng bằng chính câu người dùng đã viết, và gợi ý kế hoạch 3 tuần.

- **Module backend:** `com.mylog.analysis` (trích tín hiệu từ từng bài), `com.mylog.insight` (tạo thẻ, phản hồi, kế hoạch)
- **API gốc:** `/api/v1/patterns`
- **Web:** [`features/insights`](../front-end/my-app-web/features/insights) (`ThinkingPatternsPanel.tsx`, `api/thinking-patterns.ts`)
- **Mobile:** —
- **Migration:** `V22` (trong [`db/migration`](../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`THINKING_PATTERN_RECOGNITION_PLAN.md`](../backend/docs/THINKING_PATTERN_RECOGNITION_PLAN.md), [`THINKING_PATTERN_LABELING_GUIDE.md`](../backend/docs/THINKING_PATTERN_LABELING_GUIDE.md)

## 1. Mục tiêu và phạm vi

Giúp người dùng tự nhận ra "hóa ra mình hay nghĩ theo kiểu này", với ba yếu tố: dẫn chứng bằng lời của họ, gọi tên và bình thường hóa, để họ tự xác nhận. **Không** chẩn đoán.

- **Trong phạm vi:** trích tín hiệu từ từng bài (có consent riêng), quy tắc xác định để tạo thẻ, phản hồi "đúng / không hẳn", ẩn thẻ, bắt đầu kế hoạch 3 tuần gắn với mục tiêu và thói quen.
- **Ngoài phạm vi (chưa làm):** so sánh tần suất trước/sau khi hết kế hoạch 3 tuần; nhóm C "tín hiệu mất cân bằng" trong tài liệu kế hoạch; model tự huấn luyện.

## 2. Use case

Chưa có UC trong catalog. Đề xuất bổ sung UC mới khi tính năng được duyệt; khi có từ 2 UC trở lên thì tách file này thành thư mục.

**Luồng hiện tại**

1. Người dùng bật consent `THINKING_PATTERNS` (trong panel ở trang Insight hoặc cài đặt web). Cần cả `AI_PROCESSING`.
2. **Trích tín hiệu** (`PatternExtractionService.java`): với các bài 30 ngày gần nhất, đã lưu, rủi ro `NORMAL`/`LOW`, chưa được đọc ở phiên bản hiện tại, extractor trả về các tín hiệu `{code, confidence, quote}`. Tối đa 40 bài mỗi lần chạy. Consent được kiểm tra lại trước **mỗi** bài.
3. **Lọc tín hiệu** (`PatternSignalValidator.java`): chỉ giữ mã có trong taxonomy, `confidence` trong [0, 1], và câu trích **xuất hiện nguyên văn** trong bài (8–160 ký tự); tối đa 5 tín hiệu/bài, mỗi mã một tín hiệu.
4. **Tạo thẻ** (`POST /patterns:refresh`, `ThinkingPatternService.java`): chỉ đọc tín hiệu đã lưu, **không gọi provider**; quy tắc xác định quyết định thẻ nào hiện.
5. Người dùng đọc thẻ: tên, câu chuyện, giải thích, 3 bước, tối đa 2 câu trích kèm ngày, số bài và số tuần.
6. Người dùng có thể:
   - xác nhận `CONFIRMED`, hoặc chọn `NOT_REALLY` kèm lý do (`NOT_LIKE_ME`, `DONT_WANT_TO_SEE`, `WRONG_QUOTE`);
   - ẩn thẻ (`HIDDEN`);
   - bắt đầu kế hoạch: tạo một mục tiêu `MINDFULNESS` 21 ngày ("Thử từng bước: …") và một thói quen hằng ngày ("Ghi lại khi nhận ra mình đang …"). Gọi lại trả về mục tiêu đã có.

## 3. Business rule và phân quyền chung

- **Taxonomy `tp-v1`** (`ThinkingPatternTaxonomy.java`):
  - Nhóm A, bẫy suy nghĩ: `CATASTROPHIZING`, `ALL_OR_NOTHING`, `OVERGENERALIZATION`, `MIND_READING`, `SHOULD_STATEMENTS`, `SELF_LABELING`, `PERSONALIZATION`, `DISCOUNTING_POSITIVE`, `EMOTIONAL_REASONING`.
  - Nhóm B, vòng lặp và thói quen: `RUMINATION`, `WORRY_LOOP`, `AVOIDANCE`, `PROCRASTINATION_GUILT`, `PERFECTIONISM`, `PEOPLE_PLEASING`, `HARSH_SELF_CRITICISM`, `SOCIAL_WITHDRAWAL`.
- **Điều kiện hiện thẻ** (`ThinkingPatternRules.java`):
  - ít nhất **3 bài khác nhau**, trải trên ít nhất **2 tuần ISO**, độ tin cậy trung bình ≥ **0,70**;
  - tối đa **3 thẻ** đang hiện; tối đa **1 thẻ mới mỗi 7 ngày**;
  - mã đã bị ẩn không hiện lại; mã bị chọn `NOT_REALLY` im lặng **60 ngày**;
  - thẻ không còn đủ điều kiện bị thay thế (`SUPERSEDED`), trừ khi đang có kế hoạch.
- **Nếu trong 30 ngày có bài `HIGH`/`CRITICAL`:** mọi thẻ đang hiện bị thay thế và không hiện thẻ nào.
- **Câu chữ thẻ** lấy từ template (`ThinkingPatternTexts.java`), không do AI viết. Nếu có bài kiến thức `TP_<CODE>` (tiếng Việt) đã duyệt thì thẻ gắn `knowledgeVersionId` và `contentReviewed = true`.
- **Extractor** (`mylog.patterns.extractor`): `none` (mặc định), `fake` (luật từ khóa, chỉ profile `local`/`test`), `openai` (chỉ profile `local`, cấu hình riêng `mylog.patterns.ai.*`, không dùng chung phê duyệt của phân tích nhật ký).
- Tắt consent `THINKING_PATTERNS` → `GET /patterns` trả rỗng ngay.
- Quota: `patterns:refresh` 20 lần/giờ/user.

## 4. API contract

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/patterns` | — | `200 {items: [Pattern]}` (rỗng khi chưa có consent) | 401 |
| POST | `/patterns:refresh` | — | `200 {items: [Pattern]}` | 401, 429 |
| PATCH | `/patterns/{id}` | **hoặc** `{feedback: CONFIRMED \| NOT_REALLY, reason?}` **hoặc** `{status: HIDDEN}` | `204` | 400 (gửi cả hai, hoặc `reason` với `CONFIRMED`), 404 |
| POST | `/patterns/{id}:start-plan` | — | `200 {goalId, created}` | 404, 409 |
| POST | `/dev/pattern-signals:extract` | — | `{status, read, signals, skipped, failed, more, from, to}`. Chỉ profile `local` | 503 khi không có extractor |

**Pattern:** `{id, code, name, narrative, explanation, steps[3], quotes: [{text, date}], entryCount, weekCount, from, to, feedback, planGoalId, planStartedAt, generatedBy, knowledgeVersionId, contentReviewed, createdAt, updatedAt}`. Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `entry_pattern_extractions` | Mỗi (bài, phiên bản, taxonomy) chỉ đọc một lần | V22 |
| `entry_pattern_signals` | Tín hiệu: mã, độ tin cậy, câu trích **mã hóa**; một dòng/mã/bài | V22 |
| `pattern_recognitions` | Thẻ: trạng thái (`ACTIVE`, `SUPERSEDED`, `HIDDEN`), thống kê, phản hồi, mục tiêu kế hoạch; chỉ một thẻ sống mỗi mã | V22 |
| `pattern_recognition_evidence` | Liên kết thẻ với tín hiệu làm dẫn chứng | V22 |

- Xóa bài: xóa tín hiệu và dẫn chứng của bài, giữ thẻ. Xóa mục tiêu kế hoạch: giữ thẻ. Xóa user: xóa hết.
- Export chứa thẻ nhưng **không** chứa câu trích.

## 6. Safety, quyền riêng tư và consent

- Consent riêng `THINKING_PATTERNS` (mặc định tắt, không có trong onboarding) cộng với `AI_PROCESSING`.
- Chỉ đọc bài rủi ro `NORMAL`/`LOW`; có bài rủi ro cao trong cửa sổ thì không hiện thẻ.
- Câu trích phải có nguyên văn trong bài: model không thể "đặt lời vào miệng" người dùng.
- Ngôn ngữ thẻ dạng câu hỏi ("Có thể bạn hay…"), không phải chẩn đoán.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `ThinkingPatternRulesTest`, `ThinkingPatternIntegrationTest`, `ThinkingPatternSchemaIntegrationTest`, `ThinkingPatternTextsTest`, `PatternExtractionIntegrationTest`, `PatternSignalValidatorTest`, `FakePatternExtractorTest`, `OpenAiPatternExtractorTest` |
| Web | xong | `ThinkingPatternsPanel` có bật/tắt consent, thẻ, phản hồi, ẩn, kế hoạch; chưa có Playwright riêng |
| Mobile | chưa làm | |

## 8. Câu hỏi mở

1. **Việc trích tín hiệu chỉ được kích hoạt qua `POST /dev/pattern-signals:extract` (profile `local`).** Không có job hay lịch nào chạy nó, nên ở staging/prod sẽ không bao giờ có thẻ. Cần quyết định cách kích hoạt (job sau khi lưu bài, lịch đêm, hay khi người dùng bấm làm mới).
2. Extractor `openai` bị khóa ở profile `local`. Cần model hoặc provider nào được duyệt cho môi trường thật? (Tài liệu kế hoạch §7 nói về model tự fine-tune.)
3. Kế hoạch 3 tuần chưa có bước so sánh tần suất trước/sau như tài liệu kế hoạch mô tả.
4. Đề xuất UC chính thức cho catalog (ví dụ "Nhận ra kiểu suy nghĩ lặp lại" và "Bắt đầu kế hoạch 3 tuần").
