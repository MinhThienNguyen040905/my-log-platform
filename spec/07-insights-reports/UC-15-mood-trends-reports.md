# UC-15 — Xem xu hướng cảm xúc và báo cáo

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Insight và báo cáo](README.md)
- **Catalog:** UC-15 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đã đăng nhập.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** có hồ sơ (để biết múi giờ). Càng nhiều check-in và nhật ký thì càng nhiều nội dung.
- **Hậu điều kiện:** dashboard và insight không đổi dữ liệu nguồn; báo cáo mới (nếu yêu cầu) được lưu như một phiên bản bất biến.

## 3. Luồng chính

**Dashboard** (`DashboardService.java`)

1. Người dùng mở dashboard, chọn 7, 30 hoặc 90 ngày.
2. Backend trả dòng thời gian theo ngày (nguồn `CHECKIN` hoặc `JOURNAL_FALLBACK`), tâm trạng được chọn nhiều nhất (6), cảm xúc và chủ đề hàng đầu (5, từ phân tích AI nếu có), chuỗi ngày viết hiện tại và dài nhất, số bài, số ngày có điểm tâm trạng.

**Insight** (`InsightService.java`)

1. Người dùng mở trang Insight → `GET /insights`.
2. Người dùng bấm làm mới → `POST /insights:refresh`: backend tính lại các mẫu hình trong **30 ngày gần nhất**, so với 30 ngày trước đó, giữ tối đa **5** phát hiện mạnh nhất, cần ít nhất `mylog.insights.minimum-samples` (mặc định 7) mẫu.
3. Mỗi insight có câu mô tả, hướng (tăng/giảm/ổn định), độ mạnh, cỡ mẫu và danh sách bằng chứng.
4. Người dùng có thể ẩn insight không hữu ích (`PATCH /insights/{id} {status: HIDDEN}`); insight đã ẩn không hiện lại.

**Các loại insight**

| Loại | Ý nghĩa |
|---|---|
| `WEEKDAY_PATTERN` | Một ngày trong tuần có chỉ số khác hẳn (ví dụ căng thẳng hơn vào thứ Hai) |
| `MOOD_TREND_SHIFT` | Xu hướng tâm trạng nửa sau khác nửa đầu khoảng thời gian |
| `EMOTION_TOPIC_COOCCURRENCE` | Một cảm xúc hay đi cùng một chủ đề (cần bài đã phân tích) |
| `TAG_MOOD_ASSOCIATION` | Tâm trạng khác nhau giữa bài có và không có một nhãn |
| `ACTIVITY_MOOD_ASSOCIATION` | Tâm trạng khác nhau theo hoạt động trong check-in |
| `SLEEP_MOOD_ASSOCIATION` | Liên hệ giữa giấc ngủ và tâm trạng |
| `EXCEPTION_DAYS` | Điểm chung của những ngày nặng nề mà vẫn ổn |
| `CONDITION_SHIFT`, `TOPIC_SHIFT`, `RECOVERY_SHIFT` | Thay đổi so với 30 ngày trước: điều kiện, tỉ lệ chủ đề, số ngày để tâm trạng hồi lại |

Các liên hệ mà ai cũng đoán được bị bỏ qua (`ExpectedPatterns`), chỉ giữ chiều bất thường.

**Linh vật:** `GET /insights/mascot` trả tâm trạng được chọn nhiều nhất trong **7 ngày** để linh vật trang Insight thể hiện. Người dùng chọn hoặc tắt linh vật trong hồ sơ (`mascot`).

**Báo cáo** (`ReportService.java`)

1. Người dùng mở trang báo cáo → `GET /reports`.
2. Người dùng yêu cầu báo cáo tuần hoặc tháng → `POST /reports {type}`. Backend tạo báo cáo cho **tuần (thứ Hai–Chủ nhật) hoặc tháng gần nhất đã kết thúc** theo múi giờ hồ sơ; nếu kỳ đó đã có báo cáo thì trả báo cáo cũ.
3. Worker tính snapshot: số ngày có dữ liệu, số bài, trung bình các chỉ số, chuỗi ngày viết, so sánh với kỳ trước, cảm xúc/chủ đề hàng đầu, ngày tốt nhất và ngày thấp nhất (cần ít nhất 2 ngày có điểm khác nhau).
4. Người dùng mở báo cáo; tiêu đề các bài liên quan được giải mã lúc này.
5. Tự động: mỗi giờ, scheduler xếp báo cáo tuần vào thứ Hai và báo cáo tháng vào ngày 1 (theo giờ địa phương) cho mọi người dùng.

