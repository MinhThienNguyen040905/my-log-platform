# Tiến độ mylog — 2026-11-15

Ảnh chụp từ đợt rà soát tài liệu, code, migration và kiểm tra local ngày 2026-11-15; bổ sung kiểm tra database theo yêu cầu người dùng cùng ngày. Có implementation hoặc test trong repository không đồng nghĩa đã triển khai toàn bộ ứng dụng, được duyệt xử lý dữ liệu thật hay đạt điều kiện phát hành. Kiểm tra DB dùng cấu hình kết nối backend, chỉ đọc metadata/lịch sử Flyway, không đọc nội dung người dùng hoặc công bố thông tin xác thực.

## Tiến độ theo phần

| Phần | Implementation hiện có | Còn mở |
| --- | --- | --- |
| Backend M0–M2 | Foundation, auth/session, đăng ký tạm/xác minh email, reset/đổi mật khẩu, Google auth, profile/consent, journal/check-in, nhãn, ảnh và safety ingress | Kiểm chứng staging, đánh giá classifier/policy và nguồn hỗ trợ được duyệt |
| Backend M3–M4 | Outbox/AI worker, version/consent/safety gates, adapter analyzer, dashboard/insight/report | Provider approval, output safety evaluation, hiệu năng và E2E với môi trường được duyệt |
| Backend M5–M6 | Goal/habit/completion, API sửa/xóa habit và goal, knowledge review/chunk/embedding/retrieval/citation, admin | Wellness policy/content review, retrieval và generation evaluation |
| Backend M7 | Export CSV/PDF mã hóa, deletion job, feedback, reset dữ liệu | Kiểm chứng cleanup dịch vụ ngoài và vòng đời vận hành; xem checklist M7 |
| Web | BFF và các luồng auth/profile/journal, check-in, dashboard/insight/report, self-care CRUD, export/reset dữ liệu, feedback/admin, Companion và công cụ dịu | E2E toàn bộ với backend, staging; web không có nút yêu cầu xóa tài khoản, hướng dẫn liên hệ admin khi có cấu hình |
| Mobile | Auth/onboarding/profile/session, nhật ký văn bản và nhãn/yêu thích, dashboard/insight/report, self-care cơ bản, export; reset mật khẩu bằng mã, analysis/retry/polling và màn safety | Xem [trạng thái mobile](front-end/my-app-mobile/PROJECT_STATUS.md); còn thiếu các slice và kiểm chứng Android/iOS |
| Companion | Backend/frontend giai đoạn 1–5 đã có code; giai đoạn 4 mới có bước 4a: adapter LLM, validator, screening đầu ra, fallback và synthetic evaluation | Bước 4b–4e, dữ liệu/model tùy biến và phê duyệt provider; mặc định generation tắt |
| Safety model | Pipeline train và inference có cổng manifest approval/checksum | Chưa có bằng chứng artifact/model được duyệt hoặc đạt tiêu chí release |
| Recommend model | 42 card duy nhất: 7 `approved`, 35 `pending`; công cụ curation, demo và importer corpus riêng | 3 test không khớp trạng thái dữ liệu; approval metadata không tự chứng minh quyền dùng production |

Flyway hiện có **21 migration V1–V21**. Kiểm tra trực tiếp database đang cấu hình trong `backend/.env` ngày 2026-11-15 xác nhận cả 21 migration đã áp dụng thành công, Flyway validation khớp repository và Hibernate validation đạt. Kết quả chỉ áp dụng cho database đã kiểm tra, không suy rộng sang môi trường khác. Migration đã áp dụng phải giữ bất biến.

## Đối chiếu database ngày 2026-11-15

| Kiểm tra | Kết quả |
| --- | --- |
| Kết nối JDBC bằng cấu hình backend | Thành công; PostgreSQL 17.11, schema `public` |
| Extensions | `pgcrypto` 1.3 và `vector` 0.8.2 |
| Lịch sử Flyway | V1–V21 đều `SUCCESS`, không có bản ghi thất bại hoặc migration pending |
| `Flyway.validateWithResult()` | Đạt, không có migration không hợp lệ hoặc checksum mismatch |
| Hibernate schema validation | Đạt với 46 entity; Maven compile xác nhận compiled classes hiện hành |
| Bảng ứng dụng | Đủ 46 bảng khai báo bởi migration; 47 bảng nếu tính `flyway_schema_history` |
| Index/trigger có tên trong migration | Có đủ 63 index khai báo rõ và 3 trigger; kiểm tra sự tồn tại, không đo query plan/hiệu năng |
| V16–V18 | Có cột mã xác minh, `pending_registrations` và các index embedding/HNSW 768/1536 |
| V19 — Google auth | Có `user_oauth_identities`, `password_hash` nullable và constraint bắt buộc mật khẩu với tài khoản LOCAL |
| V20 — ảnh bản nháp | Có `position`, `is_cover`, FK entry nullable, trạng thái `PENDING_ATTACH` và index liên quan |
| V21 — Companion/habit | Có `habits.tool_code`, check constraint danh mục công cụ và `companion_habit_preferences` |
| Quyền truy cập trực tiếp | 46/46 bảng ứng dụng bật RLS; metadata không có table grant cho `anon`, `authenticated` hoặc `PUBLIC` |

