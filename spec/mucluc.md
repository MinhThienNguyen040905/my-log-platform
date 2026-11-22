# Mục lục spec

Đọc nhanh: tìm feature hoặc UC cần làm, mở file tương ứng. Quy ước xem [README.md](README.md).

## Khách và tài khoản

| # | Feature | UC | File | Mô tả |
|---|---|---|---|---|
| 01 | **Định danh và xác thực** | — | [`01-identity-auth/`](01-identity-auth/README.md) | Đăng ký, xác minh email, đăng nhập, quên mật khẩu, quản lý phiên. |
| | | UC-01 | [`UC-01-register-verify-email.md`](01-identity-auth/UC-01-register-verify-email.md) | Đăng ký và xác minh email |
| | | UC-02 | [`UC-02-login-logout.md`](01-identity-auth/UC-02-login-logout.md) | Đăng nhập và đăng xuất |
| | | UC-03 | [`UC-03-reset-password.md`](01-identity-auth/UC-03-reset-password.md) | Lấy lại mật khẩu |
| | | UC-06 | [`UC-06-sessions.md`](01-identity-auth/UC-06-sessions.md) | Xem và thu hồi phiên đăng nhập |
| 02 | **Hồ sơ và đồng ý xử lý dữ liệu** | — | [`02-profile-consent/`](02-profile-consent/README.md) | Onboarding, hồ sơ cá nhân, consent (ví dụ AI_PROCESSING). |
| | | UC-04 | [`UC-04-onboarding-profile.md`](02-profile-consent/UC-04-onboarding-profile.md) | Hoàn thành thiết lập ban đầu và sửa hồ sơ |
| | | UC-05 | [`UC-05-data-consent.md`](02-profile-consent/UC-05-data-consent.md) | Chọn hoặc rút lại sự đồng ý xử lý dữ liệu |

## Người dùng

| # | Feature | UC | File | Mô tả |
|---|---|---|---|---|
| 03 | **Nhật ký** | — | [`03-journal/`](03-journal/README.md) | Viết, lưu, tìm, sửa, xóa nhật ký; gợi ý viết; ảnh trong bài. |
| | | UC-07 | [`UC-07-writing-suggestions.md`](03-journal/UC-07-writing-suggestions.md) | Nhận gợi ý để viết tiếp |
| | | UC-08 | [`UC-08-write-save-entry.md`](03-journal/UC-08-write-save-entry.md) | Viết và lưu nhật ký |
| | | UC-09 | [`UC-09-read-search-entries.md`](03-journal/UC-09-read-search-entries.md) | Đọc, tìm và xem lại nhật ký |
| | | UC-10 | [`UC-10-edit-favorite-delete.md`](03-journal/UC-10-edit-favorite-delete.md) | Sửa, đánh dấu và xóa nhật ký |
| | | UC-11 | [`UC-11-entry-images.md`](03-journal/UC-11-entry-images.md) | Thêm ảnh vào nhật ký |
| 04 | **Check-in cảm xúc** | UC-12 | [`04-checkin.md`](04-checkin.md) | Ghi nhận cảm xúc hằng ngày. |
| 05 | **Phân tích AI và phản chiếu** | UC-13 | [`05-ai-analysis.md`](05-ai-analysis.md) | Phân tích cảm xúc/chủ đề sau khi lưu bài và hiển thị phản chiếu. |
| 06 | **Nhận diện kiểu suy nghĩ** | — | [`06-thinking-patterns.md`](06-thinking-patterns.md) | Nhận diện kiểu suy nghĩ lặp lại trong nhật ký (prototype). Chưa có UC trong catalog. |
| 07 | **Insight và báo cáo** | — | [`07-insights-reports/`](07-insights-reports/README.md) | Dashboard, xu hướng cảm xúc, báo cáo, tiến bộ nhỏ, tóm tắt cho buổi tham vấn. |
| | | UC-15 | [`UC-15-mood-trends-reports.md`](07-insights-reports/UC-15-mood-trends-reports.md) | Xem xu hướng cảm xúc và báo cáo |
| | | UC-19 | [`UC-19-small-progress.md`](07-insights-reports/UC-19-small-progress.md) | Xem lại những tiến bộ nhỏ |
| | | UC-20 | [`UC-20-counseling-summary.md`](07-insights-reports/UC-20-counseling-summary.md) | Tạo bản tóm tắt mang đến buổi tham vấn |
| 08 | **Safety** | — | [`08-safety/`](08-safety/README.md) | Sàng lọc rủi ro, hướng dẫn an toàn và nguồn hỗ trợ đã duyệt. |
| | | UC-14 | [`UC-14-risk-guidance.md`](08-safety/UC-14-risk-guidance.md) | Nhận hướng dẫn an toàn khi có dấu hiệu nguy cơ |
| | | UC-17 | [`UC-17-approved-resources.md`](08-safety/UC-17-approved-resources.md) | Xem nội dung và nguồn hỗ trợ đã duyệt |
| 09 | **Tự chăm sóc** | — | [`09-selfcare/`](09-selfcare/README.md) | Mục tiêu, thói quen, công cụ giúp bình tĩnh, lời nhắn cho bản thân tương lai. |
| | | UC-16 | [`UC-16-goals-habits.md`](09-selfcare/UC-16-goals-habits.md) | Đặt mục tiêu và theo dõi thói quen tự chăm sóc |
| | | UC-18 | [`UC-18-future-self-letter.md`](09-selfcare/UC-18-future-self-letter.md) | Viết lời nhắn cho chính mình trong tương lai |
| 10 | **Companion** | — | [`10-companion.md`](10-companion.md) | Trợ lý đồng hành: gợi ý tiêu đề, gợi ý công cụ và thói quen. Chưa có UC trong catalog. |
| 12 | **Xuất dữ liệu và xóa tài khoản** | — | [`12-export-deletion/`](12-export-deletion/README.md) | Quyền của người dùng đối với dữ liệu: xuất và xóa. |
| | | UC-22 | [`UC-22-export-data.md`](12-export-deletion/UC-22-export-data.md) | Xuất dữ liệu cá nhân |
| | | UC-23 | [`UC-23-delete-account.md`](12-export-deletion/UC-23-delete-account.md) | Xóa tài khoản |
| 13 | **Phản hồi** | — | [`13-feedback/`](13-feedback/README.md) | Người dùng gửi phản hồi; quản trị xem và xử lý. |
| | | UC-21 | [`UC-21-send-feedback.md`](13-feedback/UC-21-send-feedback.md) | Gửi phản hồi về nội dung hoặc gợi ý |
| | | UC-26 | [`UC-26-review-feedback.md`](13-feedback/UC-26-review-feedback.md) | Xem và xử lý phản hồi người dùng |

