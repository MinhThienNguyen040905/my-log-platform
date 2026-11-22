# UC-17 — Xem nội dung và nguồn hỗ trợ đã duyệt

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Safety](README.md)
- **Catalog:** UC-17 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

UC này gồm hai loại nội dung: **nguồn hỗ trợ an toàn** (thuộc safety) và **nội dung kiến thức đã duyệt** (thuộc [11-knowledge](../11-knowledge.md)).

## 1. Tác nhân

Người dùng (nguồn hỗ trợ: cả khách chưa đăng nhập; nội dung kiến thức: người dùng đã đăng nhập).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** nội dung đã qua quy trình duyệt và còn hiệu lực.
- **Hậu điều kiện:** không thay đổi dữ liệu.

## 3. Luồng chính

**Nguồn hỗ trợ an toàn**

1. Client gọi `GET /safety/resources?locale=&country=` (không cần đăng nhập).
2. Backend trả các nguồn có `status = APPROVED`, đã có `verified_at`, `source_url` bắt đầu bằng `https://`, đúng `locale` và `country_code`, sắp theo tên (`JpaSafetyResourceCatalog.java:22-24`).

**Nội dung kiến thức đã duyệt**

1. Client gọi `GET /recommendations?topicCode=...` (cần đăng nhập).
2. Backend lấy tối đa 3 đoạn trích từ phiên bản kiến thức `APPROVED`, còn hiệu lực, đúng chủ đề và ngôn ngữ của hồ sơ; mỗi đoạn có trích nguồn (item, phiên bản, chunk, tên nguồn, URL).
3. Tùy chọn: `POST /recommendations/questions {topicCode}` sinh tối đa 3 câu hỏi gợi mở từ các đoạn đó. Chi tiết ở [11-knowledge](../11-knowledge.md).

## 4. Luồng phụ và ngoại lệ

- `locale` hoặc `country` sai định dạng: `400`.
- Không có nguồn/đoạn trích phù hợp: danh sách rỗng; câu hỏi gợi mở trả `message` "Chưa đủ thông tin để tạo câu hỏi phù hợp."
- Bộ sinh câu hỏi lỗi: `503`.

## 5. Business rule

- Chỉ nội dung do con người duyệt mới được hiển thị; AI không tạo nguồn hỗ trợ.
- Nguồn hỗ trợ được quản lý ở UC-28 ([14-admin](../14-admin/README.md)).

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/safety/resources` | `locale`, `country` | `200 [Resource]` | 400 |
| GET | `/api/v1/recommendations` | `topicCode` | `200 {topicCode, excerpts: [{text, citation}]}` | 400, 401 |
| POST | `/api/v1/recommendations/questions` | `{topicCode}` | `200 {topicCode, questions[], message}` | 400, 401, 503 |

## 7. Giao diện web và mobile

- **Web:** nguồn hỗ trợ trong `SafetyModal`. Client `features/knowledge/api/recommendations.ts` có sẵn nhưng **chưa có màn hình nào dùng**.
- **Mobile:** nguồn hỗ trợ trong `safety-support-modal.tsx`; chưa có màn hình kiến thức.

## 8. Tiêu chí chấp nhận

- [x] Chỉ nguồn đã xác minh và policy đang được duyệt mới dùng được (`JournalCheckinIntegrationTest.onlyVerifiedResourcesAndActiveApprovedPoliciesAreUsable`).
- [x] Quy trình duyệt kiến thức, truy xuất và trích nguồn (`M6IntegrationTest`).
- [ ] Có ít nhất một bộ nguồn hỗ trợ thật đã xác minh cho `vi-VN`/`VN`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | |
| Web | đang làm | Có nguồn hỗ trợ; chưa có giao diện xem nội dung kiến thức |
| Mobile | đang làm | Có nguồn hỗ trợ; chưa có nội dung kiến thức |
| Nội dung | chưa có | Chưa có nguồn hỗ trợ hay bài kiến thức nào được duyệt |

## 10. Câu hỏi mở

- Người dùng sẽ duyệt nội dung kiến thức ở đâu trong app (thư viện bài viết, gợi ý trong editor, sau phản chiếu)?