**Làm mới insight theo lịch** (`InsightRefreshScheduler`, bật bằng `mylog.insights.refresh.enabled`): mỗi 15 phút kiểm tra người dùng đang ở giờ địa phương `local-hour` (mặc định 3 giờ sáng), làm mới **một lần mỗi ngày** và chỉ khi nhật ký/check-in đã thay đổi; mặc định không gọi AI (`ai-narrative: false`).

## 4. Luồng phụ và ngoại lệ

- `range` khác `7d`/`30d`/`90d`: `400`.
- Dữ liệu ít hơn ngưỡng mẫu: không tạo insight; giao diện hiện trạng thái trống thân thiện.
- Kỳ báo cáo không có dữ liệu: báo cáo vẫn được tạo với câu chữ tự nhiên ("chưa đủ dữ liệu").
- **Tạo lại báo cáo:** mỗi người dùng được tạo lại **1 lần mỗi tuần** (thứ Hai–Chủ nhật theo giờ địa phương); lần thứ hai trả `409` "Bạn đã tạo lại bản nhìn lại trong tuần này." Phiên bản cũ giữ nguyên. Local có thể tắt giới hạn (`mylog.reports.weekly-regenerate-limit=false`).
- Vượt quota làm mới insight (20/giờ) hoặc tạo báo cáo (10/giờ): `429`.

## 5. Business rule

- Chuỗi ngày viết chỉ tính ngày có nhật ký (ngày chỉ có check-in không tính) và có thể kết thúc hôm qua.
- Liên hệ cần cặp mẫu đủ và có biến thiên.
- Số thập phân trong câu chữ dùng dấu phẩy kiểu Việt Nam.
- Báo cáo bất biến: tạo lại sinh `version` mới, không sửa bản cũ.

## 6. API liên quan

Xem bảng API ở [README](README.md#4-api-contract).

## 7. Giao diện web và mobile

- **Web:**
  - `app/(app)/dashboard`: dải phổ tâm trạng, thẻ check-in.
  - `app/(app)/insight`: danh sách insight, linh vật, panel kiểu suy nghĩ.
  - `app/(app)/reports`: danh sách và chi tiết báo cáo.
- **Mobile:** `(app)/dashboard.tsx`, `(app)/insights.tsx`, `(app)/report/[id].tsx`; chỉ hiển thị dữ liệu backend đã có.

## 8. Tiêu chí chấp nhận

- [x] Dashboard, insight và báo cáo tuần có phiên bản chỉ trong phạm vi chủ sở hữu; dashboard 30 ngày chịu được 10.000 bài synthetic (`M4IntegrationTest`).
- [x] Làm mới chỉ xét 30 ngày gần nhất; AI chỉ diễn đạt lại khi có consent và qua kiểm tra; insight đã ẩn không hiện lại; linh vật theo 7 ngày (`InsightPatternIntegrationTest`).
- [x] Các bộ phát hiện mẫu hình (`PatternDetectorsTest`), chuỗi ngày và liên hệ (`InsightCalculationsTest`).
- [x] Template tiếng Việt, không có từ nguyên nhân/chẩn đoán; validator chặn số bịa, nguyên nhân, liên hệ (`InsightNarrativeTest`).
- [x] Báo cáo theo yêu cầu có so sánh, điểm nổi bật, tiêu đề chỉ cho chủ sở hữu; làm mới insight theo lịch chạy một lần mỗi ngày địa phương và chỉ khi có dữ liệu mới (`ReportDetailIntegrationTest`); kỳ báo cáo đúng khi đổi giờ mùa hè, năm nhuận (`ReportPeriodTest`); giới hạn tạo lại (`ReportRegenerationLimitTest`); câu chữ báo cáo (`ReportNarrativesTest`).
- [x] Web: `dashboard-spectrum.spec.ts`, `insight-mascot.spec.ts`, `pattern-insights.spec.ts`, `report-detail.spec.ts`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | Các test ở mục 8 |
| Web | đã kiểm chứng | Playwright ở mục 8 |
| Mobile | xong | Kiểm chứng bằng smoke API và Playwright trên Expo web |

## 10. Câu hỏi mở

- Xem câu hỏi 1 ở [README](README.md#8-câu-hỏi-mở) về báo cáo khi worker tắt.
