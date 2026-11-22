# 07 — Insight và báo cáo

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Dashboard, xu hướng cảm xúc, nhận định (insight) có bằng chứng, báo cáo tuần/tháng, linh vật trang Insight. Thêm hai tính năng mới ở mức đề xuất: những tiến bộ nhỏ và bản tóm tắt cho buổi tham vấn.

- **Module backend:** `com.mylog.insight`, `com.mylog.reporting`
- **API gốc:** `/api/v1/dashboard`, `/api/v1/insights`, `/api/v1/reports`
- **Web:** [`features/dashboard`](../../front-end/my-app-web/features/dashboard), [`features/insights`](../../front-end/my-app-web/features/insights), [`features/reporting`](../../front-end/my-app-web/features/reporting)
- **Mobile:** [`src/features/overview`](../../front-end/my-app-mobile/src/features/overview), màn hình `(app)/dashboard.tsx`, `(app)/insights.tsx`, `(app)/report/[id].tsx`
- **Migration:** `V9` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`AI_ANALYSIS_GUIDE.md`](../../backend/docs/AI_ANALYSIS_GUIDE.md), [`SELF_COMPASSION_FEATURE_PROPOSALS.md`](../../backend/docs/SELF_COMPASSION_FEATURE_PROPOSALS.md)

## 1. Mục tiêu và phạm vi

Giúp người dùng hiểu sự thay đổi của mình theo thời gian, dựa trên **số liệu thật, đủ mẫu, có nguồn**, không phải suy đoán của AI.

- **Trong phạm vi:** dashboard 7/30/90 ngày, insight dạng mẫu hình 30 ngày, báo cáo tuần/tháng có phiên bản, linh vật, ẩn insight.
- **Ngoài phạm vi:** nhận diện kiểu suy nghĩ ([06-thinking-patterns](../06-thinking-patterns.md)), phân tích từng bài ([05-ai-analysis](../05-ai-analysis.md)).

## 2. Use case

- [UC-15 — Xem xu hướng cảm xúc và báo cáo](UC-15-mood-trends-reports.md)
- [UC-19 — Xem lại những tiến bộ nhỏ](UC-19-small-progress.md) (đề xuất, chưa có code)
- [UC-20 — Tạo bản tóm tắt mang đến buổi tham vấn](UC-20-counseling-summary.md) (đề xuất, chưa có code)

## 3. Business rule và phân quyền chung

- Mọi endpoint cần bearer token và lọc theo chủ sở hữu.
- **Dữ liệu mỗi ngày:** check-in là nguồn chính, chỉ số của bài nhật ký mới nhất trong ngày là dự phòng; mỗi điểm dữ liệu ghi rõ `source` (`CHECKIN` hoặc `JOURNAL_FALLBACK`). Xem [04-checkin](../04-checkin.md).
- Ngày tính theo **múi giờ của hồ sơ**.
- **Số liệu quyết định nội dung.** Mọi insight và báo cáo được tính bằng quy tắc thống kê; câu chữ lấy từ template tiếng Việt. AI chỉ được **diễn đạt lại** câu template khi tất cả điều kiện sau đúng (`NarrativeRewriter.java`):
  - đã bật `mylog.insights.ai-narrative.enabled` với provider `openai` có đủ hai phê duyệt;
  - người dùng đang bật `AI_PROCESSING`;
  - không có bài `HIGH`/`CRITICAL` trong khoảng thời gian;
  - câu viết lại giữ nguyên các con số, không có từ nguyên nhân/chẩn đoán/liên hệ (`InsightNarrativeValidator`), và qua sàng lọc an toàn đầu ra (`ALLOW`).
  - Nếu không, giữ câu template: `generatedBy = STATISTICAL` với insight, `RULE` với báo cáo. Viết lại thành công thì `AI_ASSISTED`.
- Không diễn đạt tương quan thành nguyên nhân; câu template kết thúc bằng câu hỏi gợi mở.
- Dashboard không gọi AI trên request.