Không chạy `migrate`, `repair`, DDL, seed hay job ứng dụng. Truy vấn metadata chạy trong transaction read-only rồi rollback; Flyway chỉ dùng `info`/`validate`, Hibernate chỉ bootstrap validation, không khởi động Spring hoặc worker. Schema hiện khớp các kiểm tra trên nên **không cần nâng version DB cho code V21**. Chưa kiểm chứng tính mới/đúng của dữ liệu nghiệp vụ, khả năng giải mã với key hiện tại hoặc E2E đăng nhập/ghi nhật ký; kiểm tra schema không thay thế các bước này.

## Kiểm tra thực tế ngày 2026-11-15

| Kiểm tra | Kết quả và phạm vi |
| --- | --- |
| Backend Maven: 17 lớp test được chọn | **68 test đạt**, không fail/error/skip; chạy trên Java 25 local, chưa thay thế full `verify` trên Java 21/CI |
| `backend/scripts/check-migrations.ps1` | Đạt, kiểm tra tên/phiên bản của 21 file; không chạy Flyway lên database |
| Database cấu hình backend | Kết nối và schema validation đạt; xem đối chiếu trực tiếp ở trên. Không thay đổi schema hoặc dữ liệu |
| Web `npm run build` | Đạt production build và bước TypeScript của Next.js |
| Web `tsc --noEmit --incremental false` | Đạt sau khi sinh lại route types/build; lần đầu gặp type sinh cũ tham chiếu route đã bỏ |
| Web `npm run lint` | Đạt với 0 lỗi, 4 cảnh báo về font và thẻ `img` |
| Recommend `python -X utf8 -m unittest discover -s tests -v` | **12 đạt, 3 thất bại**: `test_sources_are_unique_and_pending_export_is_empty`, `test_pending_cards_are_not_live_by_default`, `test_pending_cards_cannot_be_sent_to_provider` vẫn giả định toàn bộ card chưa duyệt |
| PostgreSQL/Testcontainers integration | Chưa chạy lại: Docker daemon không hoạt động |
| Web Playwright `test smoke.spec.ts --workers=2` | 6 ca dừng ở bước launch vì thiếu Chromium headless shell đúng phiên bản; chưa kiểm chứng UI, không kết luận 6 lỗi sản phẩm |
| Mobile typecheck/lint/export/native | Chưa kiểm chứng lại; lần gọi `tsc` không chạy được vì thiếu executable trong dependencies local |

Backend test selection để tái lập, chạy trong `backend/`:

```powershell
./mvnw.cmd -B -ntp "-Dtest=ModuleArchitectureTest,SensitiveDataCipherTest,IdentityJwtTest,SafetyScreeningCorpusTest,AnalysisOutputValidatorTest,AnalysisJobHandlerTest,WritingSuggestionServiceTest,CompanionStage4aEvaluationTest,CompanionHabitSuggestionServiceTest,TipTapContentTest,HabitScheduleTest,HabitMoodAssociationTest,PatternDetectorsTest,InsightCalculationsTest,ReportNarrativesTest,GoogleAuthFlowTest,PasswordResetUnitTest" test
```

Kết quả smoke/bundle cũ trong tài liệu thành phần là bằng chứng lịch sử theo ngày ghi nhận, không được tính như đã chạy lại trong đợt này.

## Công việc tiếp theo

1. Đối chiếu hồ sơ duyệt 7 card của recommend-model, cập nhật test cho cả card đủ/chưa đủ điều kiện và giữ cổng approval. Chưa sửa test hay dữ liệu trong đợt cập nhật tài liệu này.
2. Chuẩn bị Docker/dependencies/browser, chạy full backend `verify`, E2E với tài khoản synthetic và kiểm chứng đồng bộ web/mobile, lỗi mạng, xung đột phiên bản.
3. Hoàn thiện các slice mobile còn thiếu và kiểm tra native, accessibility, SecureStore và tải/chia sẻ export.
4. Đóng các gate [M8](backend/docs/BACKEND_DEVELOPMENT_PLAN.md): safety/provider/resource approval, scan, staging API/worker, backup/restore, load/resilience, log/privacy và vận hành. **M8 chưa đạt exit criteria production.**

## Tài liệu chi tiết

- [Kế hoạch backend](backend/docs/BACKEND_DEVELOPMENT_PLAN.md), [release runbook](backend/docs/M8_RELEASE_RUNBOOK.md).
- [AI analysis guide](backend/docs/AI_ANALYSIS_GUIDE.md), [Companion](backend/docs/COMPANION_CHATBOT_PLAN.md).
- [Khoảng thiếu web/mobile](backend/docs/WEB_MOBILE_GAP_IMPLEMENTATION_PLAN.md), [mobile status](front-end/my-app-mobile/PROJECT_STATUS.md).
- [Recommend model](recommend-model/README.md), [safety model](safety-model/README.md).
