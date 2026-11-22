# 10 — Companion

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Trợ lý đồng hành trong trình soạn thảo nhật ký: trò chuyện ngắn, câu hỏi gợi mở, gợi ý tiêu đề, công cụ giúp bình tĩnh tức thời và gợi ý thói quen.

- **Module backend:** `com.mylog.journal` (companion), dùng `com.mylog.selfcare` (thói quen) và `com.mylog.safety`
- **API gốc:** `/api/v1/journal-writing-suggestions`, `/api/v1/companion/title-suggestions`
- **Web:** `features/journal/components/companion` (`CompanionPanel`, `CompanionComposer`, `CompanionHabitCard`, `CompanionConsentCard`...), `features/journal/components/calming-tools`, `features/journal/hooks/useCompanionConversation.ts`
- **Mobile:** —
- **Migration:** `V21` (trong [`db/migration`](../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`COMPANION_CHATBOT_PLAN.md`](../backend/docs/COMPANION_CHATBOT_PLAN.md)

## 1. Mục tiêu và phạm vi

Đồng hành nhẹ nhàng khi người dùng viết, giúp họ viết tiếp hoặc bình tĩnh lại, không thay thế hỗ trợ chuyên môn.

- **Trong phạm vi:** một lượt hội thoại (không lưu), câu trả lời từ template hoặc LLM có cổng duyệt, chọn công cụ bình tĩnh và mẫu thói quen từ danh mục cố định, gợi ý ghi nhận thói quen có sẵn, gợi ý tiêu đề theo tâm trạng.
- **Ngoài phạm vi:** lưu lịch sử trò chuyện; companion trên mobile; phân tích bài sau khi lưu ([05-ai-analysis](05-ai-analysis.md)).

## 2. Use case

Phần người dùng thấy khi viết được mô tả ở [UC-07 — Nhận gợi ý để viết tiếp](03-journal/UC-07-writing-suggestions.md). Catalog chưa có UC riêng cho công cụ bình tĩnh và gợi ý thói quen.

**Luồng một lượt** (`WritingSuggestionService.java`), thứ tự cổng cố định, không được đổi:

1. Tính năng bật (`mylog.writing-suggestions.enabled`)? Không → `UNAVAILABLE`.
2. Consent `AI_PROCESSING`? Không → `CONSENT_REQUIRED` (web hiện `CompanionConsentCard`).
3. Sàng lọc an toàn **bản nháp + mọi lượt trước của người dùng + tin nhắn mới**:
   - `SAFETY_FLOW` → không trả lời, client mở hỗ trợ an toàn;
   - `CONSTRAIN` → `BLOCKED` với câu trung tính "Mình chưa thể trò chuyện tiếp về điều này ngay bây giờ…";
   - `FAIL_SAFE` → `UNAVAILABLE`.
4. `ALLOW` → bộ sinh câu trả lời tạo `REPLY`, kèm gợi ý thói quen (nếu có).

**Bộ sinh câu trả lời**

- **Template** (mặc định, `TemplateCompanionResponder`): kịch bản dựng sẵn theo tâm trạng; luôn tính phần xác định gồm hành động tức thì, chips, `nextState`.
- **LLM** (`LlmCompanionResponder`, prompt `companion-openai-v2`): chỉ khi `mylog.companion.generation.enabled=true`, provider `openai` có đủ hai phê duyệt. Model viết `reply`, `questions`, `suggestedTitle` và chỉ được **chọn** hành động/mẫu thói quen từ danh sách cố định. Hết giờ (4 giây), lỗi HTTP, đầu ra sai, hoặc sàng lọc đầu ra chặn → quay về template.

**Gợi ý thói quen** (`CompanionHabitSuggestionService.java`)

- Tâm trạng `very_bad`: **không bao giờ** gợi ý thêm việc phải làm.
- `OPEN` / `FREE_TEXT`: chỉ khi model chọn một mẫu đã duyệt; nếu đã có thói quen đang hoạt động cùng công cụ và chưa làm hôm nay thì mời ghi nhận thói quen đó thay vì tạo trùng.
- `TOOL_COMPLETED` / `CLOSING`, theo thứ tự ưu tiên:
  1. thói quen đang hoạt động khớp công cụ vừa dùng, chưa làm hôm nay;
  2. thói quen có liên hệ tâm trạng dương, chưa làm hôm nay (câu chữ không nói nguyên nhân);
  3. một mẫu đã duyệt.
- "Không, cảm ơn" chỉ ẩn thẻ trong cuộc trò chuyện hiện tại; không có giới hạn theo ngày.

## 3. Business rule và phân quyền chung

- Cần bearer token. Quota: 40 lượt/15 phút/user.
- **Không lưu** bản nháp, tin nhắn hay lịch sử trò chuyện ở backend.
- **Hành động tức thì** (`CompanionActionCode`): `BREATHING_2MIN` (thở chậm 2 phút), `AMBIENT` (âm thanh dịu) — model được chọn, tối đa 2; `VENT_PROMPT` (viết xả ra trang), `TITLE` (gợi ý tựa đề) — chỉ từ template. Mỗi mã phải có widget đã duyệt ở frontend trước khi thêm.
- **Mẫu thói quen** (`HabitTemplateCatalog`): `EVENING_BREATHING` (thở sâu 2 phút mỗi tối, gắn `BREATHING_2MIN`), `CALM_AUDIO_PAUSE` (nghe âm thanh dịu 5 phút, gắn `AMBIENT`), `GENTLE_WALK` (đi bộ thư giãn 15 phút), `SLEEP_BEFORE_23H` (lên giường trước 23h).
- **Kiểm tra đầu ra LLM** (`CompanionOutputValidator`): `reply` ≤ 400 ký tự, tối đa 3 câu hỏi ≤ 200 ký tự, tiêu đề ≤ 160; cấm các cụm như "chẩn đoán", "trầm cảm nặng", "kê đơn", "uống thuốc", "hotline", số "1900"/"1800", đường link, "suicide", "tu tu"/"tu sat"; trích dẫn trong ngoặc phải có thật trong bài. Sau đó sàng lọc an toàn đầu ra; mặc định chặn cứng (`outbound-safety-hard-block=true`).
- **Gợi ý tiêu đề:** danh sách tĩnh theo `mood` (`happy`, `neutral`, `angry`, `sad`, `very_bad`), không dùng AI.

## 4. API contract

Xem [UC-07](03-journal/UC-07-writing-suggestions.md#6-api-liên-quan). Bổ sung về response `REPLY`:

| Trường | Ý nghĩa |
|---|---|
| `reply`, `questions`, `suggestedTitle` | Nội dung trả lời |
| `immediateActions` | Danh sách hành động tức thì (mã + nhãn) |
| `habitSuggestion` | Gợi ý tạo thói quen từ mẫu, hoặc ghi nhận thói quen đã có (`habitId`, ngày, giá trị) |
| `chips` | Nút trả lời nhanh (ví dụ chọn tâm trạng) |
| `nextState` | Trạng thái hội thoại kế tiếp, do code xác định: `LISTENING`, `LONG_TERM`, `CLOSING`, `EXIT` |
| `source`, `promptVersion` | `TEMPLATE` hoặc LLM, phiên bản prompt |

Client tạo thói quen hoặc ghi nhận hoàn thành qua API của [09-selfcare](09-selfcare/README.md).

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `habits.tool_code` | Liên kết thói quen với công cụ bình tĩnh (`BREATHING_2MIN`, `AMBIENT`, `VENT_PROMPT`) | V21 |
| `companion_habit_preferences` | Ngày gợi ý gần nhất, ẩn tới khi nào | V21 |

## 6. Safety, quyền riêng tư và consent

- Cần `AI_PROCESSING` ngay cả với câu trả lời template (cổng consent đứng trước bộ sinh).
- Bản nháp không bao giờ được gửi tới bộ sinh khi sàng lọc không cho phép.
- LLM không được tạo đường dây nóng, link hay chẩn đoán; vi phạm → dùng template.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `WritingSuggestionServiceTest`, `CompanionHabitSuggestionServiceTest`, `CompanionStage4aEvaluationTest`, `JournalCheckinIntegrationTest`. Mặc định **tắt** (`MYLOG_WRITING_SUGGESTIONS_ENABLED`, `MYLOG_COMPANION_GENERATION_ENABLED`) |
| Web | đã kiểm chứng | `journal-editor-companion.spec.ts`, `journal-editor-calming-tools.spec.ts`, `ambient-player.spec.ts` |
| Mobile | chưa làm | |

## 8. Câu hỏi mở

1. Bảng `companion_habit_preferences` (V21) chỉ được xóa khi "bắt đầu lại", không có code nào đọc/ghi; code hiện cố ý **không** có giới hạn ngày hay thời gian ẩn. Bỏ bảng hay dùng lại?
2. Javadoc của `WritingSuggestionResponse` ghi `habitSuggestion` "luôn null", nhưng code đã trả gợi ý thói quen. Cần cập nhật javadoc.
3. Ngoài hai cờ phê duyệt chung `mylog.ai.*`, provider cho companion đã được duyệt cho nội dung nhật ký chưa?
4. Có cần UC riêng trong catalog cho "dùng công cụ bình tĩnh" và "nhận gợi ý thói quen từ companion"?