## Quản trị và nội dung

| # | Feature | UC | File | Mô tả |
|---|---|---|---|---|
| 11 | **Kho kiến thức** | UC-27 | [`11-knowledge.md`](11-knowledge.md) | Biên tập, duyệt nội dung hỗ trợ; retrieval cho RAG. |
| 14 | **Quản trị** | — | [`14-admin/`](14-admin/README.md) | Quản lý tài khoản, vận hành, tác vụ lỗi, nguồn hỗ trợ và quy tắc safety. |
| | | UC-24 | [`UC-24-user-account-status.md`](14-admin/UC-24-user-account-status.md) | Quản lý trạng thái tài khoản người dùng |
| | | UC-25 | [`UC-25-operations-failed-jobs.md`](14-admin/UC-25-operations-failed-jobs.md) | Theo dõi vận hành và xử lý tác vụ lỗi |
| | | UC-28 | [`UC-28-safety-resources-rules.md`](14-admin/UC-28-safety-resources-rules.md) | Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn |

## Tra theo mã UC

- [UC-10](03-journal/UC-10-edit-favorite-delete.md) — Sửa, đánh dấu và xóa nhật ký
- [UC-11](03-journal/UC-11-entry-images.md) — Thêm ảnh vào nhật ký
- [UC-12](04-checkin.md) — Check-in cảm xúc hằng ngày
- [UC-13](05-ai-analysis.md) — Xem phản chiếu sau khi lưu bài
- [UC-14](08-safety/UC-14-risk-guidance.md) — Nhận hướng dẫn an toàn khi có dấu hiệu nguy cơ
- [UC-15](07-insights-reports/UC-15-mood-trends-reports.md) — Xem xu hướng cảm xúc và báo cáo
- [UC-16](09-selfcare/UC-16-goals-habits.md) — Đặt mục tiêu và theo dõi thói quen tự chăm sóc
- [UC-17](08-safety/UC-17-approved-resources.md) — Xem nội dung và nguồn hỗ trợ đã duyệt
- [UC-18](09-selfcare/UC-18-future-self-letter.md) — Viết lời nhắn cho chính mình trong tương lai
- [UC-19](07-insights-reports/UC-19-small-progress.md) — Xem lại những tiến bộ nhỏ
- [UC-20](07-insights-reports/UC-20-counseling-summary.md) — Tạo bản tóm tắt mang đến buổi tham vấn
- [UC-21](13-feedback/UC-21-send-feedback.md) — Gửi phản hồi về nội dung hoặc gợi ý
- [UC-22](12-export-deletion/UC-22-export-data.md) — Xuất dữ liệu cá nhân
- [UC-23](12-export-deletion/UC-23-delete-account.md) — Xóa tài khoản
- [UC-24](14-admin/UC-24-user-account-status.md) — Quản lý trạng thái tài khoản người dùng
- [UC-25](14-admin/UC-25-operations-failed-jobs.md) — Theo dõi vận hành và xử lý tác vụ lỗi
- [UC-26](13-feedback/UC-26-review-feedback.md) — Xem và xử lý phản hồi người dùng
- [UC-27](11-knowledge.md) — Biên tập và duyệt nội dung hỗ trợ
- [UC-28](14-admin/UC-28-safety-resources-rules.md) — Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn
- [UC-01](01-identity-auth/UC-01-register-verify-email.md) — Đăng ký và xác minh email
- [UC-02](01-identity-auth/UC-02-login-logout.md) — Đăng nhập và đăng xuất
- [UC-03](01-identity-auth/UC-03-reset-password.md) — Lấy lại mật khẩu
- [UC-04](02-profile-consent/UC-04-onboarding-profile.md) — Hoàn thành thiết lập ban đầu và sửa hồ sơ
- [UC-05](02-profile-consent/UC-05-data-consent.md) — Chọn hoặc rút lại sự đồng ý xử lý dữ liệu
- [UC-06](01-identity-auth/UC-06-sessions.md) — Xem và thu hồi phiên đăng nhập
- [UC-07](03-journal/UC-07-writing-suggestions.md) — Nhận gợi ý để viết tiếp
- [UC-08](03-journal/UC-08-write-save-entry.md) — Viết và lưu nhật ký
- [UC-09](03-journal/UC-09-read-search-entries.md) — Đọc, tìm và xem lại nhật ký