## 4. API contract

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/dashboard` | `range` = `7d` \| `30d` (mặc định) \| `90d` | `200` Dashboard | 400 |
| GET | `/insights` | `from?`, `to?`, `cursor?`, `limit` (1–50, mặc định 20) | `200 {items: [Insight], nextCursor}` | 400 |
| POST | `/insights:refresh` | — | `200 {items, nextCursor}` | 429 (20 lần/giờ) |
| PATCH | `/insights/{id}` | `{status: "HIDDEN"}` | `204` | 400, 404 |
| GET | `/insights/mascot` | — | `200 {mood, moodCount, entryCount, from, to, calm}` | |
| GET | `/reports` | `type?` (`WEEKLY` \| `MONTHLY`), `cursor?`, `limit` (1–50) | `200 {items: [Report], nextCursor}` | 400 |
| GET | `/reports/{id}` | — | `200` Report | 404 |
| POST | `/reports` | `{type: WEEKLY \| MONTHLY}` | `200` khi đã `READY`, `202` khi đang tạo | 400, 429 (10 lần/giờ) |
| POST | `/reports/{id}:regenerate` | — | `202` Report (phiên bản mới) | 404, 409, 429 |
| GET | `/reports/regeneration` | — | `200 {available, availableFrom}` | |

- **Dashboard:** `{range, timezone, from, to, timeline: [{date, source, journalCount, moodScore, stressScore, energyScore, sleepMinutes}], topMoods, topEmotions, topTopics: [{code, count, averageScore}], currentJournalStreak, longestJournalStreak, journalEntryCount, moodSampleSize}`.
- **Insight:** `{id, type, from, to, timezone, direction, strength, sampleSize, algorithmVersion, generatedBy, narrative, evidence: [{source, date, metric, value}]}`.
- **Report:** `{id, type, from, to, timezone, version, status, sampleSize, metrics, narrative, generatedBy, evidence, entryTitles, createdAt, completedAt}`.

Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `insights` | Insight: `insight_type`, khoảng ngày, `direction` (`UP`/`DOWN`/`STABLE`), `strength` 0–1, `sample_size`, `algorithm_version`, `generated_by` (`RULE`/`STATISTICAL`/`AI_ASSISTED`), narrative mã hóa, `status` (`ACTIVE`/`SUPERSEDED`/`HIDDEN`) | V9 |
| `insight_evidence` | Điểm dữ liệu làm bằng chứng | V9 |
| `reports` | Báo cáo bất biến theo phiên bản: loại, kỳ, `version`, `status` (`PENDING`/`PROCESSING`/`READY`/`FAILED`/`DEAD`), snapshot số liệu, narrative | V9 |
| `report_evidence` | Bằng chứng của báo cáo | V9 |

## 6. Safety, quyền riêng tư và consent

- Insight thống kê không cần consent AI (chỉ dùng số liệu); diễn đạt lại bằng AI cần `AI_PROCESSING`.
- Báo cáo chỉ lưu ngày, điểm và id bài; **tiêu đề bài được giải mã khi chủ sở hữu mở** báo cáo (`entryTitles`).
- Kỳ "khó khăn hơn" luôn giữ câu template đã duyệt, không cho AI viết lại.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | UC-15. Worker báo cáo và làm mới insight theo lịch mặc định **tắt** (`MYLOG_REPORTS_ENABLED`, `MYLOG_INSIGHTS_REFRESH_ENABLED`) |
| Web | đã kiểm chứng | UC-15 |
| Mobile | xong | UC-15: dashboard, insight, danh sách và chi tiết báo cáo |
| UC-19, UC-20 | chưa làm | Chỉ có đề xuất |

## 8. Câu hỏi mở

1. Khi `MYLOG_REPORTS_ENABLED=false` (mặc định), `POST /reports` vẫn tạo bản ghi `PENDING` và trả `202`, nhưng không có worker nào xử lý: người dùng chờ mãi. Có nên trả lỗi rõ ràng khi worker tắt?
2. Xem câu hỏi trong từng UC.
