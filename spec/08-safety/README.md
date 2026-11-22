# 08 — Safety

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Sàng lọc nguy cơ trong nội dung người dùng viết, chặn phản hồi AI thông thường khi cần, và dẫn người dùng tới nguồn hỗ trợ đã được con người duyệt.

- **Module backend:** `com.mylog.safety`
- **API gốc:** `/api/v1/safety/resources` (công khai, không cần đăng nhập)
- **Web:** `features/journal/components/SafetyModal.tsx`, [`features/journal/api/safety-resources.ts`](../../front-end/my-app-web/features/journal/api/safety-resources.ts)
- **Mobile:** [`src/features/safety`](../../front-end/my-app-mobile/src/features/safety)
- **Migration:** `V7` (trong [`db/migration`](../../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`SAFETY_SCREENING_FLOW.md`](../../backend/docs/SAFETY_SCREENING_FLOW.md), [`SAFETY_CLASSIFIER_MODEL_PLAN.md`](../../backend/docs/SAFETY_CLASSIFIER_MODEL_PLAN.md), [`SAFETY_M2_EVALUATION.md`](../../backend/docs/SAFETY_M2_EVALUATION.md), [`adr/0003-layered-safety-screening.md`](../../backend/docs/adr/0003-layered-safety-screening.md), [`safety-model/README.md`](../../safety-model/README.md)

## 1. Mục tiêu và phạm vi

An toàn đi trước cá nhân hóa: khi không chắc chắn, hệ thống chọn hướng an toàn. AI không bao giờ tự tạo đường dây nóng hay nguồn hỗ trợ khẩn cấp.

- **Trong phạm vi:** sàng lọc nhiều lớp cho nhật ký, companion và đầu ra narrative; ghi sự kiện safety; cổng policy quyết định có cho phân tích AI hay không; danh sách nguồn hỗ trợ đã duyệt; giao diện hỗ trợ an toàn.
- **Ngoài phạm vi:** quản trị nguồn hỗ trợ và policy (UC-28, [14-admin](../14-admin/README.md)); huấn luyện classifier ([`safety-model/`](../../safety-model/README.md)).

## 2. Use case

- [UC-14 — Nhận hướng dẫn an toàn khi có dấu hiệu nguy cơ](UC-14-risk-guidance.md)
- [UC-17 — Xem nội dung và nguồn hỗ trợ đã duyệt](UC-17-approved-resources.md)

## 3. Business rule và phân quyền chung

**Các lớp sàng lọc** (`SafetyScreeningService.java`), chạy theo thứ tự, dừng ở lớp đầu tiên có kết luận:

1. **Luật soạn sẵn** (phiên bản `m2-curated-draft-v1`), không phân biệt hoa thường, tiếng Việt và tiếng Anh:
   - `CRITICAL`: ý định có thời điểm cụ thể, ví dụ "kill myself now", "tự tử … tối nay";
   - `HIGH`: ví dụ "want to die", "self harm", "muốn chết", "không muốn sống", "tự tử", "tự hại".
   - Khớp → quyết định `SAFETY_FLOW`, không gọi classifier.
2. **Classifier** (`HttpRiskClassifier` gọi dịch vụ `safety-model` qua HTTPS, hết giờ sau 2 giây). Không cấu hình URL → classifier "không có" (`UnavailableRiskClassifier`).
3. **Policy:** kết quả `NORMAL`/`LOW`/`MODERATE` chỉ được dùng khi có một policy `APPROVED`, đã có người duyệt, đang trong thời gian hiệu lực, khớp phiên bản luật/classifier và `confidence ≥ minConfidence` (`JpaSafetyPolicyGate`).

**Quyết định**

| Kết quả | Quyết định | Ý nghĩa |
|---|---|---|
| Luật khớp, hoặc classifier trả `HIGH`/`CRITICAL` | `SAFETY_FLOW` | Hiện hỗ trợ an toàn; không phân tích/phản hồi AI thường |
| Classifier `MODERATE` + policy duyệt | `CONSTRAIN` | Không vào luồng phản chiếu tự do; companion trả câu trung tính |
| Classifier `NORMAL`/`LOW` + policy duyệt | `ALLOW` | Được phân tích AI thường |
| Classifier lỗi, hết giờ, không có, kết quả sai, hoặc không có policy duyệt | `FAIL_SAFE` | Vẫn lưu nội dung; chặn phản hồi AI; nhật ký được sàng lọc lại sau qua outbox |

- Mỗi lần sàng lọc nhật ký ghi một `safety_events` (mức rủi ro, quyết định, phiên bản luật/classifier/policy, độ tin cậy), không ghi nội dung.
- **Cổng phân tích** (`JpaSafetyAnalysisPermission`): một bài chỉ được phân tích AI khi sự kiện safety **mới nhất** là `ALLOW` và policy của nó vẫn còn được duyệt tại thời điểm xử lý.
- **Đầu ra AI:** narrative insight/báo cáo được sàng lọc lại trước khi dùng (xem [07-insights-reports](../07-insights-reports/README.md)). Phản chiếu nhật ký và câu hỏi gợi mở RAG **chưa** được sàng lọc đầu ra (xem câu hỏi mở).
- **Dev local:** `MYLOG_SAFETY_DEV_ALLOW` thay classifier bằng `dev-allow` (luật vẫn chạy trước). Chỉ được bật với profile `local` và không được bật cùng `MYLOG_JOBS_ENABLED` (`DevSafetyAllowConfiguration`).

## 4. API contract

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/safety/resources` | `locale` (`vi` hoặc `vi-VN`), `country` (2 chữ in hoa) — **không cần đăng nhập** | `200 [{id, name, resourceType, contactValue, description, sourceUrl, version}]` | 400 |

Kết quả sàng lọc với người dùng thể hiện qua `riskLevel` và `analysisStatus` của bài nhật ký ([03-journal](../03-journal/README.md)), `status` của companion ([10-companion](../10-companion.md)).

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `safety_policy_versions` | Policy: `status` (`DRAFT`/`APPROVED`/`RETIRED`), `config` JSON (phiên bản luật, classifier, `minConfidence`...), người duyệt, thời gian hiệu lực | V7 |
| `safety_resources` | Nguồn hỗ trợ: tên, loại, liên hệ, mô tả, `source_url`, `locale`, `country_code`, `status`, `version`, `verified_at` | V7 |
| `safety_events` | Kết quả sàng lọc mỗi lần, gắn với bài nhật ký | V7 |

## 6. Safety, quyền riêng tư và consent

- Sàng lọc chạy cho mọi bài, **không** phụ thuộc consent AI (sàng lọc không gửi nội dung tới provider bên ngoài, chỉ tới dịch vụ classifier nội bộ).
- Classifier chỉ nhận văn bản và token máy chủ; không chấp nhận endpoint không TLS ở xa (`HttpRiskClassifierTest`).
- Không log nội dung được sàng lọc.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `SafetyScreeningCorpusTest`, `HttpRiskClassifierTest`, `DevSafetyAllowConfigurationTest`, cổng safety trong `AiPipelineIntegrationTest`, `JournalCheckinIntegrationTest` |
| Web | xong | `SafetyModal` sau khi lưu bài rủi ro cao, trên dashboard, và trong panel phân tích |
| Mobile | xong | `safety-support-modal.tsx`; script `e2e-safety-analysis.mjs` |
| Nội dung | **chưa có** | Chưa có policy `APPROVED`, chưa có nguồn hỗ trợ được duyệt; classifier chưa được duyệt |

## 8. Câu hỏi mở

1. **Không có policy nào được duyệt** → mọi kết quả classifier đều thành `FAIL_SAFE`, nên ở môi trường thật không bài nào được phân tích AI. Cần quy trình và người có thẩm quyền duyệt policy.
2. Luật `HIGH` khớp cả khi người dùng chỉ nhắc tới từ "tự tử" (ví dụ kể về tin tức, phim). Chấp nhận dương tính giả hay cần luật tinh hơn? Cần bộ eval tiếng Việt có nhãn được duyệt và ngưỡng chấp nhận.
3. Phản chiếu nhật ký ([05-ai-analysis](../05-ai-analysis.md)) và câu hỏi gợi mở RAG ([11-knowledge](../11-knowledge.md)) chưa qua sàng lọc đầu ra.
4. Lời nhắn trong `SafetyModal` đang viết cứng trong code web, chưa phải "phản hồi safety đã duyệt theo ngôn ngữ" như kế hoạch M2 yêu cầu.
5. Ghi chú check-in không được sàng lọc ([04-checkin](../04-checkin.md)).
