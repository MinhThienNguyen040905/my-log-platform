# UC-28 — Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** **đề xuất**, chưa có API hay giao diện. Phần "Đã có" lấy từ code ngày 2026-11-25; phần còn lại lấy từ catalog, **chờ xác nhận** và cần thiết kế trước khi triển khai.

- **Feature:** [Quản trị](README.md)
- **Catalog:** UC-28 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)
- **Liên quan:** [UC-14](../08-safety/UC-14-risk-guidance.md), [UC-17](../08-safety/UC-17-approved-resources.md) dùng nguồn hỗ trợ và policy được duyệt ở đây.

## 1. Tác nhân

Người chịu trách nhiệm safety, vai trò `SAFETY_ADMIN` (quyền `safety:manage`).

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đăng nhập với quyền `safety:manage`.
- **Hậu điều kiện (dự kiến):** nguồn hỗ trợ hoặc phiên bản policy safety chuyển sang `APPROVED` (hoặc `RETIRED`), có người duyệt, thời điểm duyệt, bằng chứng kiểm chứng và audit; người dùng chỉ thấy bản `APPROVED`.

## 3. Luồng chính

### Đã có (không qua giao diện quản trị)

- Bảng `safety_resources` (V7): `locale`, `country_code`, `resource_type`, `name`, `contact_value`, `description`, `source_url`, `verified_at`, `status` (`DRAFT`/`APPROVED`/`RETIRED`), `version`.
- Bảng `safety_policy_versions` (V7): `version`, `status`, `config` (JSONB), `approved_by`, `approved_at`, `effective_from`, `effective_to`.
- API công khai `GET /api/v1/safety/resources?locale=&country=` chỉ trả nguồn `APPROVED`, đã xác minh và có `source_url` HTTPS (xem [UC-17](../08-safety/UC-17-approved-resources.md)).
- `SafetyPolicyGate` chỉ mở phân tích thường khi có policy `APPROVED` còn hiệu lực (xem [08-safety](../08-safety/README.md)).
- Vai trò `SAFETY_ADMIN` và quyền `safety:manage` đã có (V12) nhưng **chưa endpoint nào dùng**.
- **Hiện chưa có nguồn hỗ trợ thật nào được duyệt.** Muốn thêm hoặc duyệt phải sửa DB trực tiếp hoặc viết migration.

### Dự kiến (theo catalog, chưa làm)

1. Người phụ trách safety xem danh sách nguồn hỗ trợ và phiên bản policy, kèm nội dung, phiên bản và bằng chứng kiểm chứng.
2. Tạo hoặc sửa bản nháp (`DRAFT`).
3. Phê duyệt (`APPROVED`) hoặc từ chối.
4. Theo dõi hạn kiểm chứng lại; rút phê duyệt (`RETIRED`) khi nguồn không còn đúng.

## 4. Luồng phụ và ngoại lệ

Chưa thiết kế. Cần ít nhất: thiếu quyền → `403`; duyệt nguồn không có `source_url` HTTPS hoặc chưa xác minh → từ chối; sửa đồng thời → `409`.

## 5. Business rule

- Người dùng chỉ thấy nguồn `APPROVED`; LLM không được tự tạo hotline hay nguồn hỗ trợ khẩn cấp (`backend/AGENTS.md` mục 6).
- (Dự kiến) Mỗi lần duyệt, sửa, rút phải có audit với người thực hiện.
- (Dự kiến) Thay đổi policy ảnh hưởng ngay luồng [UC-14](../08-safety/UC-14-risk-guidance.md).

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| GET | `/api/v1/safety/resources?locale=&country=` | — | `200 [SafetyResource]` (công khai, đã có) | 400 |

API quản trị: chưa có, cần thiết kế.

## 7. Giao diện web và mobile

- **Web:** chưa có. Trang admin không có phần safety.
- **Mobile:** không áp dụng.

## 8. Tiêu chí chấp nhận

- [x] API công khai chỉ trả nguồn đã duyệt (xem test ở [08-safety](../08-safety/README.md)).
- [ ] API quản trị xem, tạo, duyệt, rút nguồn hỗ trợ, có kiểm tra `safety:manage` và audit.
- [ ] API quản trị cho phiên bản policy safety.
- [ ] Giao diện web cho người phụ trách safety.
- [ ] Ít nhất một bộ nguồn hỗ trợ thật cho `vi`/`VN` được duyệt.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | chưa làm | Có bảng, quyền và API đọc công khai; chưa có API quản trị |
| Web | chưa làm | |
| Mobile | — | Không áp dụng |

## 10. Câu hỏi mở

1. Roadmap xếp "quản trị nguồn hỗ trợ" vào P0. Có làm UC-28 trước khi ra mắt, hay ra mắt với nguồn hỗ trợ được duyệt ngoài hệ thống rồi nạp bằng migration?
2. Ai trong nhóm đóng vai `SAFETY_ADMIN` và duyệt nội dung khủng hoảng?
3. Một nguồn hỗ trợ cần kiểm chứng lại sau bao lâu?
